package com.umc.pediupatrao.repository;

import static com.umc.pediupatrao.apoio.PedidoDeExemplo.entrandoEm;
import static com.umc.pediupatrao.apoio.PedidoDeExemplo.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.apoio.PedidoDeExemplo;
import com.umc.pediupatrao.dto.painel.CriterioDeRanking;
import com.umc.pediupatrao.dto.painel.DimensaoDeVenda;
import com.umc.pediupatrao.dto.painel.FiltroDoPainel;
import com.umc.pediupatrao.dto.painel.PeriodoDoPainel;
import com.umc.pediupatrao.dto.painel.ProdutoVendido;
import com.umc.pediupatrao.dto.painel.TemposMedios;
import com.umc.pediupatrao.dto.painel.TotaisDeVendas;
import com.umc.pediupatrao.dto.painel.VendaAgrupada;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AgregacoesDePedidosTest {

    private static final FiltroDoPainel OUTUBRO = filtro(null);

    @Autowired
    private AgregacoesDePedidos agregacoes;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @AfterEach
    void limparDados() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
    }

    @Test
    void deveSomarSomenteOsPedidosFinalizadosDoPeriodo() {
        registrarMovimentoDeOutubro();

        TotaisDeVendas totais = agregacoes.totaisFinalizados(OUTUBRO);

        assertEquals(new BigDecimal("212.80"), totais.faturamento());
        assertEquals(3, totais.pedidos());
        assertEquals(new BigDecimal("10.20"), totais.descontos());
    }

    @Test
    void deveFiltrarOsTotaisPeloTipoDeSaida() {
        registrarMovimentoDeOutubro();

        TotaisDeVendas entregas = agregacoes.totaisFinalizados(filtro(TipoSaida.ENTREGA));

        assertEquals(new BigDecimal("167.80"), entregas.faturamento());
        assertEquals(2, entregas.pedidos());
    }

    @Test
    void deveIncluirOPrimeiroEOUltimoInstanteDoPeriodoESomenteEles() {
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 1, 0, 0)).com(calabresa(1)));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 31, 23, 59)).com(calabresa(1)));
        salvar(entrandoEm(LocalDateTime.of(2026, 9, 30, 23, 59)).com(calabresa(1)));
        salvar(entrandoEm(LocalDateTime.of(2026, 11, 1, 0, 0)).com(calabresa(1)));

        assertEquals(2, agregacoes.totaisFinalizados(OUTUBRO).pedidos());
    }

    @Test
    void deveDevolverTotaisZeradosQuandoNaoHaVendas() {
        TotaisDeVendas totais = agregacoes.totaisFinalizados(OUTUBRO);

        assertEquals(new BigDecimal("0.00"), totais.faturamento());
        assertEquals(0, totais.pedidos());
        assertEquals(new BigDecimal("0.00"), totais.descontos());
    }

    @Test
    void deveAgruparPorDiaNoFusoDaPizzariaMesmoNoFimDaNoite() {
        registrarMovimentoDeOutubro();

        List<VendaAgrupada> porDia = agregacoes.vendasFinalizadasPorDia(OUTUBRO);

        assertEquals(List.of(
                new VendaAgrupada("2026-10-05", new BigDecimal("136.80"), 2),
                new VendaAgrupada("2026-10-07", new BigDecimal("76.00"), 1)), porDia);
    }

    @Test
    void deveAgruparPorHoraDeEntrada() {
        registrarMovimentoDeOutubro();

        Map<String, VendaAgrupada> porHora = porChave(agregacoes.vendasFinalizadasPor(DimensaoDeVenda.HORA, OUTUBRO));

        assertEquals(new BigDecimal("91.80"), porHora.get("19").faturamento());
        assertEquals(new BigDecimal("76.00"), porHora.get("20").faturamento());
        assertEquals(new BigDecimal("45.00"), porHora.get("23").faturamento());
        assertEquals(3, porHora.size());
    }

    @Test
    void deveAgruparPeloDiaDaSemanaNoPadraoIso() {
        registrarMovimentoDeOutubro();

        Map<String, VendaAgrupada> porDia = porChave(
                agregacoes.vendasFinalizadasPor(DimensaoDeVenda.DIA_DA_SEMANA, OUTUBRO));

        assertEquals(2, porDia.get("1").pedidos());
        assertEquals(1, porDia.get("3").pedidos());
    }

    @Test
    void deveAgruparPeloTipoDeSaida() {
        registrarMovimentoDeOutubro();

        Map<String, VendaAgrupada> porSaida = porChave(
                agregacoes.vendasFinalizadasPor(DimensaoDeVenda.TIPO_SAIDA, OUTUBRO));

        assertEquals(new BigDecimal("167.80"), porSaida.get("ENTREGA").faturamento());
        assertEquals(new BigDecimal("45.00"), porSaida.get("RETIRADA").faturamento());
    }

    @Test
    void deveContarOsPedidosDoPeriodoPorStatus() {
        registrarMovimentoDeOutubro();

        Map<StatusPedido, Long> porStatus = agregacoes.pedidosPorStatus(OUTUBRO);

        assertEquals(Map.of(StatusPedido.FINALIZADO, 3L, StatusPedido.CANCELADO, 1L, StatusPedido.RECEBIDO, 1L),
                porStatus);
    }

    @Test
    void deveContarOsPedidosEmAndamentoIndependenteDoPeriodo() {
        registrarMovimentoDeOutubro();

        Map<StatusPedido, Long> emAndamento = agregacoes.pedidosEmAndamentoPorStatus(OUTUBRO);

        assertEquals(Map.of(StatusPedido.RECEBIDO, 1L, StatusPedido.EM_PREPARACAO, 1L), emAndamento);
    }

    @Test
    void deveContarOsCancelamentosPorMotivo() {
        registrarMovimentoDeOutubro();
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 8, 19, 0)).com(calabresa(1))
                .canceladoPor(MotivoCancelamento.DEMORA_NA_PREPARACAO));

        Map<String, Long> porMotivo = agregacoes.cancelamentosPorMotivo(OUTUBRO);

        assertEquals(Map.of("CLIENTE_DESISTIU", 1L, "DEMORA_NA_PREPARACAO", 1L), porMotivo);
    }

    @Test
    void deveOrdenarORankingPelaQuantidadeVendida() {
        registrarMovimentoDeOutubro();

        List<ProdutoVendido> ranking = agregacoes.produtosMaisVendidos(OUTUBRO, null,
                CriterioDeRanking.QUANTIDADE, 10);

        assertEquals(List.of(
                new ProdutoVendido(1, "guarana", "Guaraná 2L", "Bebida", 4, new BigDecimal("48.00")),
                new ProdutoVendido(2, "calabresa", "Calabresa", "Pizza", 3, new BigDecimal("135.00")),
                new ProdutoVendido(3, "mussarela", "Mussarela", "Pizza", 1, new BigDecimal("40.00"))), ranking);
    }

    @Test
    void deveOrdenarORankingPeloFaturamentoELimitarOResultado() {
        registrarMovimentoDeOutubro();

        List<ProdutoVendido> ranking = agregacoes.produtosMaisVendidos(OUTUBRO, null,
                CriterioDeRanking.FATURAMENTO, 2);

        assertEquals(List.of("calabresa", "guarana"), ranking.stream().map(ProdutoVendido::produtoId).toList());
    }

    @Test
    void deveFiltrarORankingPelaCategoria() {
        registrarMovimentoDeOutubro();

        List<ProdutoVendido> pizzas = agregacoes.produtosMaisVendidos(OUTUBRO, "Pizza",
                CriterioDeRanking.QUANTIDADE, 10);

        assertEquals(List.of("calabresa", "mussarela"), pizzas.stream().map(ProdutoVendido::produtoId).toList());
    }

    @Test
    void deveExibirONomeMaisRecenteDoProdutoNoRanking() {
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 2, 19, 0))
                .com(item("calabresa", "Calabresa Antiga", "Pizza", "40.00", 1)));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 9, 19, 0))
                .com(item("calabresa", "Calabresa Especial", "Pizza", "48.00", 1)));

        List<ProdutoVendido> ranking = agregacoes.produtosMaisVendidos(OUTUBRO, null,
                CriterioDeRanking.QUANTIDADE, 10);

        assertEquals("Calabresa Especial", ranking.get(0).nome());
        assertEquals(new BigDecimal("88.00"), ranking.get(0).faturamento());
    }

    @Test
    void deveIdentificarItensAntigosSemCategoria() {
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 2, 19, 0))
                .com(item("antigo", "Produto Antigo", null, "30.00", 1)));

        List<ProdutoVendido> ranking = agregacoes.produtosMaisVendidos(OUTUBRO, null,
                CriterioDeRanking.QUANTIDADE, 10);

        assertEquals("Sem categoria", ranking.get(0).categoria());
    }

    @Test
    void deveCalcularOsTemposMediosPeloHistoricoDeStatus() {
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 2, 19, 0)).com(calabresa(1)).preparadoEm(20).atendidoEm(40));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 3, 19, 0)).com(calabresa(1)).preparadoEm(10).atendidoEm(30));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 4, 19, 0)).com(calabresa(1)).preparadoEm(30).atendidoEm(50));

        TemposMedios tempos = agregacoes.temposMediosDosFinalizados(OUTUBRO);

        assertEquals(20L, tempos.preparoEmMinutos());
        assertEquals(40L, tempos.atendimentoEmMinutos());
    }

    @Test
    void deveIgnorarPedidosSemHistoricoNosTemposMedios() {
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 2, 19, 0)).com(calabresa(1)).preparadoEm(16).atendidoEm(36));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 3, 19, 0)).com(calabresa(1)).semHistorico());

        TemposMedios tempos = agregacoes.temposMediosDosFinalizados(OUTUBRO);

        assertEquals(16L, tempos.preparoEmMinutos());
        assertEquals(36L, tempos.atendimentoEmMinutos());
    }

    @Test
    void deveDevolverTemposVaziosQuandoNaoHaPedidosFinalizados() {
        TemposMedios tempos = agregacoes.temposMediosDosFinalizados(OUTUBRO);

        assertNull(tempos.preparoEmMinutos());
        assertNull(tempos.atendimentoEmMinutos());
    }

    @Test
    void deveListarAsCategoriasDoCatalogoEDosPedidosSemRepetir() {
        Produto esfiha = new Produto();
        esfiha.setNome("Esfiha de Carne");
        esfiha.setTipo("Esfiha");
        esfiha.setPreco(8.0);
        esfiha.setAtivo(true);
        produtoRepository.save(esfiha);
        registrarMovimentoDeOutubro();

        List<String> categorias = agregacoes.categoriasConhecidas();

        assertEquals(List.of("Bebida", "Esfiha", "Pizza"), categorias);
        assertTrue(categorias.stream().noneMatch(String::isBlank));
    }

    private void registrarMovimentoDeOutubro() {
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 5, 19, 10)).para(TipoSaida.ENTREGA)
                .com(calabresa(2)).com(guarana(1)).comDesconto("10.20"));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 5, 23, 30)).para(TipoSaida.RETIRADA)
                .com(calabresa(1)));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 7, 20, 0)).para(TipoSaida.ENTREGA)
                .com(item("mussarela", "Mussarela", "Pizza", "40.00", 1)).com(guarana(3)));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 6, 19, 0)).com(calabresa(1))
                .canceladoPor(MotivoCancelamento.CLIENTE_DESISTIU));
        salvar(entrandoEm(LocalDateTime.of(2026, 10, 7, 21, 0)).com(calabresa(1))
                .emStatus(StatusPedido.RECEBIDO));
        salvar(entrandoEm(LocalDateTime.of(2026, 9, 20, 19, 0)).com(calabresa(5)));
        salvar(entrandoEm(LocalDateTime.of(2026, 9, 1, 19, 0)).com(calabresa(1))
                .emStatus(StatusPedido.EM_PREPARACAO));
    }

    private void salvar(PedidoDeExemplo exemplo) {
        pedidoRepository.save(exemplo.montar());
    }

    private static ItemPedido calabresa(int quantidade) {
        return item("calabresa", "Calabresa", "Pizza", "45.00", quantidade);
    }

    private static ItemPedido guarana(int quantidade) {
        return item("guarana", "Guaraná 2L", "Bebida", "12.00", quantidade);
    }

    private static FiltroDoPainel filtro(TipoSaida tipoSaida) {
        return new FiltroDoPainel(new PeriodoDoPainel(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
                tipoSaida);
    }

    private Map<String, VendaAgrupada> porChave(List<VendaAgrupada> vendas) {
        return vendas.stream().collect(Collectors.toMap(VendaAgrupada::chave, venda -> venda));
    }
}
