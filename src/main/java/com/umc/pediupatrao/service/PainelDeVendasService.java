package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.painel.CancelamentosPorMotivo;
import com.umc.pediupatrao.dto.painel.ComparativoComPeriodoAnterior;
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
import com.umc.pediupatrao.dto.painel.RankingDeProdutos;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('GERENTE')")
public class PainelDeVendasService {

    static final int LIMITE_MAXIMO_DO_RANKING = 50;

    private final AgregacoesDePedidos agregacoes;
    private final Clock relogio;

    public PainelDeVendasService(AgregacoesDePedidos agregacoes, Clock relogio) {
        this.agregacoes = agregacoes;
        this.relogio = relogio;
    }

    public IndicadoresDeVendas indicadores(ParametrosDoPainel parametros) {
        FiltroDoPainel filtro = filtrar(parametros);
        TotaisDeVendas atual = agregacoes.totaisFinalizados(filtro);
        TotaisDeVendas anterior = agregacoes.totaisFinalizados(filtro.noPeriodoAnterior());
        long cancelados = agregacoes.pedidosPorStatus(filtro).getOrDefault(StatusPedido.CANCELADO, 0L);
        TemposMedios tempos = agregacoes.temposMediosDosFinalizados(filtro);
        BigDecimal ticketMedio = CalculadoraDeIndicadores.ticketMedio(atual.faturamento(), atual.pedidos());

        return new IndicadoresDeVendas(filtro.periodo(), atual.faturamento(), atual.pedidos(), ticketMedio,
                atual.descontos(), cancelados,
                CalculadoraDeIndicadores.percentual(cancelados, cancelados + atual.pedidos()),
                tempos.preparoEmMinutos(), tempos.atendimentoEmMinutos(),
                comparar(filtro.periodo().anterior(), ticketMedio, atual, anterior));
    }

    public SerieDeVendas serie(ParametrosDoPainel parametros, Granularidade granularidade) {
        FiltroDoPainel filtro = filtrar(parametros);
        Map<LocalDate, List<VendaAgrupada>> vendasPorIntervalo = agregacoes.vendasFinalizadasPorDia(filtro).stream()
                .collect(Collectors.groupingBy(
                        venda -> granularidade.inicioDoIntervalo(LocalDate.parse(venda.chave()))));

        List<PontoDaSerie> pontos = new ArrayList<>();
        LocalDate inicio = granularidade.inicioDoIntervalo(filtro.periodo().de());
        while (!inicio.isAfter(filtro.periodo().ate())) {
            List<VendaAgrupada> vendas = vendasPorIntervalo.getOrDefault(inicio, List.of());
            pontos.add(new PontoDaSerie(inicio, granularidade.rotulo(inicio), somarFaturamento(vendas),
                    somarPedidos(vendas)));
            inicio = granularidade.proximoIntervalo(inicio);
        }
        return new SerieDeVendas(filtro.periodo(), granularidade, pontos);
    }

    public DistribuicaoDeVendas distribuicao(ParametrosDoPainel parametros, DimensaoDeVenda dimensao) {
        FiltroDoPainel filtro = filtrar(parametros);
        Map<String, VendaAgrupada> vendasPorChave = agregacoes.vendasFinalizadasPor(dimensao, filtro).stream()
                .collect(Collectors.toMap(VendaAgrupada::chave, Function.identity()));

        List<FatiaDeVendas> fatias = dimensao.chaves().stream()
                .map(chave -> {
                    VendaAgrupada venda = vendasPorChave.get(chave);
                    return new FatiaDeVendas(chave, dimensao.rotulo(chave),
                            venda == null ? zero() : venda.faturamento(),
                            venda == null ? 0 : venda.pedidos());
                })
                .toList();
        return new DistribuicaoDeVendas(filtro.periodo(), dimensao, fatias);
    }

    public SituacaoDosPedidos situacao(ParametrosDoPainel parametros) {
        FiltroDoPainel filtro = filtrar(parametros);
        List<QuantidadePorStatus> noPeriodo = quantidadesPara(Arrays.asList(StatusPedido.values()),
                agregacoes.pedidosPorStatus(filtro));
        List<StatusPedido> statusEmAndamento = Arrays.stream(StatusPedido.values())
                .filter(status -> !status.ehFinal())
                .toList();
        List<QuantidadePorStatus> emAndamento = quantidadesPara(statusEmAndamento,
                agregacoes.pedidosEmAndamentoPorStatus(filtro));
        return new SituacaoDosPedidos(filtro.periodo(), somarQuantidades(noPeriodo), noPeriodo,
                somarQuantidades(emAndamento), emAndamento);
    }

