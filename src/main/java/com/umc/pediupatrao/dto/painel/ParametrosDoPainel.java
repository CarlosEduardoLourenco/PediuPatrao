package com.umc.pediupatrao.dto.painel;

import com.umc.pediupatrao.entity.TipoSaida;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ParametrosDoPainel(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
        TipoSaida tipoSaida) {

    public static ParametrosDoPainel padrao() {
        return new ParametrosDoPainel(null, null, null);
    }
}
