package com.sabesp.chamados.service;

import com.sabesp.chamados.dto.CadastroForm;
import com.sabesp.chamados.model.Usuario;
import com.sabesp.chamados.repository.UsuarioRepository;
import com.sabesp.chamados.security.SenhaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    public static final int SENHA_MINIMA = 8;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Erro de regra de negócio/validação com mensagem pronta para a tela. */
    public static class UsuarioException extends RuntimeException {
        public UsuarioException(String mensagem) {
            super(mensagem);
        }
    }

    private final UsuarioRepository usuarioRepository;
    private final SenhaService senhaService;
    // Hash fixo usado quando o e-mail não existe, para o tempo de resposta não revelar se a conta existe
    private final String hashFalso;

    public UsuarioService(UsuarioRepository usuarioRepository, SenhaService senhaService) {
        this.usuarioRepository = usuarioRepository;
        this.senhaService = senhaService;
        this.hashFalso = senhaService.gerarHash("senha-inexistente");
    }

    /**
     * Cadastra um novo usuário CLIENTE. Se o e-mail já existe porque a pessoa abriu um chamado antes
     * (conta criada sem senha), a conta é "reivindicada": ganha a senha e mantém o histórico de chamados.
     */
    @Transactional
    public Usuario cadastrar(CadastroForm form) {
        String nome = limpar(form.getNome());
        String email = limpar(form.getEmail()).toLowerCase();
        String telefone = limpar(form.getTelefone());
        String senha = form.getSenha() == null ? "" : form.getSenha();

        if (nome.isEmpty() || nome.length() > 120) {
            throw new UsuarioException("Informe seu nome completo (até 120 caracteres).");
        }
        if (email.isEmpty() || email.length() > 150 || !EMAIL.matcher(email).matches()) {
            throw new UsuarioException("Informe um e-mail válido.");
        }
        int digitos = telefone.replaceAll("\\D", "").length();
        if (digitos < 10 || digitos > 11) {
            throw new UsuarioException("Informe um telefone válido com DDD.");
        }
        if (senha.length() < SENHA_MINIMA || senha.length() > 72) {
            throw new UsuarioException("A senha deve ter entre " + SENHA_MINIMA + " e 72 caracteres.");
        }
        if (!senha.equals(form.getConfirmarSenha())) {
            throw new UsuarioException("As senhas não conferem.");
        }

        Optional<Usuario> existente = usuarioRepository.findByEmailIgnoreCase(email);
        if (existente.isPresent() && existente.get().getSenha() != null) {
            throw new UsuarioException("Já existe uma conta com este e-mail. Faça login.");
        }

        Usuario usuario = existente.orElseGet(Usuario::new);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setSenha(senhaService.gerarHash(senha));
        if (existente.isEmpty()) {
            usuario.setTipo("CLIENTE");
            usuario.setAtivo(true);
        }
        return usuarioRepository.save(usuario);
    }

    /** Valida e-mail e senha. Mensagem genérica de propósito: não revela se o e-mail existe. */
    @Transactional(readOnly = true)
    public Usuario autenticar(String emailInformado, String senha) {
        String email = limpar(emailInformado).toLowerCase();
        if (email.isEmpty() || senha == null || senha.isEmpty()) {
            throw new UsuarioException("Informe seu e-mail e sua senha.");
        }

        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(email);
        boolean senhaOk = senhaService.confere(senha,
                encontrado.map(Usuario::getSenha).orElse(hashFalso));

        if (encontrado.isEmpty() || !senhaOk) {
            throw new UsuarioException("E-mail ou senha inválidos.");
        }
        if (!encontrado.get().isAtivo()) {
            throw new UsuarioException("Esta conta está desativada.");
        }
        return encontrado.get();
    }

    private String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
