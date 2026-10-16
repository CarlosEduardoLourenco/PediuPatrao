package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.AvancoRequisicao;
import com.umc.pediupatrao.dto.CancelamentoRequisicao;
import com.umc.pediupatrao.dto.DescontoRequisicao;
import com.umc.pediupatrao.dto.ItemRequisicao;
import com.umc.pediupatrao.dto.PedidoRequisicao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.RegistroDeCancelamento;
import com.umc.pediupatrao.entity.RegistroDeSaida;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private static final BigDecimal DESCONTO_MAXIMO_PERCENTUAL = BigDecimal.valueOf(20);
    private static final int TAMANHO_MINIMO_OBSERVACAO = 10;

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final EntregadorRepository entregadorRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final PedidoAuditor pedidoAuditor;
    private final Clock relogio;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, EntregadorRepository entregadorRepository,
            UsuarioAutenticado usuarioAutenticado, PedidoAuditor pedidoAuditor, Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.entregadorRepository = entregadorRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.pedidoAuditor = pedidoAuditor;
        this.relogio = relogio;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public Pedido buscar(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criar(PedidoRequisicao requisicao) {
        exigirDadosDoPedido(requisicao);
        Cliente cliente = buscarCliente(requisicao.clienteId());
        List<ItemPedido> itens = requisicao.itens().stream().map(this::montarItem).toList();

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoSaida(requisicao.tipoSaida());
        pedido.setStatus(StatusPedido.RECEBIDO);
        definirValoresIniciais(pedido, itens);
        pedido.setDataHoraEntrada(LocalDateTime.now(relogio));
        pedido.setUsuarioEntrada(usuarioAutenticado.nome());

        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditor.criacao(salvo);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido avancar(String id, AvancoRequisicao requisicao) {
        Pedido pedido = buscar(id);
        StatusPedido statusAnterior = pedido.getStatus();
        StatusPedido proximo = proximoStatusValido(pedido);
        exigirDestinoCompativel(statusAnterior, proximo, requisicao.destino());

        RegistroDeSaida saida = proximo.registraSaida() ? registrarSaida(proximo, requisicao) : null;

        pedido.setStatus(proximo);
        pedido.setSaida(saida != null ? saida : pedido.getSaida());
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditor.mudancaDeStatus(salvo, statusAnterior);
        return salvo;
    }

    private RegistroDeSaida registrarSaida(StatusPedido destino, AvancoRequisicao requisicao) {
        Entregador entregador = destino == StatusPedido.SAIU_PARA_ENTREGA
                ? buscarEntregadorAtivo(requisicao.entregadorId())
                : null;
        return new RegistroDeSaida(LocalDateTime.now(relogio), usuarioAutenticado.nome(),
                entregador == null ? null : entregador.getId(),
                entregador == null ? null : entregador.getNome());
    }

    private Entregador buscarEntregadorAtivo(String entregadorId) {
        if (entregadorId == null || entregadorId.isBlank()) {
            throw new DadosInvalidosException("Informe o entregador responsável pela entrega");
        }
        Entregador entregador = entregadorRepository.findById(entregadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador", entregadorId));
        if (Boolean.FALSE.equals(entregador.getAtivo())) {
            throw new DadosInvalidosException("Entregador inativo não pode receber entregas");
        }
        return entregador;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, CancelamentoRequisicao requisicao) {
        Pedido pedido = buscar(id);
        exigirPedidoAntesDaSaida(pedido, "O cancelamento");
        exigirMotivoValido(requisicao);
        StatusPedido statusAnterior = pedido.getStatus();

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCancelamento(new RegistroDeCancelamento(requisicao.motivo(), observacaoLimpa(requisicao),
                usuarioAutenticado.nome(), LocalDateTime.now(relogio)));
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditor.cancelamento(salvo, statusAnterior);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, DescontoRequisicao requisicao) {
        Pedido pedido = buscar(id);
        exigirPedidoAntesDaSaida(pedido, "O desconto");
        exigirPercentualPermitido(requisicao.percentual());
        BigDecimal percentualAnterior = pedido.getDescontoPercentual();
        BigDecimal totalAnterior = pedido.getValorTotal();

        BigDecimal valorDesconto = CalculadoraDePedido.calcularDesconto(
                pedido.getSubtotal(), requisicao.percentual());
        pedido.setDescontoPercentual(requisicao.percentual().setScale(2, RoundingMode.HALF_UP));
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(CalculadoraDePedido.calcularTotal(pedido.getSubtotal(), valorDesconto));
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditor.desconto(salvo, percentualAnterior, totalAnterior);
        return salvo;
    }

    private void exigirPedidoAntesDaSaida(Pedido pedido, String operacao) {
        if (pedido.getStatus().ehFinal()) {
            throw new RegraNegocioException(mensagemDePedidoImutavel(pedido));
        }
        if (!pedido.getStatus().antesDaSaida()) {
            throw new RegraNegocioException(operacao + " só é permitido antes da saída do pedido");
        }
    }

    private void exigirMotivoValido(CancelamentoRequisicao requisicao) {
        if (requisicao.motivo() == null) {
            throw new DadosInvalidosException("Informe o motivo do cancelamento");
        }
        boolean observacaoInsuficiente = observacaoLimpa(requisicao).length() < TAMANHO_MINIMO_OBSERVACAO;
        if (requisicao.motivo().exigeObservacao() && observacaoInsuficiente) {
            throw new DadosInvalidosException("Descreva o motivo com ao menos "
                    + TAMANHO_MINIMO_OBSERVACAO + " caracteres");
        }
    }

    private void exigirPercentualPermitido(BigDecimal percentual) {
        if (percentual == null) {
            throw new DadosInvalidosException("Informe o percentual de desconto");
        }
        if (percentual.signum() < 0 || percentual.compareTo(DESCONTO_MAXIMO_PERCENTUAL) > 0) {
            throw new DadosInvalidosException("O desconto deve ficar entre 0% e "
                    + DESCONTO_MAXIMO_PERCENTUAL.toPlainString() + "%");
        }
    }

    private String observacaoLimpa(CancelamentoRequisicao requisicao) {
        return requisicao.observacao() == null ? "" : requisicao.observacao().trim();
    }

    private String mensagemDePedidoImutavel(Pedido pedido) {
        return "Pedido " + pedido.getStatus() + " não pode mais ser alterado";
    }

    private StatusPedido proximoStatusValido(Pedido pedido) {
        return pedido.getStatus().proximo(pedido.getTipoSaida())
                .orElseThrow(() -> new RegraNegocioException(mensagemDePedidoImutavel(pedido)));
    }

    private void exigirDestinoCompativel(StatusPedido atual, StatusPedido proximo, StatusPedido destino) {
        if (destino != null && destino != proximo) {
            throw new RegraNegocioException("Transição inválida: de " + atual
                    + " só é possível ir para " + proximo + ", não para " + destino);
        }
    }

    private void definirValoresIniciais(Pedido pedido, List<ItemPedido> itens) {
        BigDecimal subtotal = CalculadoraDePedido.somarSubtotais(itens);
        pedido.setItens(itens);
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(CalculadoraDePedido.zero());
        pedido.setValorDesconto(CalculadoraDePedido.zero());
        pedido.setValorTotal(subtotal);
    }

    private Cliente buscarCliente(String clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
        if (!cliente.estaAtivo()) {
            throw new DadosInvalidosException("Cliente inativo não pode fazer novos pedidos");
        }
        return cliente;
    }

    private ItemPedido montarItem(ItemRequisicao item) {
        Produto produto = produtoRepository.findById(item.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", item.produtoId()));
        if (!Boolean.TRUE.equals(produto.getAtivo()) || produto.getPreco() == null) {
            throw new DadosInvalidosException("Produto indisponível: " + produto.getNome());
        }
        return CalculadoraDePedido.montarItem(produto, item.quantidade());
    }

    private void exigirDadosDoPedido(PedidoRequisicao requisicao) {
        if (requisicao.clienteId() == null || requisicao.clienteId().isBlank()) {
            throw new DadosInvalidosException("Informe o cliente do pedido");
        }
        if (requisicao.tipoSaida() == null) {
            throw new DadosInvalidosException("Informe se o pedido é para entrega ou retirada");
        }
        if (requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw new DadosInvalidosException("O pedido precisa de ao menos um item");
        }
        requisicao.itens().forEach(this::exigirItemValido);
    }

    private void exigirItemValido(ItemRequisicao item) {
        if (item == null || item.produtoId() == null || item.produtoId().isBlank()) {
            throw new DadosInvalidosException("Informe o produto de cada item");
        }
        if (item.quantidade() < 1) {
            throw new DadosInvalidosException("A quantidade de cada item deve ser de ao menos 1");
        }
    }
}
