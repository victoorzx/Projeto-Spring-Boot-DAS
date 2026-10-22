package br.com.sistemas.chamados.dto;

import java.util.List;

public record ChamadoDetalheResponse(
    ChamadoResponse chamado,
    List<ComentarioResponse> comentarios
) { }