package com.umc.pediupatrao.dto.painel;

public record TemposMedios(Long preparoEmMinutos, Long atendimentoEmMinutos) {

    public static TemposMedios semRegistros() {
        return new TemposMedios(null, null);
    }
}
