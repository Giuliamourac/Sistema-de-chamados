package com.sabesp.chamados.dto;

/**
 * Dados enviados pelo formulário de abertura de chamado (3 passos).
 * Usar um DTO evita que o usuário consiga forçar id, protocolo, status ou data pelo formulário.
 */
public class ChamadoForm {

    // Passo 1 - Ocorrência
    private String tipoOcorrencia;
    private String descricao;

    // Passo 2 - Localização
    private String cep;
    private String bairro;
    private String logradouro;
    private String pontoReferencia;

    // Passo 3 - Contato
    private String nomeSolicitante;
    private String email;
    private String telefone;

    public String getTipoOcorrencia() { return tipoOcorrencia; }
    public void setTipoOcorrencia(String tipoOcorrencia) { this.tipoOcorrencia = tipoOcorrencia; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getPontoReferencia() { return pontoReferencia; }
    public void setPontoReferencia(String pontoReferencia) { this.pontoReferencia = pontoReferencia; }

    public String getNomeSolicitante() { return nomeSolicitante; }
    public void setNomeSolicitante(String nomeSolicitante) { this.nomeSolicitante = nomeSolicitante; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}