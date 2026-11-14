package com.sabesp.chamados.config;

import com.sabesp.chamados.model.*;
import com.sabesp.chamados.repository.ChamadoRepository;
import com.sabesp.chamados.repository.MovimentacaoRepository;
import com.sabesp.chamados.repository.TipoOcorrenciaRepository;
import com.sabesp.chamados.repository.UsuarioRepository;
import com.sabesp.chamados.security.SenhaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * Dados de exemplo SOMENTE para o perfil "dev" (banco H2 em memória).
 * No SQL Server os tipos de ocorrência vêm do script SQL.
 */
@Component
@Profile("dev")
public class DadosDeTesteDev implements CommandLineRunner {

    private final TipoOcorrenciaRepository tipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final SenhaService senhaService;

    public DadosDeTesteDev(TipoOcorrenciaRepository tipoRepository,
                           UsuarioRepository usuarioRepository,
                           ChamadoRepository chamadoRepository,
                           MovimentacaoRepository movimentacaoRepository,
                           SenhaService senhaService) {
        this.tipoRepository = tipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.chamadoRepository = chamadoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.senhaService = senhaService;
    }

    @Override
    public void run(String... args) {
        if (tipoRepository.count() == 0) {
            for (String nome : List.of("Vazamento de água na rua", "Bueiro com vazamento", "Falta de água",
                    "Esgoto a céu aberto", "Entupimento de rede", "Outros")) {
                tipoRepository.save(new TipoOcorrencia(nome));
            }
        }

        if (chamadoRepository.count() > 0) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNome("Maria de Teste");
        usuario.setEmail("maria.teste@exemplo.com");
        usuario.setTelefone("(13) 99999-9999");
        usuario.setSenha(senhaService.gerarHash("senha1234")); // login de teste (somente perfil dev)
        usuario = usuarioRepository.save(usuario);

        // Administrador de teste (somente perfil dev): acessa o painel /admin/chamados
        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin.teste@exemplo.com");
        admin.setTelefone("(13) 3333-3333");
        admin.setTipo("ADMIN");
        admin.setSenha(senhaService.gerarHash("admin1234"));
        admin = usuarioRepository.save(admin);

        TipoOcorrencia vazamento = tipoRepository.findByNomeIgnoreCaseAndAtivoTrue("Vazamento de água na rua").orElseThrow();
        TipoOcorrencia bueiro = tipoRepository.findByNomeIgnoreCaseAndAtivoTrue("Bueiro com vazamento").orElseThrow();

        String descricao = "Rua se encontra com um cano estourado do qual esta vazando água a mais de semanas "
                + "e prejudicando a circulação";

        criar(1, usuario, vazamento, "ABERTO", descricao, null, null);

        Chamado emAndamento = criar(2, usuario, bueiro, "EM_ANDAMENTO", descricao,
                LocalDateTime.now().minusDays(3), null);
        registrar(emAndamento, admin, "ABERTO", "EM_ANDAMENTO",
                "Solicitação aceita e em análise pela equipe técnica.", LocalDateTime.now().minusDays(2));
        Movimentacao visita = new Movimentacao(emAndamento, admin, Movimentacao.ATUALIZACAO);
        visita.setDescricao("Equipe fez visita técnica para confirmar as informações da solicitação.");
        visita.setDataMovimentacao(LocalDateTime.now().minusDays(1));
        movimentacaoRepository.save(visita);

        // Cancelado, aberto há 4 dias e 7 horas (como no Figma)
        LocalDateTime abertura = LocalDateTime.now().minusDays(4).minusHours(7);
        Chamado cancelado = criar(3, usuario, vazamento, "CANCELADO", descricao, abertura, LocalDateTime.now());
        registrar(cancelado, admin, "ABERTO", "CANCELADO",
                "Ocorrência duplicada: o mesmo vazamento já está sendo atendido em outro protocolo.",
                LocalDateTime.now());
    }

    private void registrar(Chamado chamado, Usuario admin, String de, String para, String texto,
                           LocalDateTime quando) {
        Movimentacao mov = new Movimentacao(chamado, admin, Movimentacao.STATUS);
        mov.setStatusAnterior(de);
        mov.setStatusNovo(para);
        mov.setDescricao(texto);
        mov.setDataMovimentacao(quando);
        movimentacaoRepository.save(mov);
    }

    private Chamado criar(int numero, Usuario usuario, TipoOcorrencia tipo, String status,
                          String descricao, LocalDateTime abertura, LocalDateTime conclusao) {
        Localizacao local = new Localizacao("11060-000", "Gonzaga", "Rua Afonso Pena, 584",
                "Próximo à praça");

        Chamado chamado = new Chamado();
        chamado.setProtocolo(String.format("CH-%d-%06d", Year.now().getValue(), numero));
        chamado.setUsuario(usuario);
        chamado.setTipo(tipo);
        chamado.setLocalizacao(local);
        chamado.setDescricao(descricao);
        chamado.setStatus(status);
        chamado.setDataAbertura(abertura != null ? abertura : LocalDateTime.now());
        chamado.setDataConclusao(conclusao);

        Movimentacao inicio = new Movimentacao(chamado, usuario, Movimentacao.ABERTURA);
        inicio.setStatusNovo("ABERTO");
        inicio.setDataMovimentacao(chamado.getDataAbertura());
        chamado.getMovimentacoes().add(inicio);
        return chamadoRepository.save(chamado);
    }
}