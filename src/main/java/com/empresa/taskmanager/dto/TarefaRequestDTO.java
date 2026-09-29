package com.empresa.taskmanager.dto;

import com.empresa.taskmanager.entity.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TarefaRequestDTO {

    @NotBlank(message = "O titulo e obrigatorio")
    @Size(min = 5, max = 100, message = "O titulo deve ter entre 5 e 100 caracteres")
    private String titulo;

    @Size(max = 255, message = "A descricao deve ter no maximo 255 caracteres")
    private String descricao;

    private StatusTarefa status;

    public TarefaRequestDTO() {
    }

    public TarefaRequestDTO(String titulo, String descricao, StatusTarefa status) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.status = status;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public void setStatus(StatusTarefa status) {
        this.status = status;
    }
}
