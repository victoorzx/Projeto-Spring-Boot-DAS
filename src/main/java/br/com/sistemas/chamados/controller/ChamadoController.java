package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(@Valid @RequestBody ChamadoRequest dto) {
        ChamadoResponse criado = service.criar(dto);
        return ResponseEntity.created(URI.create("/chamados/" + criado.id())).body(criado);
    }

    @GetMapping
public PaginaResponse<ChamadoResponse> listar(
        @RequestParam(required = false) StatusChamado status,
        @RequestParam(required = false) Prioridade prioridade,
        @RequestParam(required = false) Long clienteId,
        @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
    return service.listar(status, prioridade, clienteId, pageable);
}

    @GetMapping("/{id}")
    public ChamadoDetalheResponse buscar(@PathVariable Long id) {
    return service.buscarDetalhe(id);
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ChamadoRequest dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/atribuir")
    public ChamadoResponse atribuir(@PathVariable Long id, @Valid @RequestBody AtribuirTecnicoRequest dto) {
        return service.atribuir(id, dto);
    }

    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusRequest dto) {
        return service.alterarStatus(id, dto);
    }
}