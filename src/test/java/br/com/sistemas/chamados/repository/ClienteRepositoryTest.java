package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Cliente;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Test
    void salvaClienteEGeraId() {
        Cliente salvo = repository.save(new Cliente("Ana Souza", "ana@exemplo.com", "47999999999"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId())).isPresent();
    }

    @Test
    void existsByEmailRetornaTrueQuandoEmailExiste() {
        repository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));

        assertThat(repository.existsByEmail("ana@exemplo.com")).isTrue();
    }

    @Test
    void existsByEmailRetornaFalseQuandoEmailNaoExiste() {
        assertThat(repository.existsByEmail("nao@existe.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotIgnoraOProprioRegistro() {
        Cliente ana = repository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));

        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId() + 1)).isTrue();
    }

    @Test
    void emailDuplicadoViolaRestricaoDeUnicidade() {
        repository.saveAndFlush(new Cliente("Ana Souza", "ana@exemplo.com", null));

        assertThatThrownBy(() ->
            repository.saveAndFlush(new Cliente("Outro Cliente", "ana@exemplo.com", null)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}