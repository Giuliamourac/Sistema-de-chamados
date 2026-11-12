package com.sabesp.chamados.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senha com PBKDF2-HMAC-SHA256 (somente JDK, sem dependência extra).
 * Formato gravado em cd_senha: pbkdf2$iteracoes$salt$hash (Base64) - cabe nos 255 caracteres da coluna.
 */
@Component
public class SenhaService {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH_BITS = 256;

    private final SecureRandom random = new SecureRandom();

    public String gerarHash(String senha) {
        byte[] salt = new byte[TAMANHO_SALT];
        random.nextBytes(salt);
        byte[] hash = derivar(senha, salt, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean confere(String senha, String armazenado) {
        if (senha == null || armazenado == null) {
            return false;
        }
        String[] partes = armazenado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(senha, salt, iteracoes);
            return MessageDigest.isEqual(esperado, calculado); // comparação em tempo constante
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(String senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar o hash da senha", e);
        } finally {
            spec.clearPassword();
        }
    }
}