    public CancelamentosPorMotivo cancelamentos(ParametrosDoPainel parametros) {
        FiltroDoPainel filtro = filtrar(parametros);
        Map<String, Long> quantidades = agregacoes.cancelamentosPorMotivo(filtro);
        List<QuantidadePorMotivo> motivos = Arrays.stream(MotivoCancelamento.values())
                .map(motivo -> new QuantidadePorMotivo(motivo, motivo.getDescricao(),
                        quantidades.getOrDefault(motivo.name(), 0L)))
                .toList();
        long total = motivos.stream().mapToLong(QuantidadePorMotivo::quantidade).sum();
        return new CancelamentosPorMotivo(filtro.periodo(), total, motivos);
    }

    public RankingDeProdutos maisVendidos(ParametrosDoPainel parametros, String categoria,
            CriterioDeRanking criterio, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO_DO_RANKING) {
            throw new DadosInvalidosException("O limite do ranking deve ficar entre 1 e " + LIMITE_MAXIMO_DO_RANKING);
        }
        FiltroDoPainel filtro = filtrar(parametros);
        String categoriaLimpa = categoria == null || categoria.isBlank() ? null : categoria.trim();
        return new RankingDeProdutos(filtro.periodo(), criterio, categoriaLimpa,
                agregacoes.produtosMaisVendidos(filtro, categoriaLimpa, criterio, limite));
    }

    public OpcoesDeFiltro opcoesDeFiltro() {
        return new OpcoesDeFiltro(
                PeriodoDoPainel.entre(null, null, hoje()),
                PeriodoDoPainel.DIAS_DO_PERIODO_MAXIMO,
                List.of(TipoSaida.values()),
                agregacoes.categoriasConhecidas(),
                List.of(Granularidade.values()),
                List.of(DimensaoDeVenda.values()),
                List.of(CriterioDeRanking.values()),
                LIMITE_MAXIMO_DO_RANKING);
    }

    private FiltroDoPainel filtrar(ParametrosDoPainel parametros) {
        ParametrosDoPainel informados = parametros == null ? ParametrosDoPainel.padrao() : parametros;
        PeriodoDoPainel periodo = PeriodoDoPainel.entre(informados.de(), informados.ate(), hoje());
        return new FiltroDoPainel(periodo, informados.tipoSaida());
    }

    private ComparativoComPeriodoAnterior comparar(PeriodoDoPainel periodoAnterior, BigDecimal ticketMedio,
            TotaisDeVendas atual, TotaisDeVendas anterior) {
        BigDecimal ticketAnterior = CalculadoraDeIndicadores.ticketMedio(anterior.faturamento(), anterior.pedidos());
        return new ComparativoComPeriodoAnterior(periodoAnterior,
                anterior.faturamento(),
                CalculadoraDeIndicadores.variacao(atual.faturamento(), anterior.faturamento()),
                anterior.pedidos(),
                CalculadoraDeIndicadores.variacao(atual.pedidos(), anterior.pedidos()),
                ticketAnterior,
                CalculadoraDeIndicadores.variacao(ticketMedio, ticketAnterior));
    }

    private List<QuantidadePorStatus> quantidadesPara(List<StatusPedido> statusExibidos,
            Map<StatusPedido, Long> quantidades) {
        return statusExibidos.stream()
                .map(status -> new QuantidadePorStatus(status, quantidades.getOrDefault(status, 0L)))
                .toList();
    }

    private long somarQuantidades(List<QuantidadePorStatus> quantidades) {
        return quantidades.stream().mapToLong(QuantidadePorStatus::quantidade).sum();
    }

    private BigDecimal somarFaturamento(List<VendaAgrupada> vendas) {
        return vendas.stream().map(VendaAgrupada::faturamento).reduce(zero(), BigDecimal::add);
    }

    private long somarPedidos(List<VendaAgrupada> vendas) {
        return vendas.stream().mapToLong(VendaAgrupada::pedidos).sum();
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
