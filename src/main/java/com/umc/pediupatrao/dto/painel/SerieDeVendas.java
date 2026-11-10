package com.umc.pediupatrao.dto.painel;

import java.util.List;

public record SerieDeVendas(PeriodoDoPainel periodo, Granularidade granularidade, List<PontoDaSerie> pontos) {
}
