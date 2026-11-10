package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PontoDaSerie(LocalDate inicio, String rotulo, BigDecimal faturamento, long pedidos) {
}
