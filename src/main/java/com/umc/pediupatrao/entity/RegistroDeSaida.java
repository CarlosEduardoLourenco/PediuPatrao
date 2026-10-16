package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;

public record RegistroDeSaida(LocalDateTime dataHora, String usuario, String entregadorId,
        String entregadorNome) {

    public boolean possuiEntregador() {
        return entregadorId != null;
    }
}
