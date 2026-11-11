package com.sabesp.chamados.config;

import com.sabesp.chamados.model.Chamado;
import com.sabesp.chamados.model.Localizacao;
import com.sabesp.chamados.model.TipoOcorrencia;
import com.sabesp.chamados.model.Usuario;
import com.sabesp.chamados.repository.ChamadoRepository;
import com.sabesp.chamados.repository.TipoOcorrenciaRepository;
import com.sabesp.chamados.repository.UsuarioRepository;
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

    public DadosDeTesteDev(TipoOcorrenciaRepository tipoRepository,
                           UsuarioRepository usuarioRepository,
                           ChamadoRepository chamadoRepository) {
        this.tipoRepository = tipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.chamadoRepository = chamadoRepository;
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
        usuario = usuarioRepository.save(usuario);

        TipoOcorrencia vazamento = tipoRepository.findByNomeIgnoreCaseAndAtivoTrue("Vazamento de água na rua").orElseThrow();
        TipoOcorrencia bueiro = tipoRepository.findByNomeIgnoreCaseAndAtivoTrue("Bueiro com vazamento").orElseThrow();

        String descricao = "Rua se encontra com um cano estourado do qual esta vazando água a mais de semanas "
                + "e prejudicando a circulação";

        criar(1, usuario, vazamento, "ABERTO", descricao, null);
        criar(2, usuario, bueiro, "EM_ANDAMENTO", descricao, null);

        // Cancelado, aberto há 4 dias e 7 horas (como no Figma)
        LocalDateTime abertura = LocalDateTime.now().minusDays(4).minusHours(7);
        Chamado cancelado = criar(3, usuario, vazamento, "CANCELADO", descricao, abertura);
        cancelado.setDataConclusao(LocalDateTime.now());
        chamadoRepository.save(cancelado);
    }

    private Chamado criar(int numero, Usuario usuario, TipoOcorrencia tipo, String status,
                          String descricao, LocalDateTime abertura) {
        Localizacao local = new Localizacao("11060-000", "Gonzaga", "Rua Afonso Pena, 584",
                "Próximo à praça");

        Chamado chamado = new Chamado();
        chamado.setProtocolo(String.format("CH-%d-%06d", Year.now().getValue(), numero));
        chamado.setUsuario(usuario);
        chamado.setTipo(tipo);
        chamado.setLocalizacao(local);
        chamado.setDescricao(descricao);
        chamado.setStatus(status);
        chamado.setDataAbertura(abertura);
        return chamadoRepository.save(chamado);
    }
}