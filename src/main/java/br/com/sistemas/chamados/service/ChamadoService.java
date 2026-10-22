package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import org.springframework.data.domain.Pageable;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Prioridade;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaService categoriaService;
    private final TecnicoService tecnicoService;

    public ChamadoService(ChamadoRepository repository,
                          ClienteRepository clienteRepository,
                          CategoriaService categoriaService,
                          TecnicoService tecnicoService) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaService = categoriaService;
        this.tecnicoService = tecnicoService;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Chamado chamado = new Chamado();
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(categoriaService.buscarEntity(dto.categoriaId()));
        if (dto.tecnicoId() != null) {
            chamado.setTecnico(tecnicoService.buscarEntity(dto.tecnicoId()));
        }
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
public PaginaResponse<ChamadoResponse> listar(
        StatusChamado status, Prioridade prioridade, Long clienteId, Pageable pageable) {
    return PaginaResponse.de(
        repository.filtrar(status, prioridade, clienteId, pageable).map(ChamadoResponse::de));
}

@Transactional(readOnly = true)
public PaginaResponse<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
    if (!clienteRepository.existsById(clienteId)) {
        throw new RecursoNaoEncontradoException("Cliente " + clienteId + " não encontrado");
    }
    return PaginaResponse.de(
        repository.findByClienteId(clienteId, pageable).map(ChamadoResponse::de));
}

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);
        garantirNaoFechado(chamado);
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(categoriaService.buscarEntity(dto.categoriaId()));
        chamado.setTecnico(dto.tecnicoId() != null ? tecnicoService.buscarEntity(dto.tecnicoId()) : null);
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarEntity(id));
    }

    public Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + id + " não encontrado"));
    }

    @Transactional
    public ChamadoResponse atribuir(Long id, AtribuirTecnicoRequest dto) {
    Chamado chamado = buscarEntity(id);
    garantirNaoFechado(chamado);
    chamado.setTecnico(tecnicoService.buscarEntity(dto.tecnicoId()));
    return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, AlterarStatusRequest dto) {
    Chamado chamado = buscarEntity(id);
    garantirNaoFechado(chamado);
    StatusChamado destino = dto.status();
    if (!chamado.getStatus().podeIrPara(destino)) {
        throw new RegraNegocioException(
            "Transição de status inválida: " + chamado.getStatus() + " para " + destino);
    }
    chamado.setStatus(destino);
    if (destino == StatusChamado.FECHADO) {
        chamado.setDataFechamento(LocalDateTime.now());
    }
    return ChamadoResponse.de(chamado);
    }

    private void garantirNaoFechado(Chamado chamado) {
    if (chamado.getStatus() == StatusChamado.FECHADO) {
        throw new ChamadoFechadoException(
            "Chamado " + chamado.getId() + " está fechado e não pode ser alterado");
    }
    }
}