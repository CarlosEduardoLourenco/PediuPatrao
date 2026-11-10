package com.umc.pediupatrao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculadoraDeIndicadores {

    private static final int ESCALA_MONETARIA = 2;
    private static final int ESCALA_PERCENTUAL = 1;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraDeIndicadores() {
    }

    public static BigDecimal ticketMedio(BigDecimal faturamento, long pedidos) {
        if (pedidos == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_MONETARIA);
        }
        return faturamento.divide(BigDecimal.valueOf(pedidos), ESCALA_MONETARIA, ARREDONDAMENTO);
    }

    public static BigDecimal percentual(long parte, long todo) {
        if (todo == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL);
        }
        return BigDecimal.valueOf(parte).multiply(CEM)
                .divide(BigDecimal.valueOf(todo), ESCALA_PERCENTUAL, ARREDONDAMENTO);
    }

    public static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(CEM).divide(anterior, ESCALA_PERCENTUAL, ARREDONDAMENTO);
    }

    public static BigDecimal variacao(long atual, long anterior) {
        return variacao(BigDecimal.valueOf(atual), BigDecimal.valueOf(anterior));
    }
}
