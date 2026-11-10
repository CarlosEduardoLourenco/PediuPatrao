package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.painel.CancelamentosPorMotivo;
import com.umc.pediupatrao.dto.painel.CriterioDeRanking;
import com.umc.pediupatrao.dto.painel.DimensaoDeVenda;
import com.umc.pediupatrao.dto.painel.DistribuicaoDeVendas;
import com.umc.pediupatrao.dto.painel.Granularidade;
import com.umc.pediupatrao.dto.painel.IndicadoresDeVendas;
import com.umc.pediupatrao.dto.painel.OpcoesDeFiltro;
import com.umc.pediupatrao.dto.painel.ParametrosDoPainel;
import com.umc.pediupatrao.dto.painel.RankingDeProdutos;
import com.umc.pediupatrao.dto.painel.SerieDeVendas;
import com.umc.pediupatrao.dto.painel.SituacaoDosPedidos;
import com.umc.pediupatrao.service.PainelDeVendasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelDeVendasService painelDeVendasService;

    public PainelController(PainelDeVendasService painelDeVendasService) {
        this.painelDeVendasService = painelDeVendasService;
    }

    @GetMapping("/indicadores")
    public IndicadoresDeVendas indicadores(@ModelAttribute ParametrosDoPainel parametros) {
        return painelDeVendasService.indicadores(parametros);
    }

    @GetMapping("/vendas/serie")
    public SerieDeVendas serie(@ModelAttribute ParametrosDoPainel parametros,
            @RequestParam(defaultValue = "DIA") Granularidade granularidade) {
        return painelDeVendasService.serie(parametros, granularidade);
    }

    @GetMapping("/vendas/distribuicao")
    public DistribuicaoDeVendas distribuicao(@ModelAttribute ParametrosDoPainel parametros,
            @RequestParam(defaultValue = "HORA") DimensaoDeVenda dimensao) {
        return painelDeVendasService.distribuicao(parametros, dimensao);
    }

    @GetMapping("/pedidos/situacao")
    public SituacaoDosPedidos situacao(@ModelAttribute ParametrosDoPainel parametros) {
        return painelDeVendasService.situacao(parametros);
    }

    @GetMapping("/pedidos/cancelamentos")
    public CancelamentosPorMotivo cancelamentos(@ModelAttribute ParametrosDoPainel parametros) {
        return painelDeVendasService.cancelamentos(parametros);
    }

    @GetMapping("/produtos/mais-vendidos")
    public RankingDeProdutos maisVendidos(@ModelAttribute ParametrosDoPainel parametros,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "QUANTIDADE") CriterioDeRanking ordenarPor,
            @RequestParam(defaultValue = "10") int limite) {
        return painelDeVendasService.maisVendidos(parametros, categoria, ordenarPor, limite);
    }

    @GetMapping("/filtros")
    public OpcoesDeFiltro filtros() {
        return painelDeVendasService.opcoesDeFiltro();
    }
}
