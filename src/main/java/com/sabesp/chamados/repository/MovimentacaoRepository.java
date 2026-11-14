package com.sabesp.chamados.repository;

import com.sabesp.chamados.model.Movimentacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Integer> {

    // Linha do tempo em ordem cronológica (autor carregado junto para a tela)
    @EntityGraph(attributePaths = {"usuario"})
    List<Movimentacao> findByChamadoIdChamadoOrderByDataMovimentacaoAscIdMovimentacaoAsc(Integer idChamado);

    // Devolutivas, da mais recente para a mais antiga
    @EntityGraph(attributePaths = {"usuario"})
    List<Movimentacao> findByChamadoIdChamadoAndTipoOrderByDataMovimentacaoDescIdMovimentacaoDesc(Integer idChamado, String tipo);
}
