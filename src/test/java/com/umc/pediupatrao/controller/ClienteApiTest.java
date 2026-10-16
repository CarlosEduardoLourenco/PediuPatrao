package com.umc.pediupatrao.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.ExpurgoDeClientes;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteApiTest {

    private static final String CORPO_COM_TERMO = """
            {"nome": "Maria", "telefone": "(11) 99999-1234", "logradouro": "Rua A", "termoAceito": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ExpurgoDeClientes expurgoDeClientes;

    @BeforeEach
    void limpar() {
        mongoTemplate.remove(new Query(), RegistroAuditoria.class);
    }

    @AfterEach
    void limparDados() {
        mongoTemplate.remove(new Query(), RegistroAuditoria.class);
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
    }

    @Test
    void deveCadastrarClienteComTermoAceitoRegistrandoDataEAuditoria() throws Exception {
        String resposta = executar("POST", "/api/clientes", "atendente01", "ATENDENTE", CORPO_COM_TERMO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.termoAceitoEm").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(resposta, "$.id");

        executar("GET", "/api/auditoria?entidade=CLIENTE&entidadeId=" + id, "gerente01", "GERENTE", null)
                .andExpect(jsonPath("$.content[0].operacao").value("CRIACAO_CLIENTE"))
                .andExpect(jsonPath("$.content[0].alteracoes[?(@.campo == 'termoAceitoEm')]").isNotEmpty());
    }

    @Test
    void deveRecusarCadastroSemAceiteDoTermo() throws Exception {
        executar("POST", "/api/clientes", "atendente01", "ATENDENTE",
                "{\"nome\": \"Maria\", \"telefone\": \"(11) 99999-1234\"}")
                .andExpect(status().isBadRequest());
        executar("POST", "/api/clientes", "atendente01", "ATENDENTE",
                "{\"nome\": \"Maria\", \"telefone\": \"(11) 99999-1234\", \"termoAceito\": false}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveInativarComoGerenteSemApagarOsDadosEAuditar() throws Exception {
        String id = cadastrar();

        executar("POST", "/api/clientes/" + id + "/inativar", "gerente01", "GERENTE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.retidoAte").isNotEmpty());

        Cliente salvo = clienteRepository.findById(id).orElseThrow();
        assertEquals(salvo.getInativadoEm().plusYears(1), salvo.getRetidoAte());
        executar("GET", "/api/auditoria?entidade=CLIENTE&entidadeId=" + id, "admin", "ADMIN", null)
                .andExpect(jsonPath("$.content[?(@.operacao == 'INATIVACAO_CLIENTE')].usuario").value("gerente01"));
    }

    @Test
    void deveNegarInativacaoParaAtendenteEAdmin() throws Exception {
        String id = cadastrar();

        executar("POST", "/api/clientes/" + id + "/inativar", "atendente01", "ATENDENTE", null)
                .andExpect(status().isForbidden());
        executar("POST", "/api/clientes/" + id + "/inativar", "admin", "ADMIN", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void naoDevePermitirExclusaoDefinitivaDeClientePelaApi() throws Exception {
        String id = cadastrar();

        executar("DELETE", "/api/clientes/" + id, "atendente01", "ATENDENTE", null)
                .andExpect(status().isMethodNotAllowed());
        executar("DELETE", "/api/clientes/" + id, "gerente01", "GERENTE", null)
                .andExpect(status().isForbidden());
        assertTrue(clienteRepository.findById(id).isPresent());
    }

    @Test
    void naoDeveAlterarNemReinativarClienteInativo() throws Exception {
        String id = cadastrar();
        executar("POST", "/api/clientes/" + id + "/inativar", "gerente01", "GERENTE", null).andExpect(status().isOk());

        executar("PUT", "/api/clientes/" + id, "atendente01", "ATENDENTE", CORPO_COM_TERMO)
                .andExpect(status().isConflict());
        executar("POST", "/api/clientes/" + id + "/inativar", "gerente01", "GERENTE", null)
                .andExpect(status().isConflict());
    }

    @Test
    void naoDeveAceitarNovoPedidoParaClienteInativo() throws Exception {
        String id = cadastrar();
        executar("POST", "/api/clientes/" + id + "/inativar", "gerente01", "GERENTE", null).andExpect(status().isOk());
        Produto pizza = new Produto();
        pizza.setNome("Pizza");
        pizza.setTipo("Pizza");
        pizza.setPreco(40.0);
        pizza.setAtivo(true);
        pizza = produtoRepository.save(pizza);

        executar("POST", "/api/pedidos", "atendente01", "ATENDENTE", """
                {"clienteId": "%s", "tipoSaida": "ENTREGA", "itens": [{"produtoId": "%s", "quantidade": 1}]}
                """.formatted(id, pizza.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Cliente inativo não pode fazer novos pedidos"));
    }

    @Test
    void deveExpurgarSomenteClientesInativosComPrazoVencidoRegistrandoAuditoriaDoSistema() throws Exception {
        Cliente vencido = clienteInativo("Vencido", LocalDateTime.now().minusDays(1));
        Cliente naoVencido = clienteInativo("Retido", LocalDateTime.now().plusDays(30));
        Cliente ativo = clienteRepository.save(clienteSimples("Ativo"));

        int total = expurgoDeClientes.expurgarVencidos();

        assertEquals(1, total);
        assertTrue(clienteRepository.findById(vencido.getId()).isEmpty());
        assertTrue(clienteRepository.findById(naoVencido.getId()).isPresent());
        assertTrue(clienteRepository.findById(ativo.getId()).isPresent());
        List<RegistroAuditoria> registros = mongoTemplate.find(
                new Query(Criteria.where("operacao").is(TipoOperacao.EXPURGO_CLIENTE)), RegistroAuditoria.class);
        assertEquals(1, registros.size());
        assertEquals("sistema", registros.get(0).usuario());
        assertEquals(vencido.getId(), registros.get(0).entidadeId());
    }

    private String cadastrar() throws Exception {
        String resposta = executar("POST", "/api/clientes", "atendente01", "ATENDENTE", CORPO_COM_TERMO)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private Cliente clienteInativo(String nome, LocalDateTime retidoAte) {
        Cliente cliente = clienteSimples(nome);
        cliente.setAtivo(false);
        cliente.setInativadoEm(retidoAte.minusYears(1));
        cliente.setRetidoAte(retidoAte);
        return clienteRepository.save(cliente);
    }

    private Cliente clienteSimples(String nome) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTelefone("(11) 99999-0000");
        return cliente;
    }

    private ResultActions executar(String metodo, String caminho, String usuario, String perfil, String corpo)
            throws Exception {
        MockHttpServletRequestBuilder requisicao = MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo), caminho)
                .with(user(usuario).roles(perfil))
                .with(csrf());
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(requisicao);
    }
}
