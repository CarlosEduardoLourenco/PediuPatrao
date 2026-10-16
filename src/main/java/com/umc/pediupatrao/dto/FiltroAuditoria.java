package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoEntidade;
import java.time.LocalDate;

public record FiltroAuditoria(TipoEntidade entidade, String entidadeId, String usuario,
        LocalDate de, LocalDate ate) {

    public static FiltroAuditoria semRestricoes() {
        return new FiltroAuditoria(null, null, null, null, null);
    }
}
