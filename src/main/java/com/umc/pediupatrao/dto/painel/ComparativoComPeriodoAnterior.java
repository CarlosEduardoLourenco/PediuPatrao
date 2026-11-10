package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;

public record ComparativoComPeriodoAnterior(
        PeriodoDoPainel periodoAnterior,
        BigDecimal faturamentoAnterior,
        BigDecimal variacaoDoFaturamento,
        long pedidosAnteriores,
        BigDecimal variacaoDosPedidos,
        BigDecimal ticketMedioAnterior,
        BigDecimal variacaoDoTicketMedio) {
}
