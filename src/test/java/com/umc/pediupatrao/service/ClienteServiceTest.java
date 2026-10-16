package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.ClienteRequisicao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO = Clock.fixed(Instant.parse("2026-09-23T17:35:00Z"), FUSO);
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 23, 14, 35);

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteAuditor clienteAuditor;

    private ClienteService servico;

    @BeforeEach
    void preparar() {
        servico = new ClienteService(clienteRepository, clienteAuditor, RELOGIO_FIXO);
    }

    @Test
    void deveCriarClienteAtivoComTermoAceitoEAuditarACriacao() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente criado = servico.criar(requisicao("  Maria  ", " (11) 99999-1234 ", "Rua das Flores", true));

        assertEquals("Maria", criado.getNome());
        assertEquals("(11) 99999-1234", criado.getTelefone());
        assertEquals("Rua das Flores", criado.getLogradouro());
        assertTrue(criado.estaAtivo());
        assertEquals(AGORA, criado.getTermoAceitoEm());
        verify(clienteAuditor).criacao(criado);
    }

    @Test
    void naoDeveCriarClienteSemAceitarOTermo() {
        assertThrows(DadosInvalidosException.class,
                () -> servico.criar(requisicao("Maria", "(11) 99999-1234", "Rua A", false)));
        assertThrows(DadosInvalidosException.class,
                () -> servico.criar(requisicao("Maria", "(11) 99999-1234", "Rua A", null)));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void deveGravarCamposEmBrancoComoNulos() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente criado = servico.criar(requisicao("Maria", "(11) 99999-1234", "   ", true));

        assertNull(criado.getLogradouro());
    }

    @Test
    void naoDeveCriarClienteSemNome() {
        assertThrows(DadosInvalidosException.class,
                () -> servico.criar(requisicao(" ", "(11) 99999-1234", "Rua A", true)));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarClienteSemTelefone() {
        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao("Maria", null, "Rua A", true)));
    }

    @Test
    void deveFotografarAntesDeAtualizarEAuditarDepois() {
        Cliente existente = clienteExistente();
        Map<String, String> foto = Map.of("logradouro", "Rua das Flores");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(existente));
        when(clienteAuditor.fotografar(existente)).thenReturn(foto);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente atualizado = servico.atualizar("c1", requisicao("Maria", "(11) 99999-1234", "Av. Brasil", null));

        assertEquals("Av. Brasil", atualizado.getLogradouro());
        InOrder ordem = inOrder(clienteAuditor, clienteRepository);
        ordem.verify(clienteAuditor).fotografar(existente);
        ordem.verify(clienteRepository).save(existente);
        ordem.verify(clienteAuditor).alteracao(atualizado, foto);
    }

    @Test
    void deveRegistrarAceiteDoTermoEmClienteAntigoQueAindaNaoAceitou() {
        Cliente existente = clienteExistente();
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente atualizado = servico.atualizar("c1", requisicao("Maria", "(11) 99999-1234", "Rua das Flores", true));

        assertEquals(AGORA, atualizado.getTermoAceitoEm());
    }

    @Test
    void naoDeveSobrescreverAceiteJaRegistrado() {
        Cliente existente = clienteExistente();
        LocalDateTime aceiteOriginal = LocalDateTime.of(2026, 1, 10, 9, 0);
        existente.setTermoAceitoEm(aceiteOriginal);
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente atualizado = servico.atualizar("c1", requisicao("Maria", "(11) 99999-1234", "Rua das Flores", true));

        assertEquals(aceiteOriginal, atualizado.getTermoAceitoEm());
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> servico.atualizar("c1", requisicao("Maria", "(11) 99999-1234", "Rua A", null)));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void naoDeveAtualizarClienteInativo() {
        Cliente inativo = clienteExistente();
        inativo.setAtivo(false);
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(inativo));

        assertThrows(RegraNegocioException.class,
                () -> servico.atualizar("c1", requisicao("Maria", "(11) 99999-1234", "Rua A", null)));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void deveInativarClienteReterPorUmAnoEAuditar() {
        Cliente existente = clienteExistente();
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente inativado = servico.inativar("c1");

        assertFalse(inativado.estaAtivo());
        assertEquals(AGORA, inativado.getInativadoEm());
        assertEquals(AGORA.plusYears(1), inativado.getRetidoAte());
        verify(clienteAuditor).inativacao(inativado);
        verify(clienteRepository, never()).delete(any());
    }

    @Test
    void naoDeveInativarClienteJaInativo() {
        Cliente inativo = clienteExistente();
        inativo.setAtivo(false);
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(inativo));

        assertThrows(RegraNegocioException.class, () -> servico.inativar("c1"));
        verify(clienteAuditor, never()).inativacao(any());
    }

    @Test
    void naoDeveInativarClienteInexistente() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.inativar("c1"));
    }

    private Cliente clienteExistente() {
        Cliente existente = new Cliente();
        existente.setId("c1");
        existente.setNome("Maria");
        existente.setTelefone("(11) 99999-1234");
        existente.setLogradouro("Rua das Flores");
        return existente;
    }

    private ClienteRequisicao requisicao(String nome, String telefone, String logradouro, Boolean termoAceito) {
        return new ClienteRequisicao(nome, telefone, null, logradouro, null, null, null, null, null, termoAceito);
    }
}
