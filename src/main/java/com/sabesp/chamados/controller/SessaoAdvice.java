package com.sabesp.chamados.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Disponibiliza o nome do usuário logado para todas as telas (cabeçalho mostra Login ou Sair). */
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
}
