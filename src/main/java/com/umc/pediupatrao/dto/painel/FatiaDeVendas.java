package com.umc.pediupatrao.dto.painel;

import java.math.BigDecimal;

public record FatiaDeVendas(String chave, String rotulo, BigDecimal faturamento, long pedidos) {
}
