package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;

public record ProdutoVendido(int posicao, String produtoId, String nome, String categoria, long quantidade,
        BigDecimal faturamento) {
}
