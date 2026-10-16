package com.umc.pediupatrao.entity;

import java.math.BigDecimal;

public record ItemPedido(String produtoId, String nomeProduto, BigDecimal precoUnitario,
        int quantidade, BigDecimal subtotal) {
}
