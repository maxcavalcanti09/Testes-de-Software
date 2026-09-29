package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.entity.StatusTarefa;
import com.empresa.taskmanager.entity.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findAllByOrderByIdAsc();

    List<Tarefa> findAllByStatusOrderByIdAsc(StatusTarefa status);

    Optional<Tarefa> findByTituloIgnoreCase(String titulo);

    boolean existsByTituloIgnoreCaseAndStatusNot(String titulo, StatusTarefa status);

    boolean existsByTituloIgnoreCaseAndStatusNotAndIdNot(String titulo, StatusTarefa status, Long id);
}
