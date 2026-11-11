package com.sabesp.chamados.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "Localizacao")
public class Localizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localizacao")
    private Integer idLocalizacao;

    // CHAR(9) no banco: 00000-000
    @Column(name = "cd_cep", nullable = false, columnDefinition = "char(9)")
    private String cep;

    @Column(name = "cd_bairro", nullable = false, length = 150)
    private String bairro;

    @Column(name = "cd_logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "cd_numero", length = 20)
    private String numero;

    @Column(name = "cd_complemento", length = 100)
    private String complemento;

    @Column(name = "ds_ponto_referencia", length = 255)
    private String pontoReferencia;

    @Column(name = "cd_latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "cd_longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "ic_estado_bairro", nullable = false)
    private boolean ativo = true;

    public Localizacao() {
    }

    public Localizacao(String cep, String bairro, String logradouro, String pontoReferencia) {
        this.cep = cep;
        this.bairro = bairro;
        this.logradouro = logradouro;
        this.pontoReferencia = pontoReferencia;
    }

    public Integer getIdLocalizacao() { return idLocalizacao; }
    public void setIdLocalizacao(Integer idLocalizacao) { this.idLocalizacao = idLocalizacao; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getPontoReferencia() { return pontoReferencia; }
    public void setPontoReferencia(String pontoReferencia) { this.pontoReferencia = pontoReferencia; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}