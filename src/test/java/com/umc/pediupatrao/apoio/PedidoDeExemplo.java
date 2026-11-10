package com.umc.pediupatrao.apoio;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.RegistroDeCancelamento;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PedidoDeExemplo {

    private static final String RESPONSAVEL = "atendente01";
    private static final int MINUTOS_NA_FILA = 5;
    private static final int MINUTOS_ENTRE_ETAPAS = 5;

    private final LocalDateTime entrada;
    private final List<ItemPedido> itens = new ArrayList<>();
    private StatusPedido status = StatusPedido.FINALIZADO;
    private TipoSaida tipoSaida = TipoSaida.ENTREGA;
    private BigDecimal desconto = new BigDecimal("0.00");
    private MotivoCancelamento motivo = MotivoCancelamento.CLIENTE_DESISTIU;
    private int minutosDePreparo = 20;
    private int minutosDeAtendimento = 45;
    private boolean registrarHistorico = true;

    private PedidoDeExemplo(LocalDateTime entrada) {
        this.entrada = entrada;
    }

    public static PedidoDeExemplo entrandoEm(LocalDateTime entrada) {
        return new PedidoDeExemplo(entrada);
    }

    public static ItemPedido item(String produtoId, String nome, String categoria, String preco, int quantidade) {
        BigDecimal precoUnitario = new BigDecimal(preco);
        return new ItemPedido(produtoId, nome, categoria, precoUnitario, quantidade,
                precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public PedidoDeExemplo com(ItemPedido item) {
        itens.add(item);
        return this;
    }

    public PedidoDeExemplo emStatus(StatusPedido novoStatus) {
        this.status = novoStatus;
        return this;
    }

    public PedidoDeExemplo canceladoPor(MotivoCancelamento motivoDoCancelamento) {
        this.status = StatusPedido.CANCELADO;
        this.motivo = motivoDoCancelamento;
        return this;
    }

    public PedidoDeExemplo para(TipoSaida saida) {
        this.tipoSaida = saida;
        return this;
    }

    public PedidoDeExemplo comDesconto(String valor) {
        this.desconto = new BigDecimal(valor);
        return this;
    }

    public PedidoDeExemplo preparadoEm(int minutos) {
        this.minutosDePreparo = minutos;
        return this;
    }

    public PedidoDeExemplo atendidoEm(int minutos) {
        this.minutosDeAtendimento = minutos;
        return this;
    }

    public PedidoDeExemplo semHistorico() {
        this.registrarHistorico = false;
        return this;
    }

    public Pedido montar() {
        BigDecimal subtotal = itens.stream().map(ItemPedido::subtotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
        Pedido pedido = new Pedido();
        pedido.setClienteId("cliente-exemplo");
        pedido.setClienteNome("Cliente Exemplo");
        pedido.setTipoSaida(tipoSaida);
        pedido.setItens(List.copyOf(itens));
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(new BigDecimal("0.00"));
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(subtotal.subtract(desconto));
        pedido.setDataHoraEntrada(entrada);
        pedido.setUsuarioEntrada(RESPONSAVEL);
        if (registrarHistorico) {
            registrarPassagens(pedido);
        } else {
            pedido.setStatus(status);
        }
        if (status == StatusPedido.CANCELADO) {
            pedido.setCancelamento(new RegistroDeCancelamento(motivo, null, "gerente01",
                    entrada.plusMinutes(MINUTOS_ENTRE_ETAPAS)));
        }
        return pedido;
    }

    private void registrarPassagens(Pedido pedido) {
        pedido.passarPara(StatusPedido.RECEBIDO, entrada, RESPONSAVEL);
        if (status == StatusPedido.CANCELADO) {
            pedido.passarPara(StatusPedido.CANCELADO, entrada.plusMinutes(MINUTOS_ENTRE_ETAPAS), "gerente01");
            return;
        }
        LocalDateTime momento = entrada;
        while (pedido.getStatus() != status) {
            StatusPedido proximo = pedido.getStatus().proximo(tipoSaida).orElseThrow();
            momento = momentoDa(proximo, momento);
            pedido.passarPara(proximo, momento, RESPONSAVEL);
        }
    }

    private LocalDateTime momentoDa(StatusPedido etapa, LocalDateTime anterior) {
        return switch (etapa) {
            case EM_PREPARACAO -> entrada.plusMinutes(MINUTOS_NA_FILA);
            case PRONTO -> entrada.plusMinutes(MINUTOS_NA_FILA + minutosDePreparo);
            case FINALIZADO -> entrada.plusMinutes(minutosDeAtendimento);
            default -> anterior.plusMinutes(1);
        };
    }
}
