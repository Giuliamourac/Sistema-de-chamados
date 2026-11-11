package com.sabesp.chamados.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "Anexo")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anexo")
    private Integer idAnexo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ocorrencia", nullable = false)
    private Chamado chamado;

    // Nome original do arquivo enviado
    @Column(name = "nm_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    // Nome com que o arquivo foi gravado na pasta de uploads
    @Column(name = "ds_caminho_arquivo", nullable = false, length = 500)
    private String caminhoArquivo;

    // Ex.: image/png
    @Column(name = "cd_tipo_arquivo", nullable = false, length = 100)
    private String tipoArquivo;

    @Column(name = "dt_upload", nullable = false)
    private LocalDateTime dataUpload;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    public Anexo() {
    }

    @PrePersist
    protected void aoCriar() {
        if (dataUpload == null) {
            dataUpload = LocalDateTime.now();
        }
    }

    public Integer getIdAnexo() { return idAnexo; }
    public void setIdAnexo(Integer idAnexo) { this.idAnexo = idAnexo; }

    public Chamado getChamado() { return chamado; }
    public void setChamado(Chamado chamado) { this.chamado = chamado; }

    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

    public String getCaminhoArquivo() { return caminhoArquivo; }
    public void setCaminhoArquivo(String caminhoArquivo) { this.caminhoArquivo = caminhoArquivo; }

    public String getTipoArquivo() { return tipoArquivo; }
    public void setTipoArquivo(String tipoArquivo) { this.tipoArquivo = tipoArquivo; }

    public LocalDateTime getDataUpload() { return dataUpload; }
    public void setDataUpload(LocalDateTime dataUpload) { this.dataUpload = dataUpload; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}