package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;

public record AvancoRequisicao(StatusPedido destino, String entregadorId) {

    public static AvancoRequisicao semDestino() {
        return new AvancoRequisicao(null, null);
    }

    public static AvancoRequisicao para(StatusPedido destino) {
        return new AvancoRequisicao(destino, null);
    }

    public static AvancoRequisicao paraEntrega(String entregadorId) {
        return new AvancoRequisicao(null, entregadorId);
    }
}
