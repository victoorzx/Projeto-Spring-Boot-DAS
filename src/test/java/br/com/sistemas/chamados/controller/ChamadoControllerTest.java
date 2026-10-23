package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService service;

    private ChamadoResponse chamadoAberto() {
        return new ChamadoResponse(1L, "Notebook não liga", null, LocalDateTime.now(), null,
            Prioridade.ALTA, StatusChamado.ABERTO, 1L, "Ana Souza", 1L, "Hardware", null, null);
    }

    @Test
    void criarChamadoValidoRetorna201ComStatusAberto() throws Exception {
        when(service.criar(any())).thenReturn(chamadoAberto());

        String json = """
            {"titulo":"Notebook não liga","prioridade":"ALTA","clienteId":1,"categoriaId":1}
            """;

        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTO"))
            .andExpect(jsonPath("$.clienteNome").value("Ana Souza"));
    }

    @Test
    void criarChamadoSemCamposObrigatoriosRetorna400() throws Exception {
        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void buscarChamadoInexistenteRetorna404() throws Exception {
        when(service.buscarDetalhe(99999L))
            .thenThrow(new RecursoNaoEncontradoException("Chamado 99999 não encontrado"));

        mockMvc.perform(get("/chamados/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Chamado 99999 não encontrado"));
    }

    @Test
    void alterarStatusDeChamadoFechadoRetorna409() throws Exception {
        when(service.alterarStatus(eq(1L), any()))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está fechado e não pode ser alterado"));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Chamado 1 está fechado e não pode ser alterado"));
    }

    @Test
    void transicaoDeStatusInvalidaRetorna409() throws Exception {
        when(service.alterarStatus(eq(1L), any()))
            .thenThrow(new RegraNegocioException("Transição de status inválida: EM_ATENDIMENTO para ABERTO"));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ABERTO\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void atribuirTecnicoEmChamadoFechadoRetorna409() throws Exception {
        when(service.atribuir(eq(1L), any()))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está fechado e não pode ser alterado"));

        mockMvc.perform(patch("/chamados/1/atribuir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":1}"))
            .andExpect(status().isConflict());
    }

    @Test
    void alterarStatusComValorInexistenteRetorna400() throws Exception {
        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"XYZ\"}"))
            .andExpect(status().isBadRequest());
    }
}