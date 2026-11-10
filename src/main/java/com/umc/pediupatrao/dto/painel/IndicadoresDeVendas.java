package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;

public record IndicadoresDeVendas(
        PeriodoDoPainel periodo,
        BigDecimal faturamento,
        long pedidosFinalizados,
        BigDecimal ticketMedio,
        BigDecimal descontosConcedidos,
        long pedidosCancelados,
        BigDecimal taxaDeCancelamento,
        Long tempoMedioDePreparoEmMinutos,
        Long tempoMedioDeAtendimentoEmMinutos,
        ComparativoComPeriodoAnterior comparativo) {
}
