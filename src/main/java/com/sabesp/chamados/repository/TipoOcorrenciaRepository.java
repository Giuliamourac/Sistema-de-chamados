package com.sabesp.chamados.repository;

import com.sabesp.chamados.model.TipoOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoOcorrenciaRepository extends JpaRepository<TipoOcorrencia, Integer> {

    List<TipoOcorrencia> findByAtivoTrueOrderByIdTipoOcorrenciaAsc();

    Optional<TipoOcorrencia> findByNomeIgnoreCaseAndAtivoTrue(String nome);
}