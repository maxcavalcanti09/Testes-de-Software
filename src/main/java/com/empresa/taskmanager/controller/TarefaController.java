package com.empresa.taskmanager.controller;

import com.empresa.taskmanager.dto.TarefaAtualizacaoDTO;
import com.empresa.taskmanager.dto.TarefaRequestDTO;
import com.empresa.taskmanager.dto.TarefaResponseDTO;
import com.empresa.taskmanager.service.TarefaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(@Valid @RequestBody TarefaRequestDTO dto,
                                                   UriComponentsBuilder uriBuilder) {
        TarefaResponseDTO criada = tarefaService.criar(dto);
        URI location = uriBuilder.path("/api/tarefas/{id}").buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listar() {
        return ResponseEntity.ok(tarefaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tarefaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody TarefaAtualizacaoDTO dto) {
        return ResponseEntity.ok(tarefaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        tarefaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
