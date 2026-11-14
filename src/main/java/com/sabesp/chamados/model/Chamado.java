package com.sabesp.chamados.model;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chamado = linha da tabela Ocorrencia.
 */
@Entity
@Table(name = "Ocorrencia")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocorrencia")
    private Integer idChamado;

    @Column(name = "nm_protocolo", nullable = false, unique = true, length = 30)
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_ocorrencia", nullable = false)
    private TipoOcorrencia tipo;

    // Cada chamado cria a sua Localizacao (cascade PERSIST salva junto)
    @ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_localizacao", nullable = false)
    private Localizacao localizacao;

    @Lob
    @Column(name = "ds_chamado", nullable = false)
    private String descricao;

    @Column(name = "dt_ocorrencia")
    private LocalDateTime dataOcorrencia;

    @Column(name = "dt_abertura", nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    // BAIXA, MEDIA, ALTA ou CRITICA
    @Column(name = "cd_prioridade", nullable = false, length = 20)
    private String prioridade = "MEDIA";

    // ABERTO (pendente), EM_ANDAMENTO, CONCLUIDO, FECHADO ou CANCELADO (ver StatusChamado)
    @Column(name = "cd_status", nullable = false, length = 30)
    private String status = "ABERTO";

    @Lob
    @Column(name = "ds_solucao_aplicada")
    private String solucaoAplicada;

    // Preenchida quando o chamado é concluído, fechado ou cancelado
    @Column(name = "dt_conclusao")
    private LocalDateTime dataConclusao;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL)
    @OrderBy("idAnexo ASC")
    private List<Anexo> anexos = new ArrayList<>();

    // Linha do tempo. Usada só para gravar a abertura junto com o chamado;
    // as telas leem a linha do tempo pelo MovimentacaoRepository.
    @OneToMany(mappedBy = "chamado", cascade = CascadeType.PERSIST)
    @OrderBy("dataMovimentacao ASC, idMovimentacao ASC")
    private List<Movimentacao> movimentacoes = new ArrayList<>();

    public Chamado() {
    }

    @PrePersist
    protected void aoCriar() {
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "ABERTO";
        }
        if (prioridade == null || prioridade.isBlank()) {
            prioridade = "MEDIA";
        }
    }

    @PreUpdate
    protected void aoAtualizar() {
        if (dataConclusao == null && getStatusEnum().isFinalizado()) {
            dataConclusao = LocalDateTime.now();
        }
    }

    // ---- Apoio para as telas (não são colunas do banco) ----

    // Status desconhecido é tratado como "Em andamento" (mesmo comportamento de antes)
    public StatusChamado getStatusEnum() {
        return StatusChamado.de(status).orElse(StatusChamado.EM_ANDAMENTO);
    }

    private boolean isCancelado() {
        return getStatusEnum() == StatusChamado.CANCELADO;
    }

    public boolean isFinalizado() {
        return getStatusEnum().isFinalizado();
    }

    public String getTipoOcorrencia() {
        return tipo != null ? tipo.getNome() : null;
    }

    public String getBairro() {
        return localizacao != null ? localizacao.getBairro() : null;
    }

    public String getPontoReferencia() {
        return localizacao != null ? localizacao.getPontoReferencia() : null;
    }

    public String getStatusRotulo() {
        return getStatusEnum().getRotulo();
    }

    public String getStatusClasse() {
        return getStatusEnum().getClasse();
    }

    // Ex.: "há 20 dias", "hoje" (cartões do painel do administrador)
    public String getAbertoHa() {
        if (dataAbertura == null) return "-";
        long dias = Duration.between(dataAbertura, LocalDateTime.now()).toDays();
        if (dias <= 0) return "hoje";
        return "há " + dias + (dias == 1 ? " dia" : " dias");
    }

    public long getDiasDesdeAbertura() {
        if (dataAbertura == null) return 0;
        return Math.max(0, Duration.between(dataAbertura, LocalDateTime.now()).toDays());
    }

    // Ex.: "Rua Afonso Pena, 584, Gonzaga - SP, 11060-000"
    public String getEnderecoCompleto() {
        if (localizacao == null) return "";
        List<String> partes = new ArrayList<>();
        String rua = localizacao.getLogradouro();
        if (rua != null && !rua.isBlank()) {
            String numero = localizacao.getNumero();
            partes.add(numero != null && !numero.isBlank() ? rua.trim() + ", " + numero.trim() : rua.trim());
        }
        if (localizacao.getBairro() != null && !localizacao.getBairro().isBlank()) {
            partes.add(localizacao.getBairro().trim() + " - SP");
        }
        if (localizacao.getCep() != null && !localizacao.getCep().isBlank()) {
            partes.add(localizacao.getCep().trim());
        }
        return String.join(", ", partes);
    }

    // Prazo de 15 dias para recorrer de um cancelamento (contado a partir da data de conclusão)
    public long getDiasParaRecorrer() {
        if (!isCancelado() || dataConclusao == null) return 0;
        long passados = Duration.between(dataConclusao, LocalDateTime.now()).toDays();
        return Math.max(0, 15 - passados);
    }

    // Ex.: "4 dias e 7 horas"
    public String getTempoEmAberto() {
        if (dataAbertura == null) return "-";
        LocalDateTime fim = dataConclusao != null ? dataConclusao : LocalDateTime.now();
        Duration d = Duration.between(dataAbertura, fim);
        long dias = d.toDays();
        long horas = d.toHoursPart();
        long minutos = d.toMinutesPart();
        if (dias > 0) {
            return dias + (dias == 1 ? " dia" : " dias") + " e " + horas + (horas == 1 ? " hora" : " horas");
        }
        if (horas > 0) {
            return horas + (horas == 1 ? " hora" : " horas") + " e " + minutos + (minutos == 1 ? " minuto" : " minutos");
        }
        return minutos + (minutos == 1 ? " minuto" : " minutos");
    }

    // ---- Getters e Setters ----

    public Integer getIdChamado() { return idChamado; }
    public void setIdChamado(Integer idChamado) { this.idChamado = idChamado; }

    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public TipoOcorrencia getTipo() { return tipo; }
    public void setTipo(TipoOcorrencia tipo) { this.tipo = tipo; }

    public Localizacao getLocalizacao() { return localizacao; }
    public void setLocalizacao(Localizacao localizacao) { this.localizacao = localizacao; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDateTime getDataOcorrencia() { return dataOcorrencia; }
    public void setDataOcorrencia(LocalDateTime dataOcorrencia) { this.dataOcorrencia = dataOcorrencia; }

    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }

    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSolucaoAplicada() { return solucaoAplicada; }
    public void setSolucaoAplicada(String solucaoAplicada) { this.solucaoAplicada = solucaoAplicada; }

    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDateTime dataConclusao) { this.dataConclusao = dataConclusao; }

    public List<Anexo> getAnexos() { return anexos; }
    public void setAnexos(List<Anexo> anexos) { this.anexos = anexos; }

    public List<Movimentacao> getMovimentacoes() { return movimentacoes; }
    public void setMovimentacoes(List<Movimentacao> movimentacoes) { this.movimentacoes = movimentacoes; }
}