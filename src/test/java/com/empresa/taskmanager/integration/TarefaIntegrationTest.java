package com.empresa.taskmanager.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Testes de integracao da API completa (Controller + Service + Repository + H2)")
class TarefaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST + GET - cria uma tarefa e a recupera com os dados persistidos no H2")
    void deveCriarEBuscarTarefa() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Tarefa criada via teste de integracao",
                                  "descricao": "Validada de ponta a ponta"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(resultado.getResponse().getContentAsString());
        long id = body.get("id").asLong();
        assertNotNull(body.get("dataCriacao").asText());

        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Tarefa criada via teste de integracao"))
                .andExpect(jsonPath("$.descricao").value("Validada de ponta a ponta"));
    }

    @Test
    @DisplayName("GET /api/tarefas - retorna a carga inicial do data.sql")
    void deveListarTarefasCarregadasDoDataSql() throws Exception {
        mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[*].status", org.hamcrest.Matchers.hasItems("PENDENTE", "CONCLUIDA")));
    }

    @Test
    @DisplayName("POST - titulo duplicado de tarefa ativa retorna 400 Bad Request")
    void deveRejeitarTituloDuplicado() throws Exception {
        String payload = """
                {
                  "titulo": "Tarefa duplicada no teste",
                  "descricao": "Primeira"
                }
                """;

        mockMvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Ja existe uma tarefa ativa")));
    }

    @Test
    @DisplayName("PUT - PENDENTE -> EM_ANDAMENTO -> CONCLUIDA, preenchendo dataConclusao")
    void deveConcluirTarefaPassandoPorEmAndamento() throws Exception {
        MvcResult criada = mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Tarefa que sera concluida no teste"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONCLUIDA"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "EM_ANDAMENTO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));

        mockMvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONCLUIDA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").exists());
    }

    @Test
    @DisplayName("DELETE - tarefa pendente e removida (204) e depois retorna 404")
    void deveRemoverTarefaPendente() throws Exception {
        MvcResult criada = mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Tarefa que sera removida"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/tarefas/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE - tarefa CONCLUIDA do data.sql retorna 400 Bad Request")
    void deveRejeitarExclusaoDeTarefaConcluida() throws Exception {
        MvcResult lista = mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode concluida = null;
        for (JsonNode tarefa : objectMapper.readTree(lista.getResponse().getContentAsString())) {
            if ("CONCLUIDA".equals(tarefa.get("status").asText())) {
                concluida = tarefa;
                break;
            }
        }
        assertNotNull(concluida, "O data.sql deve conter ao menos uma tarefa CONCLUIDA");

        long id = concluida.get("id").asLong();
        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mockMvc.perform(delete("/api/tarefas/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET - id inexistente retorna 404 Not Found")
    void deveRetornar404ParaIdInexistente() throws Exception {
        mockMvc.perform(get("/api/tarefas/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Not Found"));
    }

    @Test
    @DisplayName("POST - titulo com menos de 5 caracteres retorna 400 com detalhes da validacao")
    void deveRetornar400ParaTituloCurto() throws Exception {
        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "abc",
                                  "descricao": "titulo invalido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros[0]").value(org.hamcrest.Matchers.containsString("titulo")));

    }
}
