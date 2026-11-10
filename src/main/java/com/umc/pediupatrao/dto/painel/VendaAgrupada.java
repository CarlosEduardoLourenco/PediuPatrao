package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;

public record VendaAgrupada(String chave, BigDecimal faturamento, long pedidos) {
}
