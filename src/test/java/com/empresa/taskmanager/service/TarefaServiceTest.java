package com.empresa.taskmanager.service;

import com.empresa.taskmanager.dto.TarefaAtualizacaoDTO;
import com.empresa.taskmanager.dto.TarefaRequestDTO;
import com.empresa.taskmanager.dto.TarefaResponseDTO;
import com.empresa.taskmanager.entity.StatusTarefa;
import com.empresa.taskmanager.entity.Tarefa;
import com.empresa.taskmanager.exception.RecursoNaoEncontradoException;
import com.empresa.taskmanager.exception.RegraDeNegocioException;
import com.empresa.taskmanager.repository.TarefaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitarios da camada de Service - TarefaService")
class TarefaServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @InjectMocks
    private TarefaService tarefaService;

    private Tarefa tarefaPendente;
    private Tarefa tarefaConcluida;

    @BeforeEach
    void setUp() {
        tarefaPendente = tarefa(1L, "Implementar API de tarefas", StatusTarefa.PENDENTE);
        tarefaConcluida = tarefa(2L, "Revisar documentacao da API", StatusTarefa.CONCLUIDA);
    }

    private Tarefa tarefa(Long id, String titulo, StatusTarefa status) {
        LocalDateTime agora = LocalDateTime.now();
        Tarefa tarefa = new Tarefa(titulo, "Descricao da tarefa", status, agora,
                status == StatusTarefa.CONCLUIDA ? agora : null);
        tarefa.setId(id);
        return tarefa;
    }

    // ---------------------------------------------------------------- CRIAR

    @Test
    @DisplayName("criar - caminho feliz: cria tarefa com status PENDENTE e data de criacao preenchida")
    void deveCriarTarefaComStatusPendente() {
        TarefaRequestDTO dto = new TarefaRequestDTO("Implementar API de tarefas", "Descricao", null);
        when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNot("Implementar API de tarefas", StatusTarefa.CONCLUIDA))
                .thenReturn(false);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocacao -> {
            Tarefa salva = invocacao.getArgument(0);
            salva.setId(10L);
            return salva;
        });

        TarefaResponseDTO resposta = tarefaService.criar(dto);

        assertEquals(10L, resposta.getId());
        assertEquals("Implementar API de tarefas", resposta.getTitulo());
        assertEquals(StatusTarefa.PENDENTE, resposta.getStatus());
        assertNotNull(resposta.getDataCriacao());
        assertNull(resposta.getDataConclusao());

        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        verify(tarefaRepository, times(1)).save(captor.capture());
        assertEquals("Implementar API de tarefas", captor.getValue().getTitulo());
        assertEquals(StatusTarefa.PENDENTE, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("criar - caminho feliz: aceita status EM_ANDAMENTO informado no payload")
    void deveCriarTarefaComStatusEmAndamento() {
        TarefaRequestDTO dto = new TarefaRequestDTO("Corrigir bug do login", "Ajuste no formulario",
                StatusTarefa.EM_ANDAMENTO);
        when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNot("Corrigir bug do login", StatusTarefa.CONCLUIDA))
                .thenReturn(false);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        TarefaResponseDTO resposta = tarefaService.criar(dto);

        assertEquals(StatusTarefa.EM_ANDAMENTO, resposta.getStatus());
        assertNull(resposta.getDataConclusao());
        verify(tarefaRepository, times(1)).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("criar - regra de negocio: rejeita titulo duplicado de tarefa ativa")
    void deveRejeitarTituloDuplicado() {
        TarefaRequestDTO dto = new TarefaRequestDTO("Implementar API de tarefas", "Descricao", null);
        when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNot("Implementar API de tarefas", StatusTarefa.CONCLUIDA))
                .thenReturn(true);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.criar(dto));

        assertTrue(excecao.getMessage().contains("Ja existe uma tarefa ativa"));
        verify(tarefaRepository, never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("criar - validacao: rejeita titulo com menos de 5 caracteres")
    void deveRejeitarTituloCurto() {
        TarefaRequestDTO dto = new TarefaRequestDTO("abc", "Descricao", null);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.criar(dto));

        assertTrue(excecao.getMessage().contains("entre 5 e 100 caracteres"));
        verifyNoInteractions(tarefaRepository);
    }

    @Test
    @DisplayName("criar - validacao: rejeita descricao com mais de 255 caracteres")
    void deveRejeitarDescricaoLonga() {
        TarefaRequestDTO dto = new TarefaRequestDTO("Implementar API de tarefas", "x".repeat(256), null);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.criar(dto));

        assertTrue(excecao.getMessage().contains("255"));
        verifyNoInteractions(tarefaRepository);
    }

    @Test
    @DisplayName("criar - tarefa ja concluida na criacao recebe data de conclusao e ignera regra de duplicidade")
    void deveCriarTarefaConcluidaComDataDeConclusao() {
        TarefaRequestDTO dto = new TarefaRequestDTO("Implantar release v1", "Publicar em producao",
                StatusTarefa.CONCLUIDA);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        TarefaResponseDTO resposta = tarefaService.criar(dto);

        assertEquals(StatusTarefa.CONCLUIDA, resposta.getStatus());
        assertNotNull(resposta.getDataConclusao());
        verify(tarefaRepository, never()).existsByTituloIgnoreCaseAndStatusNot(anyString(), any(StatusTarefa.class));
    }

    // ---------------------------------------------------------------- CONSULTAR

    @Test
    @DisplayName("buscarPorId - caminho feliz: retorna a tarefa encontrada")
    void deveBuscarTarefaPorId() {
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));

        TarefaResponseDTO resposta = tarefaService.buscarPorId(1L);

        assertEquals(1L, resposta.getId());
        assertEquals("Implementar API de tarefas", resposta.getTitulo());
        assertEquals(StatusTarefa.PENDENTE, resposta.getStatus());
        verify(tarefaRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId - erro: lanca RecursoNaoEncontradoException para id inexistente")
    void deveLancarExcecaoQuandoIdNaoExistir() {
        when(tarefaRepository.findById(99L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException excecao = assertThrows(RecursoNaoEncontradoException.class,
                () -> tarefaService.buscarPorId(99L));

        assertTrue(excecao.getMessage().contains("99"));
    }

    @Test
    @DisplayName("listar - caminho feliz: retorna todas as tarefas convertidas em DTO")
    void deveListarTodasAsTarefas() {
        when(tarefaRepository.findAllByOrderByIdAsc()).thenReturn(List.of(tarefaPendente, tarefaConcluida));

        List<TarefaResponseDTO> lista = tarefaService.listar();

        assertEquals(2, lista.size());
        assertEquals("Implementar API de tarefas", lista.get(0).getTitulo());
        assertEquals(StatusTarefa.CONCLUIDA, lista.get(1).getStatus());
        assertNotNull(lista.get(1).getDataConclusao());
        verify(tarefaRepository, times(1)).findAllByOrderByIdAsc();
    }

    // ---------------------------------------------------------------- ATUALIZAR

    @Test
    @DisplayName("atualizar - caminho feliz: PENDENTE para EM_ANDAMENTO")
    void deveAtualizarStatusDePendenteParaEmAndamento() {
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        TarefaResponseDTO resposta = tarefaService.atualizar(1L,
                new TarefaAtualizacaoDTO(null, null, StatusTarefa.EM_ANDAMENTO));

        assertEquals(StatusTarefa.EM_ANDAMENTO, resposta.getStatus());
        assertNull(resposta.getDataConclusao());
        verify(tarefaRepository, times(1)).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("atualizar - caminho feliz: EM_ANDAMENTO para CONCLUIDA preenche dataConclusao")
    void deveConcluirTarefaEmAndamento() {
        tarefaPendente.setStatus(StatusTarefa.EM_ANDAMENTO);
        tarefaPendente.setDataConclusao(null);
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        TarefaResponseDTO resposta = tarefaService.atualizar(1L,
                new TarefaAtualizacaoDTO(null, null, StatusTarefa.CONCLUIDA));

        assertEquals(StatusTarefa.CONCLUIDA, resposta.getStatus());
        assertNotNull(resposta.getDataConclusao());
    }

    @Test
    @DisplayName("atualizar - regra de negocio: proibe transicao direta de PENDENTE para CONCLUIDA")
    void deveProibirTransicaoDiretaParaConcluida() {
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.atualizar(1L, new TarefaAtualizacaoDTO(null, null, StatusTarefa.CONCLUIDA)));

        assertTrue(excecao.getMessage().contains("PENDENTE para CONCLUIDA"));
        verify(tarefaRepository, never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("atualizar - regra de negocio: proibe reabrir uma tarefa CONCLUIDA")
    void deveProibirReabrirTarefaConcluida() {
        when(tarefaRepository.findById(2L)).thenReturn(Optional.of(tarefaConcluida));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.atualizar(2L, new TarefaAtualizacaoDTO(null, null, StatusTarefa.EM_ANDAMENTO)));

        assertTrue(excecao.getMessage().contains("CONCLUIDA"));
        verify(tarefaRepository, never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("atualizar - regra de negocio: proibe renomear tarefa ativa para titulo ja usado por outra ativa")
    void deveProibirTituloDuplicadoNaAtualizacao() {
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));
        when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNotAndIdNot("Outro titulo valido",
                StatusTarefa.CONCLUIDA, 1L)).thenReturn(true);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.atualizar(1L, new TarefaAtualizacaoDTO("Outro titulo valido", null, null)));

        assertTrue(excecao.getMessage().contains("Ja existe uma tarefa ativa"));
        verify(tarefaRepository, never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("atualizar - validacao: rejeita titulo invalido informado no payload")
    void deveRejeitarTituloInvalidoNaAtualizacao() {
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));

        assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.atualizar(1L, new TarefaAtualizacaoDTO("ab", null, null)));

        verify(tarefaRepository, never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("atualizar - erro: lanca RecursoNaoEncontradoException para id inexistente")
    void deveLancarExcecaoAoAtualizarIdInexistente() {
        when(tarefaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> tarefaService.atualizar(404L, new TarefaAtualizacaoDTO(null, null, StatusTarefa.EM_ANDAMENTO)));

        verify(tarefaRepository, never()).save(any(Tarefa.class));
    }

    // ---------------------------------------------------------------- DELETAR

    @Test
    @DisplayName("deletar - caminho feliz: remove a tarefa existente")
    void deveDeletarTarefaExistente() {
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefaPendente));

        tarefaService.deletar(1L);

        verify(tarefaRepository, times(1)).delete(tarefaPendente);
    }

    @Test
    @DisplayName("deletar - regra de negocio: proibe exclusao de tarefa CONCLUIDA")
    void deveProibirExclusaoDeTarefaConcluida() {
        when(tarefaRepository.findById(2L)).thenReturn(Optional.of(tarefaConcluida));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.deletar(2L));

        assertTrue(excecao.getMessage().contains("CONCLUIDA"));
        verify(tarefaRepository, never()).delete(any(Tarefa.class));
    }

    @Test
    @DisplayName("deletar - erro: lanca RecursoNaoEncontradoException para id inexistente")
    void deveLancarExcecaoAoDeletarIdInexistente() {
        when(tarefaRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> tarefaService.deletar(50L));

        verify(tarefaRepository, never()).delete(any(Tarefa.class));
    }
}
