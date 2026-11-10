package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginaInicialTelaTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExibirOPainelDeVendasAoGerente() throws Exception {
        inicioComo("GERENTE")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"painelDeVendas\"")))
                .andExpect(content().string(containsString("/plugins/chart.js/Chart.min.js")))
                .andExpect(content().string(containsString("/js/painel.js")))
                .andExpect(content().string(containsString("id=\"filtrosDoPainel\"")))
                .andExpect(content().string(not(containsString("Bem-vindo!!!"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveManterABoasVindasParaOsDemaisPerfis(String perfil) throws Exception {
        inicioComo(perfil)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bem-vindo!!!")))
                .andExpect(content().string(not(containsString("painelDeVendas"))))
                .andExpect(content().string(not(containsString("/js/painel.js"))));
    }

    @Test
    void deveOferecerOsBlocosExigidosPeloPainel() throws Exception {
        inicioComo("GERENTE")
                .andExpect(content().string(containsString("id=\"blocoIndicadores\"")))
                .andExpect(content().string(containsString("id=\"blocoSerie\"")))
                .andExpect(content().string(containsString("id=\"blocoStatus\"")))
                .andExpect(content().string(containsString("id=\"blocoEmAndamento\"")))
                .andExpect(content().string(containsString("id=\"blocoRanking\"")))
                .andExpect(content().string(containsString("id=\"blocoDistribuicao\"")))
                .andExpect(content().string(containsString("id=\"blocoCancelamentos\"")))
                .andExpect(content().string(containsString("id=\"atualizacaoAutomatica\"")));
    }

    @Test
    void deveServirOsArquivosDoPainelSemExigirLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/js/painel.js")).andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/css/painel.css")).andExpect(status().isOk());
    }

    private ResultActions inicioComo(String perfil) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/").with(user("teste").roles(perfil)));
    }
}
