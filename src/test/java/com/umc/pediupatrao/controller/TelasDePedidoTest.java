package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class TelasDePedidoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Cliente cliente;
    private Produto ativo;

    @BeforeEach
    void prepararDados() {
        cliente = new Cliente();
        cliente.setNome("Cliente da Tela");
        cliente.setTelefone("(11) 99999-0000");
        cliente = clienteRepository.save(cliente);

        ativo = produto("Pizza Visivel", true);
        produto("Pizza Oculta", false);
    }

    @AfterEach
    void limparDados() {
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
    }

    @Test
    void deveMostrarTodasAsAcoesAoGerenteEmPedidoRecebido() throws Exception {
        String id = pedidoEm(StatusPedido.RECEBIDO).getId();

        detalhe(id, "GERENTE")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Avançar para EM_PREPARACAO")))
                .andExpect(content().string(containsString("modalDesconto")))
                .andExpect(content().string(containsString("modalCancelamento")))
                .andExpect(content().string(containsString("Item indisponível")));
    }

    @Test
    void deveMostrarApenasAvancoAoAtendente() throws Exception {
        String id = pedidoEm(StatusPedido.RECEBIDO).getId();

        detalhe(id, "ATENDENTE")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Avançar para EM_PREPARACAO")))
                .andExpect(content().string(not(containsString("modalDesconto"))))
                .andExpect(content().string(not(containsString("modalCancelamento"))));
    }

    @Test
    void naoDeveMostrarAcoesAoAdmin() throws Exception {
        String id = pedidoEm(StatusPedido.RECEBIDO).getId();

        detalhe(id, "ADMIN")
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Avançar para"))))
                .andExpect(content().string(not(containsString("modalCancelamento"))));
    }

    @Test
    void naoDeveOferecerCancelamentoNemDescontoDepoisDaSaida() throws Exception {
        String id = pedidoEm(StatusPedido.SAIU_PARA_ENTREGA).getId();

        detalhe(id, "GERENTE")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Avançar para FINALIZADO")))
                .andExpect(content().string(not(containsString("data-bs-target=\"#modalCancelamento\""))))
                .andExpect(content().string(not(containsString("data-bs-target=\"#modalDesconto\""))));
    }

    @Test
    void naoDeveOferecerAcoesEmPedidoFinalizado() throws Exception {
        String id = pedidoEm(StatusPedido.FINALIZADO).getId();

        detalhe(id, "GERENTE")
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Avançar para"))))
                .andExpect(content().string(not(containsString("data-bs-target"))));
    }

    @Test
    void deveRedirecionarQuandoOPedidoNaoExiste() throws Exception {
        detalhe("inexistente", "GERENTE").andExpect(status().is3xxRedirection());
    }

    @Test
    void deveListarApenasProdutosAtivosNoFormularioDeNovoPedido() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pedidos/novo").with(user("atendente01").roles("ATENDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente da Tela")))
                .andExpect(content().string(containsString("Pizza Visivel")))
                .andExpect(content().string(not(containsString("Pizza Oculta"))));
    }

    @Test
    void deveEsconderAcoesDeCatalogoDeQuemNaoEGerente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/produtos").with(user("gerente01").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Novo Produto")));
        mockMvc.perform(MockMvcRequestBuilders.get("/produtos").with(user("atendente01").roles("ATENDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pizza Visivel")))
                .andExpect(content().string(not(containsString("Novo Produto"))));
    }

    @Test
    void deveListarPedidosComLinkDeDetalhes() throws Exception {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/pedidos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/pedidos/" + pedido.getId())))
                .andExpect(content().string(not(containsString("Novo Pedido"))));
    }

    private ResultActions detalhe(String id, String perfil) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/pedidos/" + id).with(user("teste").roles(perfil)));
    }

    private Produto produto(String nome, boolean ativoNoCatalogo) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setTipo("Pizza");
        produto.setPreco(40.0);
        produto.setAtivo(ativoNoCatalogo);
        return produtoRepository.save(produto);
    }

    private Pedido pedidoEm(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoSaida(TipoSaida.ENTREGA);
        pedido.setStatus(status);
        pedido.setItens(List.of(new ItemPedido(ativo.getId(), "Pizza Visivel", new BigDecimal("40.00"), 1,
                new BigDecimal("40.00"))));
        pedido.setSubtotal(new BigDecimal("40.00"));
        pedido.setDescontoPercentual(new BigDecimal("0.00"));
        pedido.setValorDesconto(new BigDecimal("0.00"));
        pedido.setValorTotal(new BigDecimal("40.00"));
        pedido.setDataHoraEntrada(LocalDateTime.of(2026, 9, 23, 14, 35));
        pedido.setUsuarioEntrada("atendente01");
        return pedidoRepository.save(pedido);
    }
}
