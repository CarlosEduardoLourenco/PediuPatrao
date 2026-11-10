package com.umc.pediupatrao.dto.painel;

import java.util.List;

public record RankingDeProdutos(PeriodoDoPainel periodo, CriterioDeRanking criterio, String categoria,
        List<ProdutoVendido> produtos) {
}
