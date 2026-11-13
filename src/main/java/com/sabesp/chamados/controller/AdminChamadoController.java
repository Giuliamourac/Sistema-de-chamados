package com.sabesp.chamados.controller;

import com.sabesp.chamados.model.Chamado;
import com.sabesp.chamados.model.StatusChamado;
import com.sabesp.chamados.security.AdminInterceptor;
import com.sabesp.chamados.service.AtendimentoService;
import com.sabesp.chamados.service.AtendimentoService.AtendimentoException;
import com.sabesp.chamados.service.ChamadoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Painel do administrador. O acesso a /admin/** é validado pelo AdminInterceptor
 * (login + perfil ADMIN conferido no banco + token anti-CSRF nos POSTs).
 */
@Controller
@RequestMapping("/admin")
public class AdminChamadoController {

    private static final Pattern PROTOCOLO = Pattern.compile("[A-Za-z0-9-]{1,30}");
    private static final Set<String> CONTEUDOS = Set.of("com", "sem");
    private static final Set<String> TEMPOS = Set.of("ate30", "30a90", "mais90");

    private final AtendimentoService atendimentoService;
    private final ChamadoService chamadoService;

    public AdminChamadoController(AtendimentoService atendimentoService, ChamadoService chamadoService) {
        this.atendimentoService = atendimentoService;
        this.chamadoService = chamadoService;
    }

    // Token anti-CSRF disponível para todos os formulários do painel
    @ModelAttribute("csrfToken")
    public Object csrfToken(HttpSession sessao) {
        return sessao.getAttribute(AdminInterceptor.SESSAO_CSRF);
    }

    private Integer usuarioId(HttpSession sessao) {
        return sessao.getAttribute(AuthController.SESSAO_USUARIO_ID) instanceof Integer id ? id : null;
    }

    @GetMapping
    public String inicio() {
        return "redirect:/admin/chamados";
    }

    // ---------- Lista de todos os chamados ----------

    @GetMapping("/chamados")
    public String listar(@RequestParam(name = "status", required = false) List<String> status,
                         @RequestParam(name = "conteudo", required = false) List<String> conteudo,
                         @RequestParam(name = "tempo", required = false) List<String> tempo,
                         @RequestParam(name = "q", required = false) String q,
                         Model model) {

        Set<String> filtroStatus = apenasValidos(status, StatusChamado.OPCOES_ADMIN.stream().map(Enum::name).toList());
        Set<String> filtroConteudo = apenasValidos(conteudo, CONTEUDOS);
        Set<String> filtroTempo = apenasValidos(tempo, TEMPOS);
        String busca = q == null ? "" : q.trim();

        List<Chamado> todos = atendimentoService.listarTodos();
        List<Chamado> filtrados = atendimentoService.filtrar(todos,
                new AtendimentoService.Filtro(filtroStatus, filtroConteudo, filtroTempo, busca));
        Map<StatusChamado, Long> contagem = atendimentoService.contarPorStatus(todos);

        model.addAttribute("chamados", filtrados);
        model.addAttribute("contagem", contagem);
        model.addAttribute("total", todos.size());
        model.addAttribute("opcoesStatus", StatusChamado.OPCOES_ADMIN);
        model.addAttribute("filtroStatus", filtroStatus);
        model.addAttribute("filtroConteudo", filtroConteudo);
        model.addAttribute("filtroTempo", filtroTempo);
        model.addAttribute("q", busca);
        model.addAttribute("temFiltro", !filtroStatus.isEmpty() || !filtroConteudo.isEmpty()
                || !filtroTempo.isEmpty() || !busca.isEmpty());
        return "admin/chamados";
    }

    // ---------- Detalhe / gerenciamento de um chamado ----------

    @GetMapping("/chamados/{protocolo}")
    public String detalhe(@PathVariable String protocolo, Model model, RedirectAttributes redirect) {
        Optional<Chamado> encontrado = PROTOCOLO.matcher(protocolo).matches()
                ? atendimentoService.buscarParaAdmin(protocolo)
                : Optional.empty();
        if (encontrado.isEmpty()) {
            redirect.addFlashAttribute("erro", "Chamado não encontrado.");
            return "redirect:/admin/chamados";
        }
        Chamado chamado = encontrado.get();
        model.addAttribute("chamado", chamado);
        model.addAttribute("movimentacoes", atendimentoService.linhaDoTempo(chamado));
        model.addAttribute("opcoesStatus", StatusChamado.OPCOES_ADMIN);
        return "admin/detalhe";
    }

    // Foto anexada pelo cliente (o administrador vê de qualquer chamado)
    @GetMapping("/chamados/{protocolo}/foto/{indice}")
    public ResponseEntity<Resource> foto(@PathVariable String protocolo, @PathVariable int indice) {
        if (!PROTOCOLO.matcher(protocolo).matches()) {
            return ResponseEntity.notFound().build();
        }
        return atendimentoService.buscarParaAdmin(protocolo)
                .flatMap(chamado -> chamadoService.arquivoDoAnexo(chamado, indice))
                .<ResponseEntity<Resource>>map(arquivo -> ResponseEntity.ok()
                        .contentType(arquivo.tipo())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(new FileSystemResource(arquivo.caminho())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/chamados/{protocolo}/status")
    public String alterarStatus(@PathVariable String protocolo,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String comentario,
                                HttpSession sessao, RedirectAttributes redirect) {
        if (!PROTOCOLO.matcher(protocolo).matches()) {
            return "redirect:/admin/chamados";
        }
        try {
            atendimentoService.alterarStatus(protocolo, status, comentario, usuarioId(sessao));
            redirect.addFlashAttribute("sucesso", "Status atualizado.");
        } catch (AtendimentoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            redirect.addFlashAttribute("secaoErro", "status");
            redirect.addFlashAttribute("rascunhoComentario", comentario);
        }
        return voltarParaDetalhe("status");
    }

    @PostMapping("/chamados/{protocolo}/atualizacao")
    public String registrarAtualizacao(@PathVariable String protocolo,
                                       @RequestParam(required = false) String descricao,
                                       HttpSession sessao, RedirectAttributes redirect) {
        if (!PROTOCOLO.matcher(protocolo).matches()) {
            return "redirect:/admin/chamados";
        }
        try {
            atendimentoService.registrarAtualizacao(protocolo, descricao, usuarioId(sessao));
            redirect.addFlashAttribute("sucesso", "Atualização registrada na linha do tempo.");
        } catch (AtendimentoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            redirect.addFlashAttribute("secaoErro", "atualizacao");
            redirect.addFlashAttribute("rascunhoAtualizacao", descricao);
        }
        return voltarParaDetalhe("atualizacao");
    }

    @PostMapping("/chamados/{protocolo}/devolutiva")
    public String enviarDevolutiva(@PathVariable String protocolo,
                                   @RequestParam(required = false) String problema,
                                   @RequestParam(required = false) String solucao,
                                   @RequestParam(required = false) String observacoes,
                                   @RequestParam(defaultValue = "false") boolean concluir,
                                   HttpSession sessao, RedirectAttributes redirect) {
        if (!PROTOCOLO.matcher(protocolo).matches()) {
            return "redirect:/admin/chamados";
        }
        try {
            atendimentoService.enviarDevolutiva(protocolo, problema, solucao, observacoes, concluir, usuarioId(sessao));
            redirect.addFlashAttribute("sucesso", "Devolutiva enviada ao cliente.");
        } catch (AtendimentoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            redirect.addFlashAttribute("secaoErro", "devolutiva");
            redirect.addFlashAttribute("rascunhoProblema", problema);
            redirect.addFlashAttribute("rascunhoSolucao", solucao);
            redirect.addFlashAttribute("rascunhoObservacoes", observacoes);
            redirect.addFlashAttribute("rascunhoConcluir", concluir);
        }
        return voltarParaDetalhe("devolutiva");
    }

    // ---------------------------------------------------------------

    // {protocolo} é preenchido (e codificado) pelo Spring com a variável da própria URL do POST
    private String voltarParaDetalhe(String secao) {
        return "redirect:/admin/chamados/{protocolo}#" + secao;
    }

    private Set<String> apenasValidos(List<String> recebidos, Collection<String> permitidos) {
        Set<String> validos = new LinkedHashSet<>();
        if (recebidos != null) {
            for (String valor : recebidos) {
                if (valor != null && permitidos.contains(valor.trim())) {
                    validos.add(valor.trim());
                }
            }
        }
        return validos;
    }
}
