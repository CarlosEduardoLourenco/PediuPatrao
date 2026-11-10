package com.umc.pediupatrao.dto.painel;

import java.util.List;

public record CancelamentosPorMotivo(PeriodoDoPainel periodo, long total, List<QuantidadePorMotivo> motivos) {
}
