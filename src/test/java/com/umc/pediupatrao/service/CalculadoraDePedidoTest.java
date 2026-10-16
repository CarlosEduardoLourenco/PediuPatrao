package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Produto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraDePedidoTest {

    @Test
    void deveCalcularSubtotalDoItemPelaQuantidade() {
        ItemPedido item = CalculadoraDePedido.montarItem(produto("p1", "Calabresa", 42.5), 3);

        assertEquals(new BigDecimal("42.50"), item.precoUnitario());
        assertEquals(new BigDecimal("127.50"), item.subtotal());
        assertEquals(3, item.quantidade());
    }

    @Test
    void deveCopiarNomeEPrecoDoProdutoNoMomentoDoPedido() {
        ItemPedido item = CalculadoraDePedido.montarItem(produto("p1", "Mussarela", 39.9), 1);

        assertEquals("p1", item.produtoId());
        assertEquals("Mussarela", item.nomeProduto());
    }

    @Test
    void deveSomarOsSubtotaisDosItens() {
        List<ItemPedido> itens = List.of(
                CalculadoraDePedido.montarItem(produto("p1", "Pizza", 40.0), 2),
                CalculadoraDePedido.montarItem(produto("p2", "Refrigerante", 8.5), 3));

        assertEquals(new BigDecimal("105.50"), CalculadoraDePedido.somarSubtotais(itens));
    }

    @Test
    void deveRetornarZeroParaListaVazia() {
        assertEquals(new BigDecimal("0.00"), CalculadoraDePedido.somarSubtotais(List.of()));
    }

    @Test
    void deveCalcularDescontoPercentualSobreOSubtotal() {
        BigDecimal desconto = CalculadoraDePedido.calcularDesconto(new BigDecimal("200.00"), new BigDecimal("15"));

        assertEquals(new BigDecimal("30.00"), desconto);
    }

    @Test
    void deveArredondarODescontoParaDuasCasasComMeioParaCima() {
        BigDecimal desconto = CalculadoraDePedido.calcularDesconto(new BigDecimal("33.33"), new BigDecimal("5"));

        assertEquals(new BigDecimal("1.67"), desconto);
    }

    @Test
    void deveSubtrairDescontoDoSubtotalNoTotal() {
        BigDecimal total = CalculadoraDePedido.calcularTotal(new BigDecimal("200.00"), new BigDecimal("30.00"));

        assertEquals(new BigDecimal("170.00"), total);
    }

    private Produto produto(String id, String nome, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
