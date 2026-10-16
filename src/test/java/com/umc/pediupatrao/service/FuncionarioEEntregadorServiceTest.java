package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.EntregadorRequisicao;
import com.umc.pediupatrao.dto.FuncionarioRequisicao;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.Funcionario;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.FuncionarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FuncionarioEEntregadorServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private EntregadorRepository entregadorRepository;

    private FuncionarioService funcionarioService;
    private EntregadorService entregadorService;

    @BeforeEach
    void preparar() {
        funcionarioService = new FuncionarioService(funcionarioRepository);
        entregadorService = new EntregadorService(entregadorRepository);
    }

    @Test
    void deveCadastrarFuncionarioAtivoComDadosLimpos() {
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Funcionario criado = funcionarioService.criar(new FuncionarioRequisicao("  Ana  ", " Pizzaiola ", "  ", null));

        assertEquals("Ana", criado.getNome());
        assertEquals("Pizzaiola", criado.getCargo());
        assertNull(criado.getTelefone());
        assertTrue(criado.getAtivo());
    }

    @Test
    void naoDeveCadastrarFuncionarioSemNomeOuCargo() {
        assertThrows(DadosInvalidosException.class,
                () -> funcionarioService.criar(new FuncionarioRequisicao(" ", "Caixa", null, true)));
        assertThrows(DadosInvalidosException.class,
                () -> funcionarioService.criar(new FuncionarioRequisicao("Ana", null, null, true)));
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void deveAtualizarFuncionarioExistente() {
        Funcionario existente = funcionario("f1", true);
        when(funcionarioRepository.findById("f1")).thenReturn(Optional.of(existente));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Funcionario atualizado = funcionarioService.atualizar("f1",
                new FuncionarioRequisicao("Ana Paula", "Gerente", "(11) 90000-0000", true));

        assertEquals("Ana Paula", atualizado.getNome());
        assertEquals("(11) 90000-0000", atualizado.getTelefone());
    }

    @Test
    void naoDeveAtualizarFuncionarioInexistente() {
        when(funcionarioRepository.findById("f1")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> funcionarioService.atualizar("f1", new FuncionarioRequisicao("Ana", "Caixa", null, true)));
    }

    @Test
    void deveAlternarASituacaoDoFuncionario() {
        when(funcionarioRepository.findById("f1")).thenReturn(Optional.of(funcionario("f1", true)));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertFalse(funcionarioService.alternarAtivo("f1").getAtivo());
    }

    @Test
    void deveTratarFuncionarioSemSituacaoComoAtivoAoAlternar() {
        Funcionario antigo = funcionario("f1", true);
        antigo.setAtivo(null);
        when(funcionarioRepository.findById("f1")).thenReturn(Optional.of(antigo));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertFalse(funcionarioService.alternarAtivo("f1").getAtivo());
    }

    @Test
    void deveCadastrarEntregadorExigindoNomeETelefone() {
        when(entregadorRepository.save(any(Entregador.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Entregador criado = entregadorService.criar(new EntregadorRequisicao("Carlos", "(11) 98888-0000", " ", true));

        assertEquals("Carlos", criado.getNome());
        assertNull(criado.getVeiculo());
        assertThrows(DadosInvalidosException.class,
                () -> entregadorService.criar(new EntregadorRequisicao("Carlos", " ", "Moto", true)));
        assertThrows(DadosInvalidosException.class,
                () -> entregadorService.criar(new EntregadorRequisicao(null, "(11) 98888-0000", "Moto", true)));
    }

    @Test
    void deveListarSomenteEntregadoresAtivos() {
        Entregador ativo = entregador("e1", true);
        Entregador semSituacao = entregador("e2", true);
        semSituacao.setAtivo(null);
        when(entregadorRepository.findAll()).thenReturn(List.of(ativo, entregador("e3", false), semSituacao));

        List<Entregador> ativos = entregadorService.listarAtivos();

        assertEquals(List.of(ativo, semSituacao), ativos);
    }

    @Test
    void deveAlternarASituacaoDoEntregador() {
        when(entregadorRepository.findById("e1")).thenReturn(Optional.of(entregador("e1", true)));
        when(entregadorRepository.save(any(Entregador.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertFalse(entregadorService.alternarAtivo("e1").getAtivo());
    }

    @Test
    void naoDeveBuscarEntregadorInexistente() {
        when(entregadorRepository.findById("e1")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> entregadorService.buscar("e1"));
    }

    private Funcionario funcionario(String id, boolean ativo) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("Ana");
        funcionario.setCargo("Caixa");
        funcionario.setAtivo(ativo);
        return funcionario;
    }

    private Entregador entregador(String id, boolean ativo) {
        Entregador entregador = new Entregador();
        entregador.setId(id);
        entregador.setNome("Carlos");
        entregador.setTelefone("(11) 98888-0000");
        entregador.setAtivo(ativo);
        return entregador;
    }
}
