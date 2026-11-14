package com.sabesp.chamados.model;

import java.util.List;
import java.util.Optional;

/**
 * Status possíveis de um chamado (coluna Ocorrencia.cd_status).
 * O código é o valor gravado no banco; rótulo e classe são usados nas telas.
 */
public enum StatusChamado {

    ABERTO("Pendente", "pendente", false),
    EM_ANDAMENTO("Em andamento", "em-andamento", false),
    CONCLUIDO("Concluída", "concluida", true),
    FECHADO("Fechada", "fechada", true),
    CANCELADO("Cancelada", "cancelada", true);

    /** Opções que o administrador pode escolher no painel (na ordem exibida). */
    public static final List<StatusChamado> OPCOES_ADMIN = List.of(ABERTO, EM_ANDAMENTO, CONCLUIDO, FECHADO, CANCELADO);

    private final String rotulo;
    private final String classe;
    private final boolean finalizado;

    StatusChamado(String rotulo, String classe, boolean finalizado) {
        this.rotulo = rotulo;
        this.classe = classe;
        this.finalizado = finalizado;
    }

    /** Converte o código do banco; vazio se não for um status conhecido. */
    public static Optional<StatusChamado> de(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        for (StatusChamado s : values()) {
            if (s.name().equalsIgnoreCase(codigo.trim())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public String getCodigo() { return name(); }
    public String getRotulo() { return rotulo; }
    public String getClasse() { return classe; }

    /** Concluído, fechado ou cancelado: o atendimento terminou. */
    public boolean isFinalizado() { return finalizado; }
}
