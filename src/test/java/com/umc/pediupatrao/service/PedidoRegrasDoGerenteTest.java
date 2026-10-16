package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.CancelamentoRequisicao;
import com.umc.pediupatrao.dto.DescontoRequisicao;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoRegrasDoGerenteTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO = Clock.fixed(Instant.parse("2026-09-23T17:35:00Z"), FUSO);

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private EntregadorRepository entregadorRepository;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    @Mock
    private PedidoAuditor pedidoAuditor;

    private PedidoService servico;

    @BeforeEach
    void preparar() {
        servico = new PedidoService(pedidoRepository, clienteRepository, produtoRepository,
                entregadorRepository, usuarioAutenticado, pedidoAuditor, RELOGIO_FIXO);
        lenient().when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        lenient().when(usuarioAutenticado.nome()).thenReturn("gerente01");
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"RECEBIDO", "EM_PREPARACAO", "PRONTO"})
    void deveCancelarPedidoQueAindaNaoSaiu(StatusPedido status) {
        liberarPedido(pedidoEm(status));

        Pedido cancelado = servico.cancelar("ped1",
                new CancelamentoRequisicao(MotivoCancelamento.CLIENTE_DESISTIU, null));

        assertEquals(StatusPedido.CANCELADO, cancelado.getStatus());
        assertEquals("gerente01", cancelado.getCancelamento().usuario());
        assertEquals(MotivoCancelamento.CLIENTE_DESISTIU, cancelado.getCancelamento().motivo());
        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 35), cancelado.getCancelamento().dataHora());
    }

    @Test
    void devePreservarItensEValoresAoCancelar() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO);
        liberarPedido(pedido);

        Pedido cancelado = servico.cancelar("ped1",
                new CancelamentoRequisicao(MotivoCancelamento.ITEM_INDISPONIVEL, ""));

        assertEquals(1, cancelado.getItens().size());
        assertEquals(new BigDecimal("100.00"), cancelado.getValorTotal());
        assertEquals("Item indisponível", cancelado.getCancelamento().justificativa());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"})
    void naoDeveCancelarPedidoQueJaSaiuOuTerminou(StatusPedido status) {
        liberarPedido(pedidoEm(status));
        CancelamentoRequisicao requisicao = new CancelamentoRequisicao(MotivoCancelamento.CLIENTE_DESISTIU, null);

        assertThrows(RegraNegocioException.class, () -> servico.cancelar("ped1", requisicao));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveCancelarSemMotivo() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));

        assertThrows(DadosInvalidosException.class,
                () -> servico.cancelar("ped1", new CancelamentoRequisicao(null, null)));
    }

    @Test
    void deveExigirObservacaoCompletaParaMotivoOutro() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));
        CancelamentoRequisicao curta = new CancelamentoRequisicao(MotivoCancelamento.OUTRO, "curto");

        assertThrows(DadosInvalidosException.class, () -> servico.cancelar("ped1", curta));
    }

    @Test
    void deveRegistrarObservacaoNaJustificativaDoMotivoOutro() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));

        Pedido cancelado = servico.cancelar("ped1",
                new CancelamentoRequisicao(MotivoCancelamento.OUTRO, "  cliente mudou de endereco  "));

        assertEquals("Outro motivo: cliente mudou de endereco", cancelado.getCancelamento().justificativa());
    }

    @Test
    void naoDeveGuardarCancelamentoNoPedidoNaoCancelado() {
        assertNull(pedidoEm(StatusPedido.RECEBIDO).getCancelamento());
    }

    @Test
    void deveAuditarCancelamentoComOStatusAnterior() {
        liberarPedido(pedidoEm(StatusPedido.PRONTO));

        Pedido cancelado = servico.cancelar("ped1",
                new CancelamentoRequisicao(MotivoCancelamento.CLIENTE_DESISTIU, null));

        verify(pedidoAuditor).cancelamento(cancelado, StatusPedido.PRONTO);
    }

    @Test
    void deveAuditarDescontoComPercentualETotalAnteriores() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));

        Pedido pedido = servico.aplicarDesconto("ped1", new DescontoRequisicao(new BigDecimal("10")));

        verify(pedidoAuditor).desconto(pedido, new BigDecimal("0.00"), new BigDecimal("100.00"));
    }

    @Test
    void naoDeveAuditarQuandoACancelamentoForRejeitado() {
        liberarPedido(pedidoEm(StatusPedido.FINALIZADO));
        CancelamentoRequisicao requisicao = new CancelamentoRequisicao(MotivoCancelamento.CLIENTE_DESISTIU, null);

        assertThrows(RegraNegocioException.class, () -> servico.cancelar("ped1", requisicao));

        verifyNoInteractions(pedidoAuditor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "15", "20", "20.00", "7.5"})
    void deveAplicarDescontoDentroDoLimite(String percentual) {
        liberarPedido(pedidoEm(StatusPedido.EM_PREPARACAO));

        Pedido pedido = servico.aplicarDesconto("ped1", new DescontoRequisicao(new BigDecimal(percentual)));

        BigDecimal esperadoDesconto = new BigDecimal("100.00").multiply(new BigDecimal(percentual))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        assertEquals(esperadoDesconto, pedido.getValorDesconto());
        assertEquals(new BigDecimal("100.00").subtract(esperadoDesconto), pedido.getValorTotal());
    }

    @Test
    void deveCalcularTotalComDescontoDeQuinzePorCento() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));

        Pedido pedido = servico.aplicarDesconto("ped1", new DescontoRequisicao(new BigDecimal("15")));

        assertEquals(new BigDecimal("15.00"), pedido.getValorDesconto());
        assertEquals(new BigDecimal("85.00"), pedido.getValorTotal());
        assertEquals(new BigDecimal("15.00"), pedido.getDescontoPercentual());
    }

    @Test
    void deveSubstituirODescontoAnteriorEPermitirRemover() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));
        servico.aplicarDesconto("ped1", new DescontoRequisicao(new BigDecimal("10")));

        Pedido semDesconto = servico.aplicarDesconto("ped1", new DescontoRequisicao(BigDecimal.ZERO));

        assertEquals(new BigDecimal("0.00"), semDesconto.getValorDesconto());
        assertEquals(new BigDecimal("100.00"), semDesconto.getValorTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"20.01", "21", "100", "-1"})
    void naoDeveAplicarDescontoForaDoLimite(String percentual) {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));
        DescontoRequisicao requisicao = new DescontoRequisicao(new BigDecimal(percentual));

        assertThrows(DadosInvalidosException.class, () -> servico.aplicarDesconto("ped1", requisicao));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveAplicarDescontoSemPercentual() {
        liberarPedido(pedidoEm(StatusPedido.RECEBIDO));

        assertThrows(DadosInvalidosException.class,
                () -> servico.aplicarDesconto("ped1", new DescontoRequisicao(null)));
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"})
    void naoDeveAplicarDescontoDepoisDaSaidaOuEmPedidoEncerrado(StatusPedido status) {
        liberarPedido(pedidoEm(status));
        DescontoRequisicao requisicao = new DescontoRequisicao(new BigDecimal("10"));

        assertThrows(RegraNegocioException.class, () -> servico.aplicarDesconto("ped1", requisicao));
        verify(pedidoRepository, never()).save(any());
    }

    private void liberarPedido(Pedido pedido) {
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
    }

    private Pedido pedidoEm(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("ped1");
        pedido.setStatus(status);
        pedido.setTipoSaida(TipoSaida.ENTREGA);
        pedido.setItens(List.of(new ItemPedido("p1", "Pizza", new BigDecimal("50.00"), 2, new BigDecimal("100.00"))));
        pedido.setSubtotal(new BigDecimal("100.00"));
        pedido.setDescontoPercentual(new BigDecimal("0.00"));
        pedido.setValorDesconto(new BigDecimal("0.00"));
        pedido.setValorTotal(new BigDecimal("100.00"));
        return pedido;
    }
}
