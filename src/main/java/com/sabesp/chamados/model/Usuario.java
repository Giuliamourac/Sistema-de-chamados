package com.sabesp.chamados.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nm_usuario", nullable = false, length = 120)
    private String nome;

    @Column(name = "nm_email", nullable = false, unique = true, length = 150)
    private String email;

    // Ainda não há login: fica nulo. No futuro, guardar sempre o hash (BCrypt), nunca a senha pura.
    @Column(name = "cd_senha", length = 255)
    private String senha;

    @Column(name = "cd_telefone", nullable = false, length = 20)
    private String telefone;

    // ADMIN, GERENTE, SUPORTE ou CLIENTE
    @Column(name = "cd_tipo_usuario", nullable = false, length = 20)
    private String tipo = "CLIENTE";

    @Column(name = "ic_estado_usuario", nullable = false)
    private boolean ativo = true;

    @Column(name = "dt_cadastro", nullable = false, updatable = false)
    private LocalDate dataCadastro;

    public Usuario() {
    }

    @PrePersist
    protected void aoCriar() {
        if (dataCadastro == null) {
            dataCadastro = LocalDate.now();
        }
        if (tipo == null || tipo.isBlank()) {
            tipo = "CLIENTE";
        }
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDate getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDate dataCadastro) { this.dataCadastro = dataCadastro; }
}