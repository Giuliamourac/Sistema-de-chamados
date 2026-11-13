package com.sabesp.chamados.service;

import com.sabesp.chamados.model.Chamado;
import com.sabesp.chamados.model.Movimentacao;
import com.sabesp.chamados.model.StatusChamado;
import com.sabesp.chamados.model.Usuario;
import com.sabesp.chamados.repository.ChamadoRepository;
import com.sabesp.chamados.repository.MovimentacaoRepository;
import com.sabesp.chamados.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Atendimento dos chamados pelo administrador: listagem geral, troca de status,
 * atualizações de andamento, devolutivas e linha do tempo.
 * As permissões são conferidas aqui também (além do AdminInterceptor), para que
 * nenhuma alteração administrativa aconteça com um usuário que não seja administrador.
 */
@Service
public class AtendimentoService {

    /** Perfis que podem gerenciar chamados. */
    public static final Set<String> PERFIS_ADMIN = Set.of("ADMIN");

    public static final int MAX_TEXTO = 4000;

    /** Erro de validação/regra com mensagem pronta para a tela. */
    public static class AtendimentoException extends RuntimeException {
        public AtendimentoException(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * Filtros do painel. Conjuntos vazios = sem filtro.
     * conteudo: "com" / "sem" (imagens); tempo: "ate30" / "30a90" / "mais90" (dias desde a abertura).
     */
    public record Filtro(Set<String> status, Set<String> conteudo, Set<String> tempo, String busca) {
    }

    private final ChamadoRepository chamadoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public AtendimentoService(ChamadoRepository chamadoRepository,
                              MovimentacaoRepository movimentacaoRepository,
                              UsuarioRepository usuarioRepository) {
        this.chamadoRepository = chamadoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ---------------------------------------------------------------
    // Permissão
    // ---------------------------------------------------------------

    /** Administrador ativo, conferido no banco (não confia só na sessão). */
    @Transactional(readOnly = true)
    public Optional<Usuario> administrador(Integer idUsuario) {
        if (idUsuario == null) {
            return Optional.empty();
        }
        return usuarioRepository.findById(idUsuario)
                .filter(Usuario::isAtivo)
                .filter(u -> u.getTipo() != null && PERFIS_ADMIN.contains(u.getTipo().toUpperCase(Locale.ROOT)));
    }

    private Usuario exigirAdministrador(Integer idUsuario) {
        return administrador(idUsuario)
                .orElseThrow(() -> new AtendimentoException("Apenas administradores podem alterar chamados."));
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Chamado> listarTodos() {
        return chamadoRepository.findAllDetalhadoByOrderByDataAberturaDesc();
    }

    /** Aplica os filtros do painel sobre a lista completa. */
    public List<Chamado> filtrar(List<Chamado> todos, Filtro filtro) {
        String busca = filtro.busca() == null ? "" : filtro.busca().trim().toLowerCase(Locale.ROOT);
        return todos.stream()
                .filter(c -> vazio(filtro.status()) || filtro.status().contains(c.getStatusEnum().name()))
                .filter(c -> vazio(filtro.conteudo()) || filtro.conteudo().contains(c.getAnexos().isEmpty() ? "sem" : "com"))
                .filter(c -> vazio(filtro.tempo()) || filtro.tempo().contains(faixaDeTempo(c)))
                .filter(c -> busca.isEmpty() || contem(c, busca))
                .toList();
    }

    /** Quantidade de chamados em cada status (para o resumo do painel). */
    public Map<StatusChamado, Long> contarPorStatus(Collection<Chamado> chamados) {
        Map<StatusChamado, Long> contagem = new EnumMap<>(StatusChamado.class);
        for (StatusChamado s : StatusChamado.values()) {
            contagem.put(s, 0L);
        }
        for (Chamado c : chamados) {
            contagem.merge(c.getStatusEnum(), 1L, Long::sum);
        }
        return contagem;
    }

    @Transactional(readOnly = true)
    public Optional<Chamado> buscarParaAdmin(String protocolo) {
        return chamadoRepository.findDetalhadoByProtocolo(protocolo);
    }

    @Transactional(readOnly = true)
    public List<Movimentacao> linhaDoTempo(Chamado chamado) {
        return movimentacaoRepository.findByChamadoIdChamadoOrderByDataMovimentacaoAscIdMovimentacaoAsc(chamado.getIdChamado());
    }

    /** Devolutiva mais recente enviada ao cliente, se houver. */
    @Transactional(readOnly = true)
    public Optional<Movimentacao> ultimaDevolutiva(Chamado chamado) {
        return movimentacaoRepository
                .findByChamadoIdChamadoAndTipoOrderByDataMovimentacaoDescIdMovimentacaoDesc(chamado.getIdChamado(), Movimentacao.DEVOLUTIVA)
                .stream()
                .findFirst();
    }

    // ---------------------------------------------------------------
    // Ações do administrador (cada uma gera um registro novo na linha do tempo)
    // ---------------------------------------------------------------

    @Transactional
    public void alterarStatus(String protocolo, String codigoStatus, String comentario, Integer idAdmin) {
        Usuario admin = exigirAdministrador(idAdmin);
        Chamado chamado = carregar(protocolo);
        StatusChamado novo = StatusChamado.de(codigoStatus)
                .filter(StatusChamado.OPCOES_ADMIN::contains)
                .orElseThrow(() -> new AtendimentoException("Selecione um status válido."));
        String texto = textoOpcional(comentario, "O comentário");

        trocarStatus(chamado, novo, texto, admin);
    }

    @Transactional
    public void registrarAtualizacao(String protocolo, String descricao, Integer idAdmin) {
        Usuario admin = exigirAdministrador(idAdmin);
        Chamado chamado = carregar(protocolo);
        String texto = textoObrigatorio(descricao, "Descreva a atualização do atendimento.", "A atualização");

        Movimentacao mov = new Movimentacao(chamado, admin, Movimentacao.ATUALIZACAO);
        mov.setDescricao(texto);
        movimentacaoRepository.save(mov);
    }

    @Transactional
    public void enviarDevolutiva(String protocolo, String problema, String solucao, String observacoes,
                                 boolean concluir, Integer idAdmin) {
        Usuario admin = exigirAdministrador(idAdmin);
        Chamado chamado = carregar(protocolo);
        String textoProblema = textoObrigatorio(problema, "Informe o problema identificado.", "O problema identificado");
        String textoSolucao = textoObrigatorio(solucao, "Informe a solução aplicada.", "A solução aplicada");
        String textoObs = textoOpcional(observacoes, "As observações");

        Movimentacao mov = new Movimentacao(chamado, admin, Movimentacao.DEVOLUTIVA);
        mov.setProblemaIdentificado(textoProblema);
        mov.setSolucaoAplicada(textoSolucao);
        mov.setObservacoes(textoObs);
        movimentacaoRepository.save(mov);

        // A coluna que já existia em Ocorrencia guarda a solução mais recente
        chamado.setSolucaoAplicada(textoSolucao);

        if (concluir && chamado.getStatusEnum() != StatusChamado.CONCLUIDO) {
            trocarStatus(chamado, StatusChamado.CONCLUIDO, null, admin);
        }
    }

    // ---------------------------------------------------------------

    private void trocarStatus(Chamado chamado, StatusChamado novo, String comentario, Usuario admin) {
        StatusChamado anterior = StatusChamado.de(chamado.getStatus()).orElse(null);
        if (anterior == novo) {
            throw new AtendimentoException("O chamado já está com o status \"" + novo.getRotulo() + "\".");
        }

        chamado.setStatus(novo.name());
        if (novo.isFinalizado()) {
            if (chamado.getDataConclusao() == null) {
                chamado.setDataConclusao(LocalDateTime.now());
            }
        } else {
            chamado.setDataConclusao(null); // reaberto
        }

        Movimentacao mov = new Movimentacao(chamado, admin, Movimentacao.STATUS);
        mov.setStatusAnterior(anterior == null ? null : anterior.name());
        mov.setStatusNovo(novo.name());
        mov.setDescricao(comentario);
        movimentacaoRepository.save(mov);
    }

    private Chamado carregar(String protocolo) {
        return chamadoRepository.findByProtocolo(protocolo)
                .orElseThrow(() -> new AtendimentoException("Chamado não encontrado."));
    }

    private String textoObrigatorio(String valor, String mensagemVazio, String nomeCampo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new AtendimentoException(mensagemVazio);
        }
        if (texto.length() > MAX_TEXTO) {
            throw new AtendimentoException(nomeCampo + " aceita no máximo " + MAX_TEXTO + " caracteres.");
        }
        return texto;
    }

    private String textoOpcional(String valor, String nomeCampo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.length() > MAX_TEXTO) {
            throw new AtendimentoException(nomeCampo + " aceita no máximo " + MAX_TEXTO + " caracteres.");
        }
        return texto.isEmpty() ? null : texto;
    }

    private String faixaDeTempo(Chamado c) {
        long dias = c.getDiasDesdeAbertura();
        if (dias < 30) return "ate30";
        if (dias <= 90) return "30a90";
        return "mais90";
    }

    private boolean contem(Chamado c, String busca) {
        return minusculo(c.getProtocolo()).contains(busca)
                || minusculo(c.getTipoOcorrencia()).contains(busca)
                || minusculo(c.getEnderecoCompleto()).contains(busca)
                || (c.getUsuario() != null && (minusculo(c.getUsuario().getNome()).contains(busca)
                || minusculo(c.getUsuario().getEmail()).contains(busca)));
    }

    private static String minusculo(String valor) {
        return valor == null ? "" : valor.toLowerCase(Locale.ROOT);
    }

    private static boolean vazio(Set<String> conjunto) {
        return conjunto == null || conjunto.isEmpty();
    }
}
