package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraDeIndicadoresTest {

    @Test
    void deveDividirOFaturamentoPelosPedidosNoTicketMedio() {
        assertEquals(new BigDecimal("70.93"), CalculadoraDeIndicadores.ticketMedio(new BigDecimal("212.80"), 3));
    }

    @Test
    void deveDevolverTicketMedioZeroSemPedidos() {
        assertEquals(new BigDecimal("0.00"), CalculadoraDeIndicadores.ticketMedio(new BigDecimal("0.00"), 0));
    }

    @Test
    void deveCalcularPercentualComUmaCasaDecimal() {
        assertEquals(new BigDecimal("33.3"), CalculadoraDeIndicadores.percentual(1, 3));
    }

    @Test
    void deveDevolverPercentualZeroQuandoOTotalEZero() {
        assertEquals(new BigDecimal("0.0"), CalculadoraDeIndicadores.percentual(0, 0));
    }

    @Test
    void deveCalcularVariacaoPositivaENegativa() {
        assertEquals(new BigDecimal("25.0"),
                CalculadoraDeIndicadores.variacao(new BigDecimal("125.00"), new BigDecimal("100.00")));
        assertEquals(new BigDecimal("-50.0"), CalculadoraDeIndicadores.variacao(2, 4));
    }

    @Test
    void naoDeveCalcularVariacaoQuandoOPeriodoAnteriorNaoTeveMovimento() {
        assertNull(CalculadoraDeIndicadores.variacao(new BigDecimal("80.00"), new BigDecimal("0.00")));
        assertNull(CalculadoraDeIndicadores.variacao(3, 0));
    }
}
