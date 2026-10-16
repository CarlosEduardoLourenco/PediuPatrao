package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.MotivoCancelamento;

public record CancelamentoRequisicao(MotivoCancelamento motivo, String observacao) {
}
