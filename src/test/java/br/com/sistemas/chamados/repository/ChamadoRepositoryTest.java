package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired
    private ChamadoRepository repository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private final Pageable pagina = PageRequest.of(0, 10);

    private Cliente ana;
    private Cliente bruno;

    @BeforeEach
    void preparaDados() {
        ana = clienteRepository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));
        bruno = clienteRepository.save(new Cliente("Bruno Lima", "bruno@exemplo.com", null));
        Categoria hardware = categoriaRepository.save(new Categoria("Hardware", "Equipamentos"));

        novoChamado("A", Prioridade.ALTA, StatusChamado.ABERTO, ana, hardware);
        novoChamado("B", Prioridade.BAIXA, StatusChamado.EM_ATENDIMENTO, ana, hardware);
        novoChamado("C", Prioridade.ALTA, StatusChamado.ABERTO, bruno, hardware);
    }

    private void novoChamado(String titulo, Prioridade prioridade, StatusChamado status,
                             Cliente cliente, Categoria categoria) {
        Chamado chamado = new Chamado();
        chamado.setTitulo(titulo);
        chamado.setPrioridade(prioridade);
        chamado.setStatus(status);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setCliente(cliente);
        chamado.setCategoria(categoria);
        repository.save(chamado);
    }

    @Test
    void filtrarSemFiltrosRetornaTodos() {
        Page<Chamado> resultado = repository.filtrar(null, null, null, pagina);

        assertThat(resultado.getTotalElements()).isEqualTo(3);
    }

    @Test
    void filtrarPorStatus() {
        Page<Chamado> resultado = repository.filtrar(StatusChamado.ABERTO, null, null, pagina);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filtrarPorPrioridade() {
        Page<Chamado> resultado = repository.filtrar(null, Prioridade.ALTA, null, pagina);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filtrarCombinandoStatusPrioridadeECliente() {
        Page<Chamado> resultado =
            repository.filtrar(StatusChamado.ABERTO, Prioridade.ALTA, ana.getId(), pagina);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getTitulo()).isEqualTo("A");
    }

    @Test
    void filtrarSemResultadoRetornaPaginaVazia() {
        Page<Chamado> resultado =
            repository.filtrar(StatusChamado.FECHADO, null, null, pagina);

        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    void findByClienteIdRetornaApenasChamadosDoCliente() {
        assertThat(repository.findByClienteId(ana.getId(), pagina).getTotalElements()).isEqualTo(2);
        assertThat(repository.findByClienteId(bruno.getId(), pagina).getTotalElements()).isEqualTo(1);
    }

    @Test
    void paginacaoDividePeloTamanhoDaPagina() {
        Page<Chamado> resultado = repository.filtrar(null, null, null, PageRequest.of(0, 2));

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
    }
}