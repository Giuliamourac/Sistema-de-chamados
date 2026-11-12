package com.sabesp.chamados.controller;

import com.sabesp.chamados.dto.CadastroForm;
import com.sabesp.chamados.dto.LoginForm;
import com.sabesp.chamados.model.Usuario;
import com.sabesp.chamados.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    public static final String SESSAO_USUARIO_ID = "usuarioId";
    public static final String SESSAO_USUARIO_NOME = "usuarioNome";
    /** Página para onde voltar depois do login (definida só pelo servidor, nunca vem do usuário). */
    public static final String SESSAO_DESTINO = "destinoLogin";

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ---------- Login ----------

    @GetMapping("/login")
    public String login(HttpSession sessao, Model model) {
        if (sessao.getAttribute(SESSAO_USUARIO_ID) != null) {
            return "redirect:/";
        }
        if (sessao.getAttribute(SESSAO_DESTINO) != null && !model.containsAttribute("sucesso")) {
            model.addAttribute("sucesso", "Faça login para continuar.");
        }
        model.addAttribute("form", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@ModelAttribute("form") LoginForm form, HttpServletRequest request, Model model) {
        try {
            Usuario usuario = usuarioService.autenticar(form.getEmail(), form.getSenha());
            HttpSession antiga = request.getSession(false);
            Object destino = antiga == null ? null : antiga.getAttribute(SESSAO_DESTINO);
            iniciarSessao(request, usuario);
            return "redirect:" + (destino instanceof String d && d.startsWith("/") && !d.startsWith("//") ? d : "/");
        } catch (UsuarioService.UsuarioException e) {
            form.setSenha(null); // nunca devolve a senha para a tela
            model.addAttribute("erro", e.getMessage());
            return "login";
        }
    }

    // ---------- Cadastro ----------

    @GetMapping("/cadastro")
    public String cadastro(HttpSession sessao, Model model) {
        if (sessao.getAttribute(SESSAO_USUARIO_ID) != null) {
            return "redirect:/";
        }
        model.addAttribute("form", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@ModelAttribute("form") CadastroForm form, Model model, RedirectAttributes redirect) {
        try {
            usuarioService.cadastrar(form);
            redirect.addFlashAttribute("sucesso", "Cadastro realizado! Faça login para continuar.");
            return "redirect:/login";
        } catch (UsuarioService.UsuarioException e) {
            form.setSenha(null);
            form.setConfirmarSenha(null);
            model.addAttribute("erro", e.getMessage());
            return "cadastro";
        }
    }

    // ---------- Logout ----------

    @PostMapping("/logout")
    public String sair(HttpSession sessao) {
        sessao.invalidate();
        return "redirect:/login";
    }

    private void iniciarSessao(HttpServletRequest request, Usuario usuario) {
        HttpSession antiga = request.getSession(false);
        if (antiga != null) {
            antiga.invalidate(); // evita fixação de sessão
        }
        HttpSession sessao = request.getSession(true);
        sessao.setAttribute(SESSAO_USUARIO_ID, usuario.getIdUsuario());
        sessao.setAttribute(SESSAO_USUARIO_NOME, usuario.getNome());
    }
}
