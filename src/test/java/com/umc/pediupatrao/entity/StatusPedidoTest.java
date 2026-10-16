package com.umc.pediupatrao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class StatusPedidoTest {

    @ParameterizedTest(name = "{0} em pedido de {1} segue para {2}")
    @CsvSource({
        "RECEBIDO, ENTREGA, EM_PREPARACAO",
        "RECEBIDO, RETIRADA, EM_PREPARACAO",
        "EM_PREPARACAO, ENTREGA, PRONTO",
        "EM_PREPARACAO, RETIRADA, PRONTO",
        "PRONTO, ENTREGA, SAIU_PARA_ENTREGA",
        "PRONTO, RETIRADA, RETIRADO",
        "SAIU_PARA_ENTREGA, ENTREGA, FINALIZADO",
        "RETIRADO, RETIRADA, FINALIZADO"
    })
    void deveSeguirOFluxoObrigatorio(StatusPedido atual, TipoSaida tipoSaida, StatusPedido esperado) {
        assertEquals(Optional.of(esperado), atual.proximo(tipoSaida));
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"FINALIZADO", "CANCELADO"})
    void naoDeveTerProximoPassoDepoisDeEstadoFinal(StatusPedido finalizado) {
        for (TipoSaida tipoSaida : TipoSaida.values()) {
            assertTrue(finalizado.proximo(tipoSaida).isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "RETIRADO"})
    void deveRegistrarSaidaSomenteNosEstadosDeSaida(StatusPedido status) {
        assertTrue(status.registraSaida());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"RECEBIDO", "EM_PREPARACAO", "PRONTO", "FINALIZADO", "CANCELADO"})
    void naoDeveRegistrarSaidaNosDemaisEstados(StatusPedido status) {
        assertFalse(status.registraSaida());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"RECEBIDO", "EM_PREPARACAO", "PRONTO"})
    void devePermitirCancelamentoEDescontoApenasAntesDaSaida(StatusPedido status) {
        assertTrue(status.antesDaSaida());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"})
    void naoDevePermitirCancelamentoEDescontoDepoisDaSaida(StatusPedido status) {
        assertFalse(status.antesDaSaida());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"FINALIZADO", "CANCELADO"})
    void deveIdentificarEstadosFinais(StatusPedido status) {
        assertTrue(status.ehFinal());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"FINALIZADO", "CANCELADO"}, mode = EnumSource.Mode.EXCLUDE)
    void naoDeveTratarEstadosIntermediariosComoFinais(StatusPedido status) {
        assertFalse(status.ehFinal());
    }
}
