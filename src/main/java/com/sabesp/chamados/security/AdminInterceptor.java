package com.sabesp.chamados.security;

import com.sabesp.chamados.controller.AuthController;
import com.sabesp.chamados.service.AtendimentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protege todas as rotas /admin/** no servidor:
 * - sem login: manda para o login (e volta para a página depois);
 * - logado, mas sem perfil de administrador ativo (conferido no banco): 403;
 * - POST sem o token anti-CSRF da sessão: 403.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    public static final String SESSAO_CSRF = "csrfToken";
    public static final String PARAMETRO_CSRF = "_csrf";

    private final AtendimentoService atendimentoService;
    private final SecureRandom random = new SecureRandom();

    public AdminInterceptor(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession sessao = request.getSession(false);
        Object id = sessao == null ? null : sessao.getAttribute(AuthController.SESSAO_USUARIO_ID);

        if (!(id instanceof Integer idUsuario)) {
            HttpSession nova = request.getSession(true);
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String caminho = request.getRequestURI().substring(request.getContextPath().length());
                nova.setAttribute(AuthController.SESSAO_DESTINO, caminho);
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        if (atendimentoService.administrador(idUsuario).isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso restrito a administradores.");
            return false;
        }

        String token = (String) sessao.getAttribute(SESSAO_CSRF);
        if (token == null) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            sessao.setAttribute(SESSAO_CSRF, token);
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String enviado = request.getParameter(PARAMETRO_CSRF);
            if (enviado == null || !MessageDigest.isEqual(
                    enviado.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Formulário expirado. Recarregue a página.");
                return false;
            }
        }
        return true;
    }
}
