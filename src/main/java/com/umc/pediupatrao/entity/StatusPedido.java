package com.umc.pediupatrao.entity;

import java.util.Optional;

public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    public Optional<StatusPedido> proximo(TipoSaida tipoSaida) {
        return switch (this) {
            case RECEBIDO -> Optional.of(EM_PREPARACAO);
            case EM_PREPARACAO -> Optional.of(PRONTO);
            case PRONTO -> Optional.of(tipoSaida == TipoSaida.ENTREGA ? SAIU_PARA_ENTREGA : RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> Optional.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> Optional.empty();
        };
    }

    public boolean registraSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    public boolean ehFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }

    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
