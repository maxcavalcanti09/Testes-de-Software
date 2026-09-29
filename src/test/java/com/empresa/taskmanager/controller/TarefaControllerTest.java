package com.empresa.taskmanager.controller;

import com.empresa.taskmanager.dto.TarefaResponseDTO;
import com.empresa.taskmanager.entity.StatusTarefa;
import com.empresa.taskmanager.exception.RecursoNaoEncontradoException;
import com.empresa.taskmanager.exception.RegraDeNegocioException;
import com.empresa.taskmanager.service.TarefaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TarefaController.class)
@DisplayName("Testes da camada Web - TarefaController (MockMvc com service mockado)")
class TarefaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TarefaService tarefaService;

    private TarefaResponseDTO resposta(Long id, String titulo, StatusTarefa status, LocalDateTime conclusao) {
        return new TarefaResponseDTO(id, titulo, "Descricao da tarefa", status, LocalDateTime.now(), conclusao);
    }

    @Test
    @DisplayName("POST /api/tarefas - cria tarefa e responde 201 Created com Location e payload")
    void deveRetornar201AoCriarTarefa() throws Exception {
        when(tarefaService.criar(any())).thenReturn(resposta(1L, "Implementar API de tarefas",
                StatusTarefa.PENDENTE, null));

        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Implementar API de tarefas",
                                  "descricao": "Descricao da tarefa"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tarefas/1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Implementar API de tarefas"))
                .andExpect(jsonPath("$.descricao").value("Descricao da tarefa"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataCriacao").exists())
                .andExpect(jsonPath("$.dataConclusao").doesNotExist());

        verify(tarefaService, times(1)).criar(any());
    }

    @Test
    @DisplayName("POST /api/tarefas - payload invalido responde 400 Bad Request com lista de erros")
    void deveRetornar400AoCriarComPayloadInvalido() throws Exception {
        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "abc",
                                  "descricao": "Descricao valida"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Payload invalido"))
                .andExpect(jsonPath("$.erros").isArray())
                .andExpect(jsonPath("$.erros[0]").value(org.hamcrest.Matchers.containsString("titulo")));

        verify(tarefaService, never()).criar(any());
    }

    @Test
    @DisplayName("GET /api/tarefas - lista as tarefas e responde 200 OK")
    void deveListarTarefas() throws Exception {
        when(tarefaService.listar()).thenReturn(List.of(
                resposta(1L, "Implementar API de tarefas", StatusTarefa.PENDENTE, null),
                resposta(2L, "Revisar documentacao", StatusTarefa.CONCLUIDA, LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].status").value("CONCLUIDA"));

        verify(tarefaService, times(1)).listar();
    }

    @Test
    @DisplayName("GET /api/tarefas/{id} - tarefa existente responde 200 OK com o JSON completo")
    void deveBuscarTarefaPorId() throws Exception {
        when(tarefaService.buscarPorId(1L)).thenReturn(resposta(1L, "Implementar API de tarefas",
                StatusTarefa.EM_ANDAMENTO, null));

        mockMvc.perform(get("/api/tarefas/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Implementar API de tarefas"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("GET /api/tarefas/{id} - id inexistente responde 404 Not Found")
    void deveRetornar404QuandoTarefaNaoEncontrada() throws Exception {
        when(tarefaService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Tarefa nao encontrada com o id: 99"));

        mockMvc.perform(get("/api/tarefas/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Tarefa nao encontrada com o id: 99"));
    }

    @Test
    @DisplayName("PUT /api/tarefas/{id} - atualiza status e responde 200 OK")
    void deveAtualizarTarefa() throws Exception {
        when(tarefaService.atualizar(eq(1L), any()))
                .thenReturn(resposta(1L, "Implementar API de tarefas", StatusTarefa.CONCLUIDA, LocalDateTime.now()));

        mockMvc.perform(put("/api/tarefas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONCLUIDA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").exists());
    }

    @Test
    @DisplayName("PUT /api/tarefas/{id} - transicao PENDENTE -> CONCLUIDA responde 400 Bad Request")
    void deveRetornar400AoConcluirTarefaPendente() throws Exception {
        when(tarefaService.atualizar(eq(1L), any())).thenThrow(new RegraDeNegocioException(
                "Nao e permitido transicionar a tarefa (id 1) de PENDENTE para CONCLUIDA."));

        mockMvc.perform(put("/api/tarefas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONCLUIDA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Bad Request"));
    }

    @Test
    @DisplayName("DELETE /api/tarefas/{id} - remove a tarefa e responde 204 No Content")
    void deveRetornar204AoDeletarTarefa() throws Exception {
        mockMvc.perform(delete("/api/tarefas/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tarefaService, times(1)).deletar(1L);
    }

    @Test
    @DisplayName("DELETE /api/tarefas/{id} - tarefa CONCLUIDA responde 400 Bad Request")
    void deveRetornar400AoDeletarTarefaConcluida() throws Exception {
        doThrow(new RegraDeNegocioException(
                "Nao e permitido excluir a tarefa (id 1) com status CONCLUIDA"))
                .when(tarefaService).deletar(anyLong());

        mockMvc.perform(delete("/api/tarefas/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("CONCLUIDA")));
    }

    @Test
    @DisplayName("DELETE /api/tarefas/{id} - id inexistente responde 404 Not Found")
    void deveRetornar404AoDeletarTarefaInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Tarefa nao encontrada com o id: 77"))
                .when(tarefaService).deletar(anyLong());

        mockMvc.perform(delete("/api/tarefas/{id}", 77L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
