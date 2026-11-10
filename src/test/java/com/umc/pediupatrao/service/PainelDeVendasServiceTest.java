package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.painel.CancelamentosPorMotivo;
import com.umc.pediupatrao.dto.painel.CriterioDeRanking;
import com.umc.pediupatrao.dto.painel.DimensaoDeVenda;
import com.umc.pediupatrao.dto.painel.DistribuicaoDeVendas;
import com.umc.pediupatrao.dto.painel.FatiaDeVendas;
import com.umc.pediupatrao.dto.painel.FiltroDoPainel;
import com.umc.pediupatrao.dto.painel.Granularidade;
import com.umc.pediupatrao.dto.painel.IndicadoresDeVendas;
import com.umc.pediupatrao.dto.painel.OpcoesDeFiltro;
import com.umc.pediupatrao.dto.painel.ParametrosDoPainel;
import com.umc.pediupatrao.dto.painel.PeriodoDoPainel;
import com.umc.pediupatrao.dto.painel.PontoDaSerie;
import com.umc.pediupatrao.dto.painel.QuantidadePorMotivo;
import com.umc.pediupatrao.dto.painel.QuantidadePorStatus;
import com.umc.pediupatrao.dto.painel.SerieDeVendas;
import com.umc.pediupatrao.dto.painel.SituacaoDosPedidos;
import com.umc.pediupatrao.dto.painel.TemposMedios;
import com.umc.pediupatrao.dto.painel.TotaisDeVendas;
import com.umc.pediupatrao.dto.painel.VendaAgrupada;
import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.repository.AgregacoesDePedidos;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PainelDeVendasServiceTest {

    private static final Clock DIA_15_DE_OUTUBRO = Clock.fixed(Instant.parse("2026-10-15T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private static final PeriodoDoPainel SEMANA = new PeriodoDoPainel(LocalDate.of(2026, 10, 8),
            LocalDate.of(2026, 10, 14));
    private static final FiltroDoPainel FILTRO_DA_SEMANA = new FiltroDoPainel(SEMANA, null);

    @Mock
    private AgregacoesDePedidos agregacoes;

    private PainelDeVendasService servico;

    @BeforeEach
    void preparar() {
        servico = new PainelDeVendasService(agregacoes, DIA_15_DE_OUTUBRO);
    }

    @Test
    void deveConsultarOsUltimosTrintaDiasQuandoNenhumPeriodoForInformado() {
        FiltroDoPainel esperado = new FiltroDoPainel(
                new PeriodoDoPainel(LocalDate.of(2026, 9, 16), LocalDate.of(2026, 10, 15)), TipoSaida.RETIRADA);
        when(agregacoes.pedidosPorStatus(esperado)).thenReturn(Map.of());
        when(agregacoes.pedidosEmAndamentoPorStatus(esperado)).thenReturn(Map.of());

        SituacaoDosPedidos situacao = servico.situacao(new ParametrosDoPainel(null, null, TipoSaida.RETIRADA));

        assertEquals(esperado.periodo(), situacao.periodo());
    }

    @Test
    void deveRecusarPeriodoInvertidoSemConsultarOBanco() {
        ParametrosDoPainel invertido = new ParametrosDoPainel(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 8),
                null);

        assertThrows(DadosInvalidosException.class, () -> servico.indicadores(invertido));
        verifyNoInteractions(agregacoes);
    }

    @Test
    void deveMontarOsIndicadoresComOComparativoDoPeriodoAnterior() {
        when(agregacoes.totaisFinalizados(FILTRO_DA_SEMANA))
                .thenReturn(new TotaisDeVendas(new BigDecimal("200.00"), 4, new BigDecimal("12.00")));
        when(agregacoes.totaisFinalizados(FILTRO_DA_SEMANA.noPeriodoAnterior()))
                .thenReturn(new TotaisDeVendas(new BigDecimal("100.00"), 2, new BigDecimal("0.00")));
        when(agregacoes.pedidosPorStatus(FILTRO_DA_SEMANA))
                .thenReturn(Map.of(StatusPedido.FINALIZADO, 4L, StatusPedido.CANCELADO, 1L));
        when(agregacoes.temposMediosDosFinalizados(FILTRO_DA_SEMANA)).thenReturn(new TemposMedios(18L, 42L));

        IndicadoresDeVendas indicadores = servico.indicadores(parametrosDaSemana());

        assertEquals(new BigDecimal("200.00"), indicadores.faturamento());
        assertEquals(4, indicadores.pedidosFinalizados());
        assertEquals(new BigDecimal("50.00"), indicadores.ticketMedio());
        assertEquals(new BigDecimal("12.00"), indicadores.descontosConcedidos());
        assertEquals(1, indicadores.pedidosCancelados());
        assertEquals(new BigDecimal("20.0"), indicadores.taxaDeCancelamento());
        assertEquals(18L, indicadores.tempoMedioDePreparoEmMinutos());
        assertEquals(42L, indicadores.tempoMedioDeAtendimentoEmMinutos());
        assertEquals(SEMANA.anterior(), indicadores.comparativo().periodoAnterior());
        assertEquals(new BigDecimal("100.0"), indicadores.comparativo().variacaoDoFaturamento());
        assertEquals(new BigDecimal("100.0"), indicadores.comparativo().variacaoDosPedidos());
        assertEquals(new BigDecimal("0.0"), indicadores.comparativo().variacaoDoTicketMedio());
    }

    @Test
    void deveDeixarAsVariacoesVaziasQuandoOPeriodoAnteriorNaoTeveVendas() {
        when(agregacoes.totaisFinalizados(FILTRO_DA_SEMANA))
                .thenReturn(new TotaisDeVendas(new BigDecimal("90.00"), 1, new BigDecimal("0.00")));
        when(agregacoes.totaisFinalizados(FILTRO_DA_SEMANA.noPeriodoAnterior()))
                .thenReturn(new TotaisDeVendas(new BigDecimal("0.00"), 0, new BigDecimal("0.00")));
        when(agregacoes.pedidosPorStatus(FILTRO_DA_SEMANA)).thenReturn(Map.of());
        when(agregacoes.temposMediosDosFinalizados(FILTRO_DA_SEMANA)).thenReturn(TemposMedios.semRegistros());

        IndicadoresDeVendas indicadores = servico.indicadores(parametrosDaSemana());

        assertNull(indicadores.comparativo().variacaoDoFaturamento());
        assertNull(indicadores.comparativo().variacaoDosPedidos());
        assertNull(indicadores.comparativo().variacaoDoTicketMedio());
        assertEquals(new BigDecimal("0.0"), indicadores.taxaDeCancelamento());
        assertNull(indicadores.tempoMedioDePreparoEmMinutos());
    }

    @Test
    void devePreencherOsDiasSemVendaNaSerieDiaria() {
        FiltroDoPainel tresDias = filtroEntre(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        when(agregacoes.vendasFinalizadasPorDia(tresDias))
                .thenReturn(List.of(new VendaAgrupada("2026-10-02", new BigDecimal("80.00"), 2)));

        SerieDeVendas serie = servico.serie(parametros(tresDias), Granularidade.DIA);

        assertEquals(List.of(
                new PontoDaSerie(LocalDate.of(2026, 10, 1), "01/10", new BigDecimal("0.00"), 0),
                new PontoDaSerie(LocalDate.of(2026, 10, 2), "02/10", new BigDecimal("80.00"), 2),
                new PontoDaSerie(LocalDate.of(2026, 10, 3), "03/10", new BigDecimal("0.00"), 0)), serie.pontos());
    }

    @Test
    void deveSomarOsDiasDeCadaSemanaComecandoNaSegunda() {
        FiltroDoPainel duasSemanas = filtroEntre(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14));
        when(agregacoes.vendasFinalizadasPorDia(duasSemanas)).thenReturn(List.of(
                new VendaAgrupada("2026-10-02", new BigDecimal("50.00"), 1),
                new VendaAgrupada("2026-10-05", new BigDecimal("30.00"), 1),
                new VendaAgrupada("2026-10-11", new BigDecimal("20.00"), 2)));

        SerieDeVendas serie = servico.serie(parametros(duasSemanas), Granularidade.SEMANA);

        assertEquals(List.of(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)),
                serie.pontos().stream().map(PontoDaSerie::inicio).toList());
        assertEquals(new BigDecimal("50.00"), serie.pontos().get(1).faturamento());
        assertEquals(3, serie.pontos().get(1).pedidos());
        assertEquals(0, serie.pontos().get(2).pedidos());
    }

    @Test
    void deveAgruparASeriePorMes() {
        FiltroDoPainel doisMeses = filtroEntre(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 10));
        when(agregacoes.vendasFinalizadasPorDia(doisMeses)).thenReturn(List.of(
                new VendaAgrupada("2026-09-25", new BigDecimal("40.00"), 1),
                new VendaAgrupada("2026-10-03", new BigDecimal("60.00"), 1),
                new VendaAgrupada("2026-10-09", new BigDecimal("15.00"), 1)));

        SerieDeVendas serie = servico.serie(parametros(doisMeses), Granularidade.MES);

        assertEquals(List.of("09/2026", "10/2026"), serie.pontos().stream().map(PontoDaSerie::rotulo).toList());
        assertEquals(new BigDecimal("75.00"), serie.pontos().get(1).faturamento());
    }

    @Test
    void devePreencherTodasAsHorasDoDiaNaDistribuicao() {
        when(agregacoes.vendasFinalizadasPor(DimensaoDeVenda.HORA, FILTRO_DA_SEMANA))
                .thenReturn(List.of(new VendaAgrupada("20", new BigDecimal("300.00"), 6)));

        DistribuicaoDeVendas distribuicao = servico.distribuicao(parametrosDaSemana(), DimensaoDeVenda.HORA);

        assertEquals(24, distribuicao.fatias().size());
        assertEquals(new FatiaDeVendas("20", "20h", new BigDecimal("300.00"), 6), distribuicao.fatias().get(20));
        assertEquals(new FatiaDeVendas("0", "0h", new BigDecimal("0.00"), 0), distribuicao.fatias().get(0));
    }

    @Test
    void deveNomearOsDiasDaSemanaNaDistribuicao() {
        when(agregacoes.vendasFinalizadasPor(DimensaoDeVenda.DIA_DA_SEMANA, FILTRO_DA_SEMANA)).thenReturn(List.of());

        DistribuicaoDeVendas distribuicao = servico.distribuicao(parametrosDaSemana(), DimensaoDeVenda.DIA_DA_SEMANA);

        assertEquals(List.of("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"),
                distribuicao.fatias().stream().map(FatiaDeVendas::rotulo).toList());
    }

    @Test
    void deveListarTodosOsStatusComTotaisNaSituacao() {
        when(agregacoes.pedidosPorStatus(FILTRO_DA_SEMANA))
                .thenReturn(Map.of(StatusPedido.FINALIZADO, 7L, StatusPedido.CANCELADO, 1L));
        when(agregacoes.pedidosEmAndamentoPorStatus(FILTRO_DA_SEMANA))
                .thenReturn(Map.of(StatusPedido.EM_PREPARACAO, 2L));

        SituacaoDosPedidos situacao = servico.situacao(parametrosDaSemana());

        assertEquals(StatusPedido.values().length, situacao.noPeriodo().size());
        assertEquals(8, situacao.totalNoPeriodo());
        assertEquals(List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO,
                        StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO),
                situacao.emAndamentoAgora().stream().map(QuantidadePorStatus::status).toList());
        assertEquals(2, situacao.totalEmAndamentoAgora());
    }

    @Test
    void deveListarTodosOsMotivosDeCancelamentoComDescricao() {
        when(agregacoes.cancelamentosPorMotivo(FILTRO_DA_SEMANA)).thenReturn(Map.of("ITEM_INDISPONIVEL", 3L));

        CancelamentosPorMotivo cancelamentos = servico.cancelamentos(parametrosDaSemana());

        assertEquals(MotivoCancelamento.values().length, cancelamentos.motivos().size());
        assertEquals(3, cancelamentos.total());
        assertEquals(new QuantidadePorMotivo(MotivoCancelamento.ITEM_INDISPONIVEL, "Item indisponível", 3),
                cancelamentos.motivos().get(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 51})
    void deveRecusarLimiteDeRankingForaDaFaixa(int limite) {
        assertThrows(DadosInvalidosException.class,
                () -> servico.maisVendidos(parametrosDaSemana(), null, CriterioDeRanking.QUANTIDADE, limite));
        verifyNoInteractions(agregacoes);
    }

    @Test
    void deveIgnorarCategoriaEmBrancoNoRanking() {
        servico.maisVendidos(parametrosDaSemana(), "  ", CriterioDeRanking.FATURAMENTO, 5);

        verify(agregacoes).produtosMaisVendidos(FILTRO_DA_SEMANA, null, CriterioDeRanking.FATURAMENTO, 5);
    }

    @Test
    void deveOferecerAsOpcoesDeFiltroComOPeriodoPadrao() {
        when(agregacoes.categoriasConhecidas()).thenReturn(List.of("Bebida", "Pizza"));

        OpcoesDeFiltro opcoes = servico.opcoesDeFiltro();

        assertEquals(new PeriodoDoPainel(LocalDate.of(2026, 9, 16), LocalDate.of(2026, 10, 15)),
                opcoes.periodoPadrao());
        assertEquals(List.of("Bebida", "Pizza"), opcoes.categorias());
        assertEquals(366, opcoes.diasDoPeriodoMaximo());
        assertEquals(50, opcoes.limiteMaximoDoRanking());
    }

    private ParametrosDoPainel parametrosDaSemana() {
        return parametros(FILTRO_DA_SEMANA);
    }

    private ParametrosDoPainel parametros(FiltroDoPainel filtro) {
        return new ParametrosDoPainel(filtro.periodo().de(), filtro.periodo().ate(), filtro.tipoSaida());
    }

    private FiltroDoPainel filtroEntre(LocalDate de, LocalDate ate) {
        return new FiltroDoPainel(new PeriodoDoPainel(de, ate), null);
    }
}
