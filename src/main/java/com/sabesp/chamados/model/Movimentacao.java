package com.sabesp.chamados.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro da linha do tempo de um chamado (tabela Movimentacao_ocorrencia).
 * Só é inserido, nunca alterado: cada ação gera um registro novo.
 */
@Entity
@Table(name = "Movimentacao_ocorrencia")
public class Movimentacao {

    public static final String ABERTURA = "ABERTURA";
    public static final String STATUS = "STATUS";
    public static final String ATUALIZACAO = "ATUALIZACAO";
    public static final String DEVOLUTIVA = "DEVOLUTIVA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimentacao")
    private Integer idMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ocorrencia", nullable = false, updatable = false)
    private Chamado chamado;

    // Quem fez a movimentação (cliente na abertura, administrador nas demais)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    // ABERTURA, STATUS, ATUALIZACAO ou DEVOLUTIVA
    @Column(name = "cd_tipo_movimentacao", nullable = false, length = 20, updatable = false)
    private String tipo;

    @Column(name = "cd_status_anterior", length = 30, updatable = false)
    private String statusAnterior;

    @Column(name = "cd_status_novo", length = 30, updatable = false)
    private String statusNovo;

    // Texto da atualização ou comentário da troca de status
    @Lob
    @Column(name = "ds_movimentacao", updatable = false)
    private String descricao;

    // Campos da devolutiva
    @Lob
    @Column(name = "ds_problema_identificado", updatable = false)
    private String problemaIdentificado;

    @Lob
    @Column(name = "ds_solucao_aplicada", updatable = false)
    private String solucaoAplicada;

    @Lob
    @Column(name = "ds_observacoes", updatable = false)
    private String observacoes;

    @Column(name = "dt_movimentacao", nullable = false, updatable = false)
    private LocalDateTime dataMovimentacao;

    public Movimentacao() {
    }

    public Movimentacao(Chamado chamado, Usuario usuario, String tipo) {
        this.chamado = chamado;
        this.usuario = usuario;
        this.tipo = tipo;
    }

    @PrePersist
    protected void aoCriar() {
        if (dataMovimentacao == null) {
            dataMovimentacao = LocalDateTime.now();
        }
    }

    // ---- Apoio para as telas (não são colunas do banco) ----

    public boolean isAbertura() { return ABERTURA.equals(tipo); }
    public boolean isTrocaStatus() { return STATUS.equals(tipo); }
    public boolean isAtualizacao() { return ATUALIZACAO.equals(tipo); }
    public boolean isDevolutiva() { return DEVOLUTIVA.equals(tipo); }

    public String getTitulo() {
        return switch (tipo == null ? "" : tipo) {
            case ABERTURA -> "Solicitação aberta";
            case STATUS -> "Status alterado para " + rotuloStatus(statusNovo);
            case ATUALIZACAO -> "Atualização do atendimento";
            case DEVOLUTIVA -> "Devolutiva da equipe";
            default -> "Movimentação";
        };
    }

    // Classe extra do item na linha do tempo. Fica no Java porque o Thymeleaf trata "__" dentro de
    // th:* como pré-processamento, e as classes BEM (linha-tempo__item) têm "__".
    public String getClasseItem() {
        if (isDevolutiva()) return "linha-tempo__item--devolutiva";
        if (isTrocaStatus()) return "linha-tempo__item--status";
        return "";
    }

    // Classe do selo do status novo (para colorir a troca de status na linha do tempo)
    public String getStatusNovoClasse() {
        return StatusChamado.de(statusNovo).map(StatusChamado::getClasse).orElse("em-andamento");
    }

    public String getStatusAnteriorRotulo() {
        return rotuloStatus(statusAnterior);
    }

    public String getStatusNovoRotulo() {
        return rotuloStatus(statusNovo);
    }

    // "Administrador", "Cliente"... para identificar quem fez a movimentação
    public String getPerfilAutor() {
        if (usuario == null || usuario.getTipo() == null) return "";
        return switch (usuario.getTipo()) {
            case "ADMIN" -> "Administrador";
            case "GERENTE" -> "Gerente";
            case "SUPORTE" -> "Suporte";
            default -> "Cliente";
        };
    }

    private static String rotuloStatus(String codigo) {
        return StatusChamado.de(codigo).map(StatusChamado::getRotulo).orElse(codigo == null ? "" : codigo);
    }

    // ---- Getters e Setters ----

    public Integer getIdMovimentacao() { return idMovimentacao; }
    public void setIdMovimentacao(Integer idMovimentacao) { this.idMovimentacao = idMovimentacao; }

    public Chamado getChamado() { return chamado; }
    public void setChamado(Chamado chamado) { this.chamado = chamado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(String statusAnterior) { this.statusAnterior = statusAnterior; }

    public String getStatusNovo() { return statusNovo; }
    public void setStatusNovo(String statusNovo) { this.statusNovo = statusNovo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getProblemaIdentificado() { return problemaIdentificado; }
    public void setProblemaIdentificado(String problemaIdentificado) { this.problemaIdentificado = problemaIdentificado; }

    public String getSolucaoAplicada() { return solucaoAplicada; }
    public void setSolucaoAplicada(String solucaoAplicada) { this.solucaoAplicada = solucaoAplicada; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(LocalDateTime dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }
}
