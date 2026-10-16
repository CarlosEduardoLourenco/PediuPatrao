package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;

public record RegistroDeCancelamento(MotivoCancelamento motivo, String observacao, String usuario,
        LocalDateTime dataHora) {

    public String justificativa() {
        boolean possuiObservacao = observacao != null && !observacao.isBlank();
        return possuiObservacao ? motivo.getDescricao() + ": " + observacao : motivo.getDescricao();
    }
}
