package com.sabesp.chamados.service;

import com.sabesp.chamados.dto.ChamadoForm;
import com.sabesp.chamados.model.Anexo;
import com.sabesp.chamados.model.Chamado;
import com.sabesp.chamados.model.Localizacao;
import com.sabesp.chamados.model.TipoOcorrencia;
import com.sabesp.chamados.model.Usuario;
import com.sabesp.chamados.repository.ChamadoRepository;
import com.sabesp.chamados.repository.TipoOcorrenciaRepository;
import com.sabesp.chamados.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChamadoService {

    private static final int MAX_TENTATIVAS = 5;
    private static final int MAX_FOTOS = 10;
    private static final long MAX_BYTES_FOTO = 10L * 1024 * 1024; // 10 MB
    private static final List<String> STATUS_EM_ANDAMENTO = List.of("ABERTO", "EM_ANDAMENTO");

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoOcorrenciaRepository tipoOcorrenciaRepository;
    private final Path pastaUploads;

    public ChamadoService(ChamadoRepository chamadoRepository,
                          UsuarioRepository usuarioRepository,
                          TipoOcorrenciaRepository tipoOcorrenciaRepository,
                          @Value("${app.upload-dir:uploads}") String pastaUploads) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoOcorrenciaRepository = tipoOcorrenciaRepository;
        this.pastaUploads = Paths.get(pastaUploads).toAbsolutePath().normalize();
    }

    // Nomes dos tipos ativos, na ordem do banco (usados no formulário)
    public List<String> listarTiposAtivos() {
        return tipoOcorrenciaRepository.findByAtivoTrueOrderByIdTipoOcorrenciaAsc()
                .stream()
                .map(TipoOcorrencia::getNome)
                .toList();
    }

    // Cria usuário (se o e-mail for novo), localização, chamado e anexos.
    // Se dois chamados gerarem o mesmo protocolo ao mesmo tempo, o banco recusa
    // (UNIQUE) e tentamos de novo com outro número.
    public Chamado abrirChamado(ChamadoForm form, List<MultipartFile> fotos) {
        TipoOcorrencia tipo = tipoOcorrenciaRepository
                .findByNomeIgnoreCaseAndAtivoTrue(form.getTipoOcorrencia().trim())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ocorrência inválido."));
        Usuario usuario = obterOuCriarUsuario(form);
        List<MultipartFile> fotosValidas = fotosEnviadas(fotos);

        Chamado salvo;
        for (int tentativa = 1; ; tentativa++) {
            Chamado chamado = montarChamado(form, tipo, usuario, fotosValidas);
            try {
                salvo = chamadoRepository.saveAndFlush(chamado);
                break;
            } catch (DataIntegrityViolationException e) {
                if (tentativa >= MAX_TENTATIVAS) {
                    throw e;
                }
            }
        }

        gravarArquivos(salvo, fotosValidas);
        return salvo;
    }

    // Retorna a mensagem de erro ou null se as fotos estiverem válidas
    public String validarFotos(List<MultipartFile> fotos) {
        List<MultipartFile> enviadas = fotosEnviadas(fotos);
        for (MultipartFile foto : enviadas) {
            if (foto.getSize() > MAX_BYTES_FOTO) {
                return "Cada foto deve ter no máximo 10 MB.";
            }
            if (extensao(foto) == null) {
                return "As fotos devem ser PNG ou JPG.";
            }
        }
        if (enviadas.size() > MAX_FOTOS) {
            return "Envie no máximo " + MAX_FOTOS + " fotos.";
        }
        return null;
    }

    public List<Chamado> listarEmAndamento() {
        return chamadoRepository.findByStatusInOrderByDataAberturaDesc(STATUS_EM_ANDAMENTO);
    }

    public List<Chamado> listarTodos() {
        return chamadoRepository.findAllByOrderByDataAberturaDesc();
    }

    public List<Chamado> buscar(String termo) {
        return chamadoRepository.buscar(termo);
    }

    public Optional<Chamado> buscarPorProtocolo(String protocolo) {
        return chamadoRepository.findByProtocolo(protocolo);
    }

    // ---------------------------------------------------------------

    // Reaproveita o usuário pelo e-mail; se não existir, cria como CLIENTE (sem senha, pois ainda não há login)
    private Usuario obterOuCriarUsuario(ChamadoForm form) {
        String email = form.getEmail().trim().toLowerCase();
        return usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Usuario novo = new Usuario();
            novo.setNome(form.getNomeSolicitante().trim());
            novo.setEmail(email);
            novo.setTelefone(form.getTelefone().trim());
            novo.setTipo("CLIENTE");
            novo.setAtivo(true);
            return usuarioRepository.save(novo);
        });
    }

    private Chamado montarChamado(ChamadoForm form, TipoOcorrencia tipo, Usuario usuario,
                                  List<MultipartFile> fotos) {
        String protocolo = gerarProtocolo();

        Localizacao localizacao = new Localizacao(
                form.getCep().trim(),
                form.getBairro().trim(),
                form.getLogradouro().trim(),
                form.getPontoReferencia().trim()
        );

        Chamado chamado = new Chamado();
        chamado.setProtocolo(protocolo);
        chamado.setUsuario(usuario);
        chamado.setTipo(tipo);
        chamado.setLocalizacao(localizacao);
        chamado.setDescricao(form.getDescricao().trim());
        chamado.setStatus("ABERTO");
        chamado.setPrioridade("MEDIA");

        int numero = 1;
        for (MultipartFile foto : fotos) {
            Anexo anexo = new Anexo();
            anexo.setChamado(chamado);
            anexo.setUsuario(usuario);
            anexo.setNomeArquivo(nomeOriginal(foto));
            anexo.setCaminhoArquivo(protocolo + "-" + numero++ + "." + extensao(foto));
            anexo.setTipoArquivo(foto.getContentType());
            chamado.getAnexos().add(anexo);
        }
        return chamado;
    }

    // Grava os arquivos na pasta de uploads usando o nome já registrado em cada Anexo
    private void gravarArquivos(Chamado chamado, List<MultipartFile> fotos) {
        if (fotos.isEmpty()) return;
        try {
            Files.createDirectories(pastaUploads);
            for (int i = 0; i < fotos.size(); i++) {
                Path destino = pastaUploads.resolve(chamado.getAnexos().get(i).getCaminhoArquivo());
                Files.copy(fotos.get(i).getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar as fotos do chamado", e);
        }
    }

    private List<MultipartFile> fotosEnviadas(List<MultipartFile> fotos) {
        List<MultipartFile> enviadas = new ArrayList<>();
        if (fotos != null) {
            for (MultipartFile foto : fotos) {
                if (foto != null && !foto.isEmpty()) {
                    enviadas.add(foto);
                }
            }
        }
        return enviadas;
    }

    // Formato: CH-2026-483920
    private String gerarProtocolo() {
        String protocolo;
        do {
            int numero = ThreadLocalRandom.current().nextInt(1_000_000);
            protocolo = String.format("CH-%d-%06d", Year.now().getValue(), numero);
        } while (chamadoRepository.existsByProtocolo(protocolo));
        return protocolo;
    }

    private String nomeOriginal(MultipartFile foto) {
        String nome = foto.getOriginalFilename();
        if (nome == null || nome.isBlank()) return "foto";
        nome = Paths.get(nome).getFileName().toString();
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }

    // "png", "jpg" ou null (tipo não aceito)
    private String extensao(MultipartFile foto) {
        String tipo = foto.getContentType() == null ? "" : foto.getContentType().toLowerCase();
        String nome = foto.getOriginalFilename() == null ? "" : foto.getOriginalFilename().toLowerCase();
        if (tipo.equals("image/png") && nome.endsWith(".png")) return "png";
        if (tipo.equals("image/jpeg") && (nome.endsWith(".jpg") || nome.endsWith(".jpeg"))) return "jpg";
        return null;
    }
}