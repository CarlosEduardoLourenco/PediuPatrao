package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpurgoDeClientesTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO = Clock.fixed(Instant.parse("2027-09-23T17:35:00Z"), FUSO);
    private static final LocalDateTime AGORA = LocalDateTime.of(2027, 9, 23, 14, 35);

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteAuditor clienteAuditor;

    private ExpurgoDeClientes expurgo;

    @BeforeEach
    void preparar() {
        expurgo = new ExpurgoDeClientes(clienteRepository, clienteAuditor, RELOGIO_FIXO);
    }

    @Test
    void deveAuditarEExcluirOsClientesComPrazoVencido() {
        Cliente vencido = new Cliente();
        vencido.setId("c1");
        when(clienteRepository.findByAtivoFalseAndRetidoAteLessThanEqual(AGORA)).thenReturn(List.of(vencido));

        int total = expurgo.expurgarVencidos();

        assertEquals(1, total);
        InOrder ordem = inOrder(clienteAuditor, clienteRepository);
        ordem.verify(clienteAuditor).expurgo(vencido);
        ordem.verify(clienteRepository).delete(vencido);
    }

    @Test
    void naoDeveFazerNadaQuandoNaoHaClientesVencidos() {
        when(clienteRepository.findByAtivoFalseAndRetidoAteLessThanEqual(AGORA)).thenReturn(List.of());

        assertEquals(0, expurgo.expurgarVencidos());

        verify(clienteRepository, never()).delete(any());
        verify(clienteAuditor, never()).expurgo(any());
    }
}
