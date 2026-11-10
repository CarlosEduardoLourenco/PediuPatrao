package com.umc.pediupatrao.dto.painel;

import java.util.List;

public record DistribuicaoDeVendas(PeriodoDoPainel periodo, DimensaoDeVenda dimensao, List<FatiaDeVendas> fatias) {
}
