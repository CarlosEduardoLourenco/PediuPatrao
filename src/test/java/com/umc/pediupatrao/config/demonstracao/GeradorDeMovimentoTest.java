package com.umc.pediupatrao.config.demonstracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.PassagemDeStatus;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GeradorDeMovimentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 9, 21, 30);
    private static final long SEMENTE = 42L;
    private static final BigDecimal DESCONTO_MAXIMO = BigDecimal.valueOf(20);

    private static List<Pedido> pedidos;

    @BeforeAll
    static void gerarMovimento() {
        pedidos = novoGerador(SEMENTE).gerar();
    }

    @Test
    void deveGerarSempreOMesmoMovimentoParaAMesmaSemente() {
        List<Pedido> repetidos = novoGerador(SEMENTE).gerar();

        assertEquals(pedidos.size(), repetidos.size());
        assertEquals(pedidos.get(0).getValorTotal(), repetidos.get(0).getValorTotal());
        assertEquals(pedidos.get(pedidos.size() - 1).getDataHoraEntrada(),
                repetidos.get(repetidos.size() - 1).getDataHoraEntrada());
    }

    @Test
    void deveGerarUmVolumeCompativelComTresMesesDeOperacao() {
        assertTrue(pedidos.size() > 900 && pedidos.size() < 1800, "pedidos gerados: " + pedidos.size());
    }

    @Test
    void deveManterTodasAsEntradasNosUltimosNoventaDiasESemDatasFuturas() {
        LocalDateTime inicio = AGORA.toLocalDate().minusDays(GeradorDeMovimento.DIAS_DE_HISTORICO - 1L).atStartOfDay();

        assertTrue(pedidos.stream().allMatch(pedido -> !pedido.getDataHoraEntrada().isBefore(inicio)
                && pedido.getDataHoraEntrada().isBefore(AGORA)));
    }

    @Test
    void deveConcentrarAsEntradasNoHorarioDeFuncionamento() {
        assertTrue(pedidos.stream().map(Pedido::getDataHoraEntrada)
                .allMatch(entrada -> entrada.getHour() >= 18 && entrada.getHour() <= 22));
    }

    @Test
    void deveOrdenarOsPedidosPelaEntrada() {
        assertTrue(IntStream.range(1, pedidos.size()).allMatch(indice ->
                !pedidos.get(indice).getDataHoraEntrada().isBefore(pedidos.get(indice - 1).getDataHoraEntrada())));
    }

    @Test
    void deveCalcularOsValoresComAsRegrasDoSistema() {
        for (Pedido pedido : pedidos) {
            BigDecimal somaDosItens = pedido.getItens().stream().map(ItemPedido::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, somaDosItens.compareTo(pedido.getSubtotal()));
            assertEquals(pedido.getSubtotal().subtract(pedido.getValorDesconto()), pedido.getValorTotal());
            assertTrue(pedido.getDescontoPercentual().compareTo(DESCONTO_MAXIMO) <= 0);
        }
    }

    @Test
    void deveIdentificarACategoriaDeTodosOsItens() {
        assertTrue(pedidos.stream().flatMap(pedido -> pedido.getItens().stream())
                .allMatch(item -> item.categoria() != null && !item.categoria().isBlank()));
    }

    @Test
    void deveRegistrarUmHistoricoCoerenteComOStatusAtual() {
        for (Pedido pedido : pedidos) {
            List<PassagemDeStatus> historico = pedido.getHistoricoDeStatus();
            assertEquals(new PassagemDeStatus(StatusPedido.RECEBIDO, pedido.getDataHoraEntrada(),
                    pedido.getUsuarioEntrada()), historico.get(0));
            assertEquals(pedido.getStatus(), historico.get(historico.size() - 1).status());
            assertTrue(IntStream.range(1, historico.size()).allMatch(indice ->
                    !historico.get(indice).dataHora().isBefore(historico.get(indice - 1).dataHora())));
            assertFalse(historico.get(historico.size() - 1).dataHora().isAfter(AGORA));
        }
    }

    @Test
    void deveDeixarEmAndamentoSomenteOsPedidosMaisRecentes() {
        int inicioDosRecentes = pedidos.size() - GeradorDeMovimento.PEDIDOS_EM_ANDAMENTO;

        assertTrue(pedidos.subList(inicioDosRecentes, pedidos.size()).stream()
                .noneMatch(pedido -> pedido.getStatus().ehFinal()));
        assertTrue(pedidos.subList(0, inicioDosRecentes).stream()
                .allMatch(pedido -> pedido.getStatus().ehFinal()));
    }

    @Test
    void deveIndicarOEntregadorSomenteNasEntregas() {
        pedidos.stream().filter(pedido -> pedido.getStatus() == StatusPedido.FINALIZADO).forEach(pedido -> {
            assertNotNull(pedido.getSaida());
            if (pedido.getTipoSaida() == TipoSaida.ENTREGA) {
                assertNotNull(pedido.getSaida().entregadorNome());
            } else {
                assertNull(pedido.getSaida().entregadorId());
            }
        });
    }

    @Test
    void deveCancelarUmaPequenaParteDosPedidosComMotivoValido() {
        List<Pedido> cancelados = pedidos.stream()
                .filter(pedido -> pedido.getStatus() == StatusPedido.CANCELADO).toList();

        assertFalse(cancelados.isEmpty());
        assertTrue(cancelados.size() < pedidos.size() * 0.15);
        cancelados.forEach(pedido -> {
            assertNotNull(pedido.getCancelamento().motivo());
            if (pedido.getCancelamento().motivo().exigeObservacao()) {
                assertTrue(pedido.getCancelamento().observacao().length() >= 10);
            }
        });
    }

    @Test
    void deveTerMaisMovimentoNoSabadoDoQueNaTerca() {
        Map<DayOfWeek, Long> porDia = pedidos.stream().collect(Collectors.groupingBy(
                pedido -> pedido.getDataHoraEntrada().getDayOfWeek(), Collectors.counting()));

        assertTrue(porDia.get(DayOfWeek.SATURDAY) > porDia.get(DayOfWeek.TUESDAY) * 2);
    }

    @Test
    void deveCrescerAoLongoDoPeriodo() {
        LocalDateTime meioDoPeriodo = AGORA.minusDays(GeradorDeMovimento.DIAS_DE_HISTORICO / 2);
        long primeiraMetade = pedidos.stream().filter(pedido -> pedido.getDataHoraEntrada().isBefore(meioDoPeriodo))
                .count();

        assertTrue(pedidos.size() - primeiraMetade > primeiraMetade);
    }

    private static GeradorDeMovimento novoGerador(long semente) {
        List<Produto> produtos = CatalogoDeDemonstracao.produtos();
        IntStream.range(0, produtos.size()).forEach(indice -> produtos.get(indice).setId("produto-" + indice));
        List<Cliente> clientes = CatalogoDeDemonstracao.clientes(AGORA.minusDays(90));
        IntStream.range(0, clientes.size()).forEach(indice -> clientes.get(indice).setId("cliente-" + indice));
        List<Entregador> entregadores = CatalogoDeDemonstracao.entregadores();
        IntStream.range(0, entregadores.size())
                .forEach(indice -> entregadores.get(indice).setId("entregador-" + indice));
        return new GeradorDeMovimento(semente, AGORA, CatalogoDeDemonstracao.opcoes(produtos), clientes,
                entregadores);
    }
}
