package com.umc.pediupatrao.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EntregadorRepository entregadorRepository;

    private Cliente cliente;
    private Produto pizza;
    private Entregador entregador;

    @BeforeEach
    void prepararDados() {
        cliente = new Cliente();
        cliente.setNome("Cliente de Teste");
        cliente = clienteRepository.save(cliente);

        pizza = new Produto();
        pizza.setNome("Pizza de Teste");
        pizza.setTipo("Pizza");
        pizza.setPreco(40.0);
        pizza.setAtivo(true);
        pizza = produtoRepository.save(pizza);

        entregador = new Entregador();
        entregador.setNome("Carlos");
        entregador.setTelefone("(11) 98888-0000");
        entregador.setAtivo(true);
        entregador = entregadorRepository.save(entregador);
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @AfterEach
    void limparDados() {
        mongoTemplate.remove(new Query(), RegistroAuditoria.class);
        pedidoRepository.deleteAll();
        clienteRepository.delete(cliente);
        produtoRepository.delete(pizza);
        entregadorRepository.deleteAll();
    }

    @Test
    void deveIgnorarTotalStatusEResponsavelEnviadosPeloCliente() throws Exception {
        String corpo = """
                {
                  "clienteId": "%s",
                  "tipoSaida": "ENTREGA",
                  "itens": [{"produtoId": "%s", "quantidade": 2}],
                  "valorTotal": 0.01,
                  "valorDesconto": 99,
                  "descontoPercentual": 90,
                  "status": "FINALIZADO",
                  "usuarioEntrada": "invasor"
                }
                """.formatted(cliente.getId(), pizza.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.subtotal").value(80.0))
                .andExpect(jsonPath("$.valorTotal").value(80.0))
                .andExpect(jsonPath("$.valorDesconto").value(0.0))
                .andExpect(jsonPath("$.descontoPercentual").value(0.0))
                .andExpect(jsonPath("$.usuarioEntrada").value("atendente01"))
                .andExpect(jsonPath("$.clienteNome").value("Cliente de Teste"))
                .andExpect(jsonPath("$.itens[0].nomeProduto").value("Pizza de Teste"))
                .andExpect(jsonPath("$.itens[0].subtotal").value(80.0));
    }

    @Test
    void deveUsarOPrecoDoCadastroEIgnorarPrecoEnviadoNoItem() throws Exception {
        String corpo = """
                {
                  "clienteId": "%s",
                  "tipoSaida": "RETIRADA",
                  "itens": [{"produtoId": "%s", "quantidade": 1, "precoUnitario": 0.01, "subtotal": 0.01}]
                }
                """.formatted(cliente.getId(), pizza.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos")
                        .with(user("gerente01").roles("GERENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(40.0))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(40.0));
    }

    @Test
    void deveResponderComErroDeNegocioParaPedidoSemItens() throws Exception {
        String corpo = """
                {"clienteId": "%s", "tipoSaida": "ENTREGA", "itens": []}
                """.formatted(cliente.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O pedido precisa de ao menos um item"));
    }

    @Test
    void deveConduzirPedidoDeEntregaPeloFluxoCompleto() throws Exception {
        String id = criarPedido("ENTREGA");

        avancar(id, "atendente01", "ATENDENTE", "{}").andExpect(jsonPath("$.status").value("EM_PREPARACAO"));
        avancar(id, "atendente01", "ATENDENTE", "").andExpect(jsonPath("$.status").value("PRONTO"));
        avancar(id, "gerente01", "GERENTE", "{\"entregadorId\": \"" + entregador.getId() + "\"}")
                .andExpect(jsonPath("$.status").value("SAIU_PARA_ENTREGA"))
                .andExpect(jsonPath("$.saida.entregadorNome").value("Carlos"))
                .andExpect(jsonPath("$.saida.usuario").value("gerente01"))
                .andExpect(jsonPath("$.saida.dataHora").isNotEmpty());
        avancar(id, "atendente01", "ATENDENTE", "{}").andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    void deveConduzirPedidoDeRetiradaPeloFluxoCompleto() throws Exception {
        String id = criarPedido("RETIRADA");

        avancar(id, "atendente01", "ATENDENTE", "{}");
        avancar(id, "atendente01", "ATENDENTE", "{}");
        avancar(id, "atendente01", "ATENDENTE", "{}")
                .andExpect(jsonPath("$.status").value("RETIRADO"))
                .andExpect(jsonPath("$.saida.usuario").value("atendente01"));
        avancar(id, "atendente01", "ATENDENTE", "{}").andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    void deveRejeitarPuloDeEtapaComConflito() throws Exception {
        String id = criarPedido("ENTREGA");
        avancar(id, "atendente01", "ATENDENTE", "{}");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos/" + id + "/avancar")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destino\": \"FINALIZADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Transição inválida: de EM_PREPARACAO só é possível ir para PRONTO, não para FINALIZADO"));
    }

    @Test
    void deveRejeitarAvancoDePedidoFinalizado() throws Exception {
        String id = criarPedido("RETIRADA");
        for (int passo = 0; passo < 4; passo++) {
            avancar(id, "atendente01", "ATENDENTE", "{}");
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos/" + id + "/avancar")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void deveNegarAvancoAoAdmin() throws Exception {
        String id = criarPedido("ENTREGA");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos/" + id + "/avancar")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveCancelarPedidoComoGerenteMantendoOsDados() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "cancelar", "gerente01", "GERENTE",
                "{\"motivo\": \"PAGAMENTO_RECUSADO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.cancelamento.usuario").value("gerente01"))
                .andExpect(jsonPath("$.cancelamento.motivo").value("PAGAMENTO_RECUSADO"))
                .andExpect(jsonPath("$.itens[0].nomeProduto").value("Pizza de Teste"))
                .andExpect(jsonPath("$.valorTotal").value(40.0));
    }

    @Test
    void deveNegarCancelamentoParaAtendenteEAdmin() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "cancelar", "atendente01", "ATENDENTE", "{\"motivo\": \"CLIENTE_DESISTIU\"}")
                .andExpect(status().isForbidden());
        postar(id, "cancelar", "admin", "ADMIN", "{\"motivo\": \"CLIENTE_DESISTIU\"}")
                .andExpect(status().isForbidden());
        postar(id, "avancar", "atendente01", "ATENDENTE", "{}").andExpect(status().isOk());
    }

    @Test
    void deveRejeitarMotivoForaDaListaPredefinida() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "cancelar", "gerente01", "GERENTE", "{\"motivo\": \"porque sim\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExigirObservacaoParaMotivoOutro() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "cancelar", "gerente01", "GERENTE", "{\"motivo\": \"OUTRO\", \"observacao\": \"x\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoDeveCancelarPedidoQueJaSaiu() throws Exception {
        String id = criarPedido("RETIRADA");
        for (int passo = 0; passo < 3; passo++) {
            postar(id, "avancar", "atendente01", "ATENDENTE", "{}").andExpect(status().isOk());
        }

        postar(id, "cancelar", "gerente01", "GERENTE", "{\"motivo\": \"CLIENTE_DESISTIU\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void naoDeveAlterarPedidoDepoisDeCancelado() throws Exception {
        String id = criarPedido("ENTREGA");
        postar(id, "cancelar", "gerente01", "GERENTE", "{\"motivo\": \"CLIENTE_DESISTIU\"}")
                .andExpect(status().isOk());

        postar(id, "avancar", "atendente01", "ATENDENTE", "{}").andExpect(status().isConflict());
        postar(id, "desconto", "gerente01", "GERENTE", "{\"percentual\": 10}").andExpect(status().isConflict());
        postar(id, "cancelar", "gerente01", "GERENTE", "{\"motivo\": \"ERRO_NO_PEDIDO\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void deveAplicarDescontoDeQuinzePorCentoComoGerente() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "desconto", "gerente01", "GERENTE", "{\"percentual\": 15}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoPercentual").value(15.0))
                .andExpect(jsonPath("$.valorDesconto").value(6.0))
                .andExpect(jsonPath("$.valorTotal").value(34.0));
    }

    @Test
    void deveRejeitarDescontoDeVinteEUmPorCento() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "desconto", "gerente01", "GERENTE", "{\"percentual\": 21}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveNegarDescontoParaAtendenteEAdmin() throws Exception {
        String id = criarPedido("ENTREGA");

        postar(id, "desconto", "atendente01", "ATENDENTE", "{\"percentual\": 10}")
                .andExpect(status().isForbidden());
        postar(id, "desconto", "admin", "ADMIN", "{\"percentual\": 10}")
                .andExpect(status().isForbidden());
    }

    @Test
    void naoDeveAplicarDescontoDepoisDaSaida() throws Exception {
        String id = criarPedido("RETIRADA");
        for (int passo = 0; passo < 3; passo++) {
            postar(id, "avancar", "atendente01", "ATENDENTE", "{}").andExpect(status().isOk());
        }

        postar(id, "desconto", "gerente01", "GERENTE", "{\"percentual\": 10}")
                .andExpect(status().isConflict());
    }

    @Test
    void deveResponder404ParaClienteInexistente() throws Exception {
        String corpo = """
                {"clienteId": "naoexiste", "tipoSaida": "ENTREGA", "itens": [{"produtoId": "%s", "quantidade": 1}]}
                """.formatted(pizza.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound());
    }

    private String criarPedido(String tipoSaida) throws Exception {
        String corpo = """
                {"clienteId": "%s", "tipoSaida": "%s", "itens": [{"produtoId": "%s", "quantidade": 1}]}
                """.formatted(cliente.getId(), tipoSaida, pizza.getId());

        String resposta = mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos")
                        .with(user("atendente01").roles("ATENDENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions postar(String id, String acao, String usuario, String perfil, String corpo)
            throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/pedidos/" + id + "/" + acao)
                .with(user(usuario).roles(perfil))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions avancar(String id, String usuario, String perfil, String corpo) throws Exception {
        MockHttpServletRequestBuilder requisicao = MockMvcRequestBuilders.post("/api/pedidos/" + id + "/avancar")
                .with(user(usuario).roles(perfil))
                .with(csrf());
        if (!corpo.isEmpty()) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk());
    }
}
