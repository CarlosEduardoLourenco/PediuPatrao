package com.umc.pediupatrao.dto.painel;

import com.umc.pediupatrao.entity.MotivoCancelamento;

public record QuantidadePorMotivo(MotivoCancelamento motivo, String descricao, long quantidade) {
}
