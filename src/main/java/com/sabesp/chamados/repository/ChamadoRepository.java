package com.sabesp.chamados.repository;

import com.sabesp.chamados.model.Chamado;
import org.springframework.data.jpa.repository.EntityGraph;
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

    // Somente chamados do usuário logado
    List<Chamado> findByUsuarioIdUsuarioAndStatusInOrderByDataAberturaDesc(Integer idUsuario, Collection<String> status);

    List<Chamado> findByUsuarioIdUsuarioOrderByDataAberturaDesc(Integer idUsuario);

    // A tela de detalhe usa tudo isto fora de transação, então carrega junto.
    // (Num EntityGraph, o que não estiver listado vira LAZY, mesmo sendo EAGER na entidade: por isso tipo e localizacao entram.)
    @EntityGraph(attributePaths = {"usuario", "anexos", "tipo", "localizacao"})
    Optional<Chamado> findByProtocoloAndUsuarioIdUsuario(String protocolo, Integer idUsuario);

    // ---- Painel do administrador (todos os chamados, de qualquer cliente) ----

    @EntityGraph(attributePaths = {"usuario", "anexos", "tipo", "localizacao"})
    List<Chamado> findAllDetalhadoByOrderByDataAberturaDesc();

    @EntityGraph(attributePaths = {"usuario", "anexos", "tipo", "localizacao"})
    Optional<Chamado> findDetalhadoByProtocolo(String protocolo);

    // Busca do histórico (protocolo, bairro, tipo ou logradouro) restrita ao usuário
    @Query("""
            select c from Chamado c
            join c.tipo t
            join c.localizacao l
            where c.usuario.idUsuario = :idUsuario
              and (lower(c.protocolo) like lower(concat('%', :q, '%'))
                or lower(l.bairro) like lower(concat('%', :q, '%'))
                or lower(t.nome) like lower(concat('%', :q, '%'))
                or lower(l.logradouro) like lower(concat('%', :q, '%')))
            order by c.dataAbertura desc
            """)
    List<Chamado> buscarDoUsuario(@Param("idUsuario") Integer idUsuario, @Param("q") String q);
}