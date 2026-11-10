package com.umc.pediupatrao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PedidoTest {

    private static final LocalDateTime ENTRADA = LocalDateTime.of(2026, 10, 2, 19, 10);

    @Test
    void deveTratarPedidoSemHistoricoComoListaVazia() {
        Pedido pedidoAntigo = new Pedido();

        assertTrue(pedidoAntigo.getHistoricoDeStatus().isEmpty());
    }

    @Test
    void deveMudarOStatusERegistrarAPassagem() {
        Pedido pedido = new Pedido();

        pedido.passarPara(StatusPedido.RECEBIDO, ENTRADA, "atendente01");

        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        assertEquals(List.of(new PassagemDeStatus(StatusPedido.RECEBIDO, ENTRADA, "atendente01")),
                pedido.getHistoricoDeStatus());
    }

    @Test
    void deveAcumularAsPassagensNaOrdemEmQueAconteceram() {
        Pedido pedido = new Pedido();

        pedido.passarPara(StatusPedido.RECEBIDO, ENTRADA, "atendente01");
        pedido.passarPara(StatusPedido.EM_PREPARACAO, ENTRADA.plusMinutes(5), "atendente01");
        pedido.passarPara(StatusPedido.PRONTO, ENTRADA.plusMinutes(32), "gerente01");

        List<StatusPedido> sequencia = pedido.getHistoricoDeStatus().stream()
                .map(PassagemDeStatus::status)
                .toList();
        assertEquals(List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO), sequencia);
        assertEquals(StatusPedido.PRONTO, pedido.getStatus());
    }

    @Test
    void deveContinuarOHistoricoDeUmaListaImutavelVindaDoBanco() {
        Pedido pedido = new Pedido();
        pedido.setHistoricoDeStatus(List.of(new PassagemDeStatus(StatusPedido.RECEBIDO, ENTRADA, "atendente01")));

        pedido.passarPara(StatusPedido.EM_PREPARACAO, ENTRADA.plusMinutes(3), "atendente01");

        assertEquals(2, pedido.getHistoricoDeStatus().size());
    }
}
