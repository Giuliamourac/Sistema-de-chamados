package com.sabesp.chamados.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Disponibiliza o nome (e se é administrador) do usuário logado para todas as telas do cabeçalho. */
@ControllerAdvice
public class SessaoAdvice {

    @ModelAttribute("usuarioLogado")
    public String usuarioLogado(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false); // não cria sessão só para renderizar a tela
        if (sessao == null) {
            return null;
        }
        Object nome = sessao.getAttribute(AuthController.SESSAO_USUARIO_NOME);
        return nome == null ? null : nome.toString();
    }

    /** Só decide se o link "Painel" aparece no cabeçalho; o acesso real é validado no AdminInterceptor. */
    @ModelAttribute("usuarioAdmin")
    public boolean usuarioAdmin(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        return sessao != null && "ADMIN".equals(sessao.getAttribute(AuthController.SESSAO_USUARIO_TIPO));
    }
}
