package com.sabesp.chamados.repository;

import com.sabesp.chamados.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Integer> {

    Optional<Chamado> findByProtocolo(String protocolo);

    boolean existsByProtocolo(String protocolo);

    // Histórico completo: mais recentes primeiro
    List<Chamado> findAllByOrderByDataAberturaDesc();

    // Tela "Acompanhe suas solicitações": só os que ainda estão em andamento
    List<Chamado> findByStatusInOrderByDataAberturaDesc(Collection<String> status);

    // Busca do cabeçalho / histórico (protocolo, bairro, tipo ou logradouro)
    @Query("""
            select c from Chamado c
            join c.tipo t
            join c.localizacao l
            where lower(c.protocolo) like lower(concat('%', :q, '%'))
               or lower(l.bairro) like lower(concat('%', :q, '%'))
               or lower(t.nome) like lower(concat('%', :q, '%'))
               or lower(l.logradouro) like lower(concat('%', :q, '%'))
            order by c.dataAbertura desc
            """)
    List<Chamado> buscar(@Param("q") String q);
}