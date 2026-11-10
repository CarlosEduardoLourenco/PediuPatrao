package com.umc.pediupatrao.dto.painel;

import com.umc.pediupatrao.entity.TipoSaida;

public record FiltroDoPainel(PeriodoDoPainel periodo, TipoSaida tipoSaida) {

    public FiltroDoPainel noPeriodoAnterior() {
        return new FiltroDoPainel(periodo.anterior(), tipoSaida);
    }
}
