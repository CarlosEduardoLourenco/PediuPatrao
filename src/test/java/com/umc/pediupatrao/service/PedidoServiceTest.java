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

import com.umc.pediupatrao.dto.AvancoRequisicao;
import com.umc.pediupatrao.dto.ItemRequisicao;
import com.umc.pediupatrao.dto.PedidoRequisicao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

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
    }

    @Test
    void deveCriarPedidoRecebidoComValoresCalculadosNoServidor() {
        prepararClienteEProdutosValidos();
        when(usuarioAutenticado.nome()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido pedido = servico.criar(new PedidoRequisicao("c1", TipoSaida.ENTREGA, List.of(
                new ItemRequisicao("p1", 2),
                new ItemRequisicao("p2", 3))));

        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        assertEquals(new BigDecimal("105.50"), pedido.getSubtotal());
        assertEquals(new BigDecimal("105.50"), pedido.getValorTotal());
        assertEquals(new BigDecimal("0.00"), pedido.getValorDesconto());
        assertEquals(new BigDecimal("0.00"), pedido.getDescontoPercentual());
        assertEquals(2, pedido.getItens().size());
        assertEquals("Maria", pedido.getClienteNome());
        assertEquals(TipoSaida.ENTREGA, pedido.getTipoSaida());
    }

    @Test
    void deveRegistrarResponsavelEDataHoraDeEntrada() {
        prepararClienteEProdutosValidos();
        when(usuarioAutenticado.nome()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido pedido = servico.criar(new PedidoRequisicao("c1", TipoSaida.RETIRADA,
                List.of(new ItemRequisicao("p1", 1))));

        assertEquals("atendente01", pedido.getUsuarioEntrada());
        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 35), pedido.getDataHoraEntrada());
        assertNull(pedido.getSaida());
    }

    @Test
    void naoDeveCriarPedidoParaClienteInexistente() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.empty());
        PedidoRequisicao requisicao = new PedidoRequisicao("c1", TipoSaida.ENTREGA,
                List.of(new ItemRequisicao("p1", 1)));

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.criar(requisicao));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarPedidoComProdutoInexistente() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
        when(produtoRepository.findById("p1")).thenReturn(Optional.empty());
        PedidoRequisicao requisicao = new PedidoRequisicao("c1", TipoSaida.ENTREGA,
                List.of(new ItemRequisicao("p1", 1)));

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.criar(requisicao));
    }

    @Test
    void naoDeveCriarPedidoComProdutoInativo() {
        Produto inativo = produto("p1", "Calabresa", 40.0);
        inativo.setAtivo(false);
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(inativo));
        PedidoRequisicao requisicao = new PedidoRequisicao("c1", TipoSaida.ENTREGA,
                List.of(new ItemRequisicao("p1", 1)));

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarPedidoSemItens() {
        PedidoRequisicao requisicao = new PedidoRequisicao("c1", TipoSaida.ENTREGA, List.of());

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
    }

    @Test
    void naoDeveCriarPedidoComQuantidadeMenorQueUm() {
        PedidoRequisicao requisicao = new PedidoRequisicao("c1", TipoSaida.ENTREGA,
                List.of(new ItemRequisicao("p1", 0)));

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
    }

    @Test
    void naoDeveCriarPedidoSemTipoDeSaida() {
        PedidoRequisicao requisicao = new PedidoRequisicao("c1", null,
                List.of(new ItemRequisicao("p1", 1)));

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
    }

    @Test
    void naoDeveCriarPedidoSemCliente() {
        PedidoRequisicao requisicao = new PedidoRequisicao(" ", TipoSaida.ENTREGA,
                List.of(new ItemRequisicao("p1", 1)));

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
    }

    @Test
    void deveAvancarPedidoParaOProximoStatus() {
        Pedido pedido = pedidoEm(StatusPedido.RECEBIDO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido avancado = servico.avancar("ped1", AvancoRequisicao.semDestino());

        assertEquals(StatusPedido.EM_PREPARACAO, avancado.getStatus());
        assertNull(avancado.getSaida());
    }

    @Test
    void deveRegistrarSaidaComResponsavelEDataHoraAoSairParaEntrega() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(usuarioAutenticado.nome()).thenReturn("gerente01");
        when(entregadorRepository.findById("e1")).thenReturn(Optional.of(entregador(true)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido avancado = servico.avancar("ped1", AvancoRequisicao.paraEntrega("e1"));

        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, avancado.getStatus());
        assertEquals("gerente01", avancado.getSaida().usuario());
        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 35), avancado.getSaida().dataHora());
        assertEquals("e1", avancado.getSaida().entregadorId());
        assertEquals("Carlos", avancado.getSaida().entregadorNome());
    }

    @Test
    void naoDeveSairParaEntregaSemInformarOEntregador() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));

        assertThrows(DadosInvalidosException.class, () -> servico.avancar("ped1", AvancoRequisicao.semDestino()));
        assertThrows(DadosInvalidosException.class, () -> servico.avancar("ped1", AvancoRequisicao.paraEntrega(" ")));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveSairParaEntregaComEntregadorInexistente() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById("e1")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> servico.avancar("ped1", AvancoRequisicao.paraEntrega("e1")));
    }

    @Test
    void naoDeveSairParaEntregaComEntregadorInativo() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById("e1")).thenReturn(Optional.of(entregador(false)));

        assertThrows(DadosInvalidosException.class,
                () -> servico.avancar("ped1", AvancoRequisicao.paraEntrega("e1")));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveExigirEntregadorParaRetiradaNoBalcao() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.RETIRADA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(usuarioAutenticado.nome()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido avancado = servico.avancar("ped1", AvancoRequisicao.semDestino());

        assertNull(avancado.getSaida().entregadorId());
    }

    private Entregador entregador(boolean ativo) {
        Entregador entregador = new Entregador();
        entregador.setId("e1");
        entregador.setNome("Carlos");
        entregador.setAtivo(ativo);
        return entregador;
    }

    @Test
    void deveRegistrarSaidaAoSerRetirado() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.RETIRADA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(usuarioAutenticado.nome()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido avancado = servico.avancar("ped1", AvancoRequisicao.semDestino());

        assertEquals(StatusPedido.RETIRADO, avancado.getStatus());
        assertEquals("atendente01", avancado.getSaida().usuario());
    }

    @Test
    void deveAuditarACriacaoDoPedido() {
        prepararClienteEProdutosValidos();
        when(usuarioAutenticado.nome()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido pedido = servico.criar(new PedidoRequisicao("c1", TipoSaida.ENTREGA,
                List.of(new ItemRequisicao("p1", 1))));

        verify(pedidoAuditor).criacao(pedido);
    }

    @Test
    void deveAuditarMudancaDeStatusComOStatusAnterior() {
        Pedido pedido = pedidoEm(StatusPedido.EM_PREPARACAO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pedido avancado = servico.avancar("ped1", AvancoRequisicao.semDestino());

        verify(pedidoAuditor).mudancaDeStatus(avancado, StatusPedido.EM_PREPARACAO);
    }

    @Test
    void naoDeveAuditarTransicaoRejeitada() {
        Pedido pedido = pedidoEm(StatusPedido.EM_PREPARACAO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        AvancoRequisicao pulo = AvancoRequisicao.para(StatusPedido.FINALIZADO);

        assertThrows(RegraNegocioException.class, () -> servico.avancar("ped1", pulo));

        verifyNoInteractions(pedidoAuditor);
    }

    @Test
    void naoDeveFinalizarPedidoEmPreparacao() {
        Pedido pedido = pedidoEm(StatusPedido.EM_PREPARACAO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        AvancoRequisicao pulo = AvancoRequisicao.para(StatusPedido.FINALIZADO);

        assertThrows(RegraNegocioException.class, () -> servico.avancar("ped1", pulo));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveVoltarParaStatusAnterior() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        AvancoRequisicao retrocesso = AvancoRequisicao.para(StatusPedido.RECEBIDO);

        assertThrows(RegraNegocioException.class, () -> servico.avancar("ped1", retrocesso));
    }

    @Test
    void naoDeveCancelarPeloCaminhoDeAvanco() {
        Pedido pedido = pedidoEm(StatusPedido.RECEBIDO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        AvancoRequisicao cancelamento = AvancoRequisicao.para(StatusPedido.CANCELADO);

        assertThrows(RegraNegocioException.class, () -> servico.avancar("ped1", cancelamento));
    }

    @Test
    void naoDeveAlterarPedidoCancelado() {
        Pedido pedido = pedidoEm(StatusPedido.CANCELADO, TipoSaida.ENTREGA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> servico.avancar("ped1", AvancoRequisicao.semDestino()));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoDeveAlterarPedidoFinalizado() {
        Pedido pedido = pedidoEm(StatusPedido.FINALIZADO, TipoSaida.RETIRADA);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> servico.avancar("ped1", AvancoRequisicao.semDestino()));
    }

    @Test
    void deveResponderNaoEncontradoAoAvancarPedidoInexistente() {
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> servico.avancar("ped1", AvancoRequisicao.semDestino()));
    }

    private Pedido pedidoEm(StatusPedido status, TipoSaida tipoSaida) {
        Pedido pedido = new Pedido();
        pedido.setId("ped1");
        pedido.setStatus(status);
        pedido.setTipoSaida(tipoSaida);
        return pedido;
    }

    private void prepararClienteEProdutosValidos() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto("p1", "Pizza", 40.0)));
        lenient().when(produtoRepository.findById("p2"))
                .thenReturn(Optional.of(produto("p2", "Refrigerante", 8.5)));
    }

    private Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        return cliente;
    }

    private Produto produto(String id, String nome, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
