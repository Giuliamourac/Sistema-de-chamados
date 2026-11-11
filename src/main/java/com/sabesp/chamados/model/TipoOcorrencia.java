package com.sabesp.chamados.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Tipo_ocorrencia")
public class TipoOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_ocorrencia")
    private Integer idTipoOcorrencia;

    @Column(name = "nm_tipo_ocorrencia", nullable = false, unique = true, length = 100)
    private String nome;

    @Column(name = "ds_tipo_ocorrencia", length = 255)
    private String descricao;

    @Column(name = "ic_tipo_ocorrencia", nullable = false)
    private boolean ativo = true;

    public TipoOcorrencia() {
    }

    public TipoOcorrencia(String nome) {
        this.nome = nome;
    }

    public Integer getIdTipoOcorrencia() { return idTipoOcorrencia; }
    public void setIdTipoOcorrencia(Integer idTipoOcorrencia) { this.idTipoOcorrencia = idTipoOcorrencia; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}