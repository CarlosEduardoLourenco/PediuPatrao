package com.umc.pediupatrao.dto.painel;

import com.umc.pediupatrao.entity.StatusPedido;

public record QuantidadePorStatus(StatusPedido status, long quantidade) {
}
