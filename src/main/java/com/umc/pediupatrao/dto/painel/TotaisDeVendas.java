package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;

public record TotaisDeVendas(BigDecimal faturamento, long pedidos, BigDecimal descontos) {
}
