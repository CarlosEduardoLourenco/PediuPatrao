package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Produto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculadoraDePedido {

    private static final int ESCALA_MONETARIA = 2;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraDePedido() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(ESCALA_MONETARIA);
    }

    public static ItemPedido montarItem(Produto produto, int quantidade) {
        BigDecimal precoUnitario = BigDecimal.valueOf(produto.getPreco())
                .setScale(ESCALA_MONETARIA, ARREDONDAMENTO);
        BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        return new ItemPedido(produto.getId(), produto.getNome(), precoUnitario, quantidade, subtotal);
    }

    public static BigDecimal somarSubtotais(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(zero(), BigDecimal::add);
    }

    public static BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal percentual) {
        return subtotal.multiply(percentual).divide(CEM, ESCALA_MONETARIA, ARREDONDAMENTO);
    }

    public static BigDecimal calcularTotal(BigDecimal subtotal, BigDecimal valorDesconto) {
        return subtotal.subtract(valorDesconto);
    }
}
