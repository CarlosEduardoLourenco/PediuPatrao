package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Cliente cliente;
    private Produto pizza;

    @BeforeEach
    void prepararDados() {
        mongoTemplate.remove(new Query(), RegistroAuditoria.class);
        cliente = new Cliente();
        cliente.setNome("Cliente de Teste");
        cliente.setTelefone("(11) 99999-1234");
        cliente = clienteRepository.save(cliente);

        pizza = new Produto();
        pizza.setNome("Pizza de Teste");
        pizza.setTipo("Pizza");
        pizza.setPreco(40.0);
        pizza.setAtivo(true);
        pizza = produtoRepository.save(pizza);
    }

    @AfterEach
    void limparDados() {
        mongoTemplate.remove(new Query(), RegistroAuditoria.class);
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        produtoRepository.delete(pizza);
    }

    @Test
    void deveAuditarCriacaoStatusDescontoECancelamentoDoPedido() throws Exception {
        String id = criarPedido();
        executar("POST", "/api/pedidos/" + id + "/desconto", "gerente01", "GERENTE", "{\"percentual\": 15}");
        executar("POST", "/api/pedidos/" + id + "/avancar", "atendente01", "ATENDENTE", "{}");
        executar("POST", "/api/pedidos/" + id + "/cancelar", "gerente01", "GERENTE",
                "{\"motivo\": \"ITEM_INDISPONIVEL\"}");

        executar("GET", "/api/auditoria?entidade=PEDIDO&entidadeId=" + id, "gerente01", "GERENTE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.content[?(@.operacao == 'CRIACAO_PEDIDO')].usuario").value("atendente01"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'CRIACAO_PEDIDO')].perfil").value("ATENDENTE"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'DESCONTO')].usuario").value("gerente01"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'DESCONTO')].alteracoes[0][?(@.campo == 'descontoPercentual')].valorAnterior")
                        .value("0.00"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'DESCONTO')].alteracoes[0][?(@.campo == 'descontoPercentual')].valorNovo")
                        .value("15.00"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'MUDANCA_STATUS')].alteracoes[0][?(@.campo == 'status')].valorAnterior")
                        .value("RECEBIDO"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'MUDANCA_STATUS')].alteracoes[0][?(@.campo == 'status')].valorNovo")
                        .value("EM_PREPARACAO"))
                .andExpect(jsonPath("$.content[?(@.operacao == 'CANCELAMENTO')].justificativa")
                        .value("Item indisponível"))
                .andExpect(jsonPath("$.content[0].dataHora").isNotEmpty());
    }

    @Test
    void deveAuditarAlteracaoDeLogradouroDoClienteComUsuarioEPerfil() throws Exception {
        executar("PUT", "/api/clientes/" + cliente.getId(), "atendente01", "ATENDENTE",
                "{\"nome\": \"Cliente de Teste\", \"telefone\": \"(11) 99999-1234\", \"logradouro\": \"Rua das Flores\"}")
                .andExpect(status().isOk());
        executar("PUT", "/api/clientes/" + cliente.getId(), "atendente01", "ATENDENTE",
                "{\"nome\": \"Cliente de Teste\", \"telefone\": \"(11) 99999-1234\", \"logradouro\": \"Av. Brasil\"}")
                .andExpect(status().isOk());

        executar("GET", "/api/auditoria?entidade=CLIENTE&entidadeId=" + cliente.getId(), "admin", "ADMIN", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].operacao").value("ALTERACAO_CLIENTE"))
                .andExpect(jsonPath("$.content[0].usuario").value("atendente01"))
                .andExpect(jsonPath("$.content[0].perfil").value("ATENDENTE"))
                .andExpect(jsonPath("$.content[0].alteracoes.length()").value(1))
                .andExpect(jsonPath("$.content[0].alteracoes[0].campo").value("logradouro"))
                .andExpect(jsonPath("$.content[0].alteracoes[0].valorAnterior").value("Rua das Flores"))
                .andExpect(jsonPath("$.content[0].alteracoes[0].valorNovo").value("Av. Brasil"));
    }

    @Test
    void deveAuditarTelefoneENomeDoClienteComOsValoresAnteriorENovo() throws Exception {
        executar("PUT", "/api/clientes/" + cliente.getId(), "atendente01", "ATENDENTE",
                "{\"nome\": \"Nome Novo\", \"telefone\": \"(11) 98888-5678\"}")
                .andExpect(status().isOk());

        executar("GET", "/api/auditoria?entidade=CLIENTE", "gerente01", "GERENTE", null)
                .andExpect(jsonPath("$.content[0].alteracoes.length()").value(2))
                .andExpect(jsonPath("$.content[0].alteracoes[?(@.campo == 'telefone')].valorAnterior")
                        .value("(11) 99999-1234"))
                .andExpect(jsonPath("$.content[0].alteracoes[?(@.campo == 'telefone')].valorNovo")
                        .value("(11) 98888-5678"))
                .andExpect(jsonPath("$.content[0].alteracoes[?(@.campo == 'nome')].valorAnterior")
                        .value("Cliente de Teste"))
                .andExpect(jsonPath("$.content[0].alteracoes[?(@.campo == 'nome')].valorNovo").value("Nome Novo"));
    }

    @Test
    void naoDeveAuditarAlteracaoSemMudancaNosCamposAuditados() throws Exception {
        executar("PUT", "/api/clientes/" + cliente.getId(), "atendente01", "ATENDENTE",
                "{\"nome\": \"Cliente de Teste\", \"telefone\": \"(11) 99999-1234\"}")
                .andExpect(status().isOk());

        executar("GET", "/api/auditoria?entidade=CLIENTE", "gerente01", "GERENTE", null)
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void deveNegarConsultaDeAuditoriaAoAtendente() throws Exception {
        executar("GET", "/api/auditoria", "atendente01", "ATENDENTE", null).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void naoDevePermitirEscritaNaAuditoriaPelaApi(String metodo) throws Exception {
        executar(metodo, "/api/auditoria", "admin", "ADMIN", "{}").andExpect(status().isMethodNotAllowed());
        executar(metodo, "/api/auditoria/qualquer", "admin", "ADMIN", "{}")
                .andExpect(status().is4xxClientError());
    }

    @Test
    void naoDeveExporMetodosDeAlteracaoOuExclusaoNoRepositorioDeAuditoria() {
        boolean possuiRemocao = Arrays.stream(AuditoriaRepository.class.getMethods())
                .map(Method::getName)
                .anyMatch(nome -> nome.startsWith("delete") || nome.startsWith("remove"));

        assertTrue(!possuiRemocao);
    }

    @Test
    void deveFiltrarPorPeriodoSemRetornarRegistrosFuturos() throws Exception {
        criarPedido();

        executar("GET", "/api/auditoria?de=2999-01-01", "gerente01", "GERENTE", null)
                .andExpect(jsonPath("$.page.totalElements").value(0));
        executar("GET", "/api/auditoria?ate=2000-01-01", "gerente01", "GERENTE", null)
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void deveFiltrarPorUsuario() throws Exception {
        criarPedido();

        executar("GET", "/api/auditoria?usuario=atendente01", "admin", "ADMIN", null)
                .andExpect(jsonPath("$.page.totalElements").value(1));
        executar("GET", "/api/auditoria?usuario=outro", "admin", "ADMIN", null)
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void naoDeveExporSenhaOuTokenNaConsulta() throws Exception {
        criarPedido();

        executar("GET", "/api/auditoria", "admin", "ADMIN", null)
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("token"))));
    }

    @Test
    void deveRenderizarATelaDeAuditoriaParaAdminEGerente() throws Exception {
        criarPedido();

        executar("GET", "/auditoria", "admin", "ADMIN", null).andExpect(status().isOk());
        executar("GET", "/auditoria?entidade=PEDIDO&usuario=atendente01", "gerente01", "GERENTE", null)
                .andExpect(status().isOk());
        executar("GET", "/auditoria", "atendente01", "ATENDENTE", null).andExpect(status().isForbidden());
    }

    private String criarPedido() throws Exception {
        String corpo = """
                {"clienteId": "%s", "tipoSaida": "ENTREGA", "itens": [{"produtoId": "%s", "quantidade": 1}]}
                """.formatted(cliente.getId(), pizza.getId());
        String resposta = executar("POST", "/api/pedidos", "atendente01", "ATENDENTE", corpo)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions executar(String metodo, String caminho, String usuario, String perfil, String corpo)
            throws Exception {
        var requisicao = MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo), caminho)
                .with(user(usuario).roles(perfil))
                .with(csrf());
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(requisicao);
    }
}
