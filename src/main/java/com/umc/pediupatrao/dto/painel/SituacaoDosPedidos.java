package com.umc.pediupatrao.dto.painel;

import java.util.List;

public record SituacaoDosPedidos(
        PeriodoDoPainel periodo,
        long totalNoPeriodo,
        List<QuantidadePorStatus> noPeriodo,
        long totalEmAndamentoAgora,
        List<QuantidadePorStatus> emAndamentoAgora) {
}
