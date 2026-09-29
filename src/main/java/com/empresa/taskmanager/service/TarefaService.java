package com.empresa.taskmanager.service;

import com.empresa.taskmanager.dto.TarefaAtualizacaoDTO;
import com.empresa.taskmanager.dto.TarefaRequestDTO;
import com.empresa.taskmanager.dto.TarefaResponseDTO;
import com.empresa.taskmanager.entity.StatusTarefa;
import com.empresa.taskmanager.entity.Tarefa;
import com.empresa.taskmanager.exception.RecursoNaoEncontradoException;
import com.empresa.taskmanager.exception.RegraDeNegocioException;
import com.empresa.taskmanager.repository.TarefaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TarefaService {

    private static final int TAMANHO_MINIMO_TITULO = 5;
    private static final int TAMANHO_MAXIMO_TITULO = 100;
    private static final int TAMANHO_MAXIMO_DESCRICAO = 255;

    private final TarefaRepository tarefaRepository;

    public TarefaService(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    public TarefaResponseDTO criar(TarefaRequestDTO dto) {
        String titulo = normalizar(dto.getTitulo());
        validarTitulo(titulo);
        validarDescricao(dto.getDescricao());

        StatusTarefa status = dto.getStatus() != null ? dto.getStatus() : StatusTarefa.PENDENTE;

        if (status != StatusTarefa.CONCLUIDA) {
            validarTituloDuplicado(titulo, null);
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(dto.getDescricao());
        tarefa.setStatus(status);
        tarefa.setDataCriacao(LocalDateTime.now());
        if (status == StatusTarefa.CONCLUIDA) {
            tarefa.setDataConclusao(LocalDateTime.now());
        }

        return TarefaResponseDTO.from(tarefaRepository.save(tarefa));
    }

    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listar() {
        return tarefaRepository.findAllByOrderByIdAsc().stream()
                .map(TarefaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TarefaResponseDTO buscarPorId(Long id) {
        return TarefaResponseDTO.from(buscarEntidade(id));
    }

    public TarefaResponseDTO atualizar(Long id, TarefaAtualizacaoDTO dto) {
        Tarefa tarefa = buscarEntidade(id);
        StatusTarefa statusAtual = tarefa.getStatus();
        StatusTarefa novoStatus = dto.getStatus() != null ? dto.getStatus() : statusAtual;

        if (statusAtual == StatusTarefa.CONCLUIDA && novoStatus != StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "Nao e permitido alterar o status de uma tarefa CONCLUIDA (id " + id + ")");
        }
        if (statusAtual == StatusTarefa.PENDENTE && novoStatus == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "Nao e permitido transicionar a tarefa (id " + id + ") de PENDENTE para CONCLUIDA. "
                            + "Altere o status para EM_ANDAMENTO antes de concluir.");
        }

        if (dto.getTitulo() != null) {
            String titulo = normalizar(dto.getTitulo());
            validarTitulo(titulo);
            if (novoStatus != StatusTarefa.CONCLUIDA) {
                validarTituloDuplicado(titulo, id);
            }
            tarefa.setTitulo(titulo);
        }

        if (dto.getDescricao() != null) {
            validarDescricao(dto.getDescricao());
            tarefa.setDescricao(dto.getDescricao());
        }

        if (novoStatus == StatusTarefa.CONCLUIDA && tarefa.getDataConclusao() == null) {
            tarefa.setDataConclusao(LocalDateTime.now());
        }
        if (novoStatus != StatusTarefa.CONCLUIDA) {
            tarefa.setDataConclusao(null);
        }

        tarefa.setStatus(novoStatus);

        return TarefaResponseDTO.from(tarefaRepository.save(tarefa));
    }

    public void deletar(Long id) {
        Tarefa tarefa = buscarEntidade(id);
        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "Nao e permitido excluir a tarefa (id " + id + ") com status CONCLUIDA");
        }
        tarefaRepository.delete(tarefa);
    }

    private Tarefa buscarEntidade(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa nao encontrada com o id: " + id));
    }

    private void validarTituloDuplicado(String titulo, Long idIgnorado) {
        boolean duplicada = idIgnorado == null
                ? tarefaRepository.existsByTituloIgnoreCaseAndStatusNot(titulo, StatusTarefa.CONCLUIDA)
                : tarefaRepository.existsByTituloIgnoreCaseAndStatusNotAndIdNot(titulo, StatusTarefa.CONCLUIDA, idIgnorado);

        if (duplicada) {
            throw new RegraDeNegocioException(
                    "Ja existe uma tarefa ativa com o titulo: " + titulo);
        }
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.length() < TAMANHO_MINIMO_TITULO || titulo.length() > TAMANHO_MAXIMO_TITULO) {
            throw new RegraDeNegocioException(
                    "O titulo deve ter entre " + TAMANHO_MINIMO_TITULO + " e " + TAMANHO_MAXIMO_TITULO + " caracteres");
        }
    }

    private void validarDescricao(String descricao) {
        if (descricao != null && descricao.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new RegraDeNegocioException(
                    "A descricao deve ter no maximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        }
    }

    private String normalizar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
