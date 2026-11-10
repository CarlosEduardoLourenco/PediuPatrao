package com.umc.pediupatrao.dto.painel;

import com.umc.pediupatrao.entity.TipoSaida;
import java.util.List;

public record OpcoesDeFiltro(
        PeriodoDoPainel periodoPadrao,
        int diasDoPeriodoMaximo,
        List<TipoSaida> tiposSaida,
        List<String> categorias,
        List<Granularidade> granularidades,
        List<DimensaoDeVenda> dimensoes,
        List<CriterioDeRanking> criteriosDeRanking,
        int limiteMaximoDoRanking) {
}
