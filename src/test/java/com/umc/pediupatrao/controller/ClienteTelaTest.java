package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteTelaTest {

    private static final String TOAST = "Termos de uso encaminhados ao cliente.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @AfterEach
    void limparDados() {
        mongoTemplate.remove(new Query(), RegistroAuditoria.class);
        clienteRepository.deleteAll();
    }

    @Test
    void deveAvisarQueOsTermosForamEncaminhadosAoCriarCliente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/clientes/salvar")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .param("nome", "Maria Souza")
                        .param("telefone", "(11) 99999-1234")
                        .param("termoAceito", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clientes"))
                .andExpect(flash().attribute("toast", TOAST))
                .andExpect(flash().attribute("sucesso", "Cliente criado com sucesso!"));

        assertEquals(1, clienteRepository.count());
    }

    @Test
    void naoDeveAvisarQuandoOCadastroForRecusadoPorFaltaDoTermo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/clientes/salvar")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .param("nome", "Maria Souza")
                        .param("telefone", "(11) 99999-1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"))
                .andExpect(flash().attributeCount(1));

        assertEquals(0, clienteRepository.count());
    }

    @Test
    void deveExibirOToastNaListaDeClientes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/clientes")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .flashAttr("toast", TOAST))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("toastTermos")))
                .andExpect(content().string(containsString(TOAST)));
    }

    @Test
    void naoDeveExibirOToastSemOAvisoDeEncaminhamento() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/clientes").with(user("atendente01").roles("ATENDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"toastTermos\""))));
    }
}
