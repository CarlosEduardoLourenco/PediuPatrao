package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.umc.pediupatrao.dto.EventoDeAuditoria;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.entity.TipoOperacao;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteAuditorTest {

    @Mock
    private AuditoriaService auditoriaService;

    private ClienteAuditor auditor;

    @BeforeEach
    void preparar() {
        auditor = new ClienteAuditor(auditoriaService);
    }

    @Test
    void deveAuditarAlteracaoDeLogradouroComValoresAnteriorENovo() {
        Cliente cliente = clienteComLogradouro("Rua das Flores");
        Map<String, String> foto = auditor.fotografar(cliente);
        cliente.setLogradouro("Av. Brasil");

        auditor.alteracao(cliente, foto);

        EventoDeAuditoria evento = eventoRegistrado();
        assertEquals(TipoOperacao.ALTERACAO_CLIENTE, evento.operacao());
        assertEquals(TipoEntidade.CLIENTE, evento.entidade());
        assertEquals("c1", evento.entidadeId());
        assertEquals(1, evento.alteracoes().size());
        assertEquals(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil"), evento.alteracoes().get(0));
    }

    @Test
    void naoDeveAuditarQuandoNenhumCampoAuditadoMudou() {
        Cliente cliente = clienteComLogradouro("Rua das Flores");
        Map<String, String> foto = auditor.fotografar(cliente);

        auditor.alteracao(cliente, foto);

        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void deveAuditarMudancaApenasNoNome() {
        Cliente cliente = clienteComLogradouro("Rua das Flores");
        Map<String, String> foto = auditor.fotografar(cliente);
        cliente.setNome("Outro Nome");

        auditor.alteracao(cliente, foto);

        EventoDeAuditoria evento = eventoRegistrado();
        assertEquals(1, evento.alteracoes().size());
        assertEquals(new AlteracaoCampo("nome", "Maria", "Outro Nome"), evento.alteracoes().get(0));
    }

    @Test
    void deveAuditarOTelefoneComOsValoresAnteriorENovoCompletos() {
        Cliente cliente = clienteComLogradouro("Rua das Flores");
        Map<String, String> foto = auditor.fotografar(cliente);
        cliente.setTelefone("(11) 98888-1234");

        auditor.alteracao(cliente, foto);

        AlteracaoCampo alteracao = eventoRegistrado().alteracoes().get(0);
        assertEquals("telefone", alteracao.campo());
        assertEquals("(11) 99999-1234", alteracao.valorAnterior());
        assertEquals("(11) 98888-1234", alteracao.valorNovo());
    }

    @Test
    void deveAuditarCriacaoApenasComCamposPreenchidos() {
        Cliente cliente = new Cliente();
        cliente.setId("c2");
        cliente.setNome("Maria");
        cliente.setTelefone("(11) 99999-0000");
        cliente.setLogradouro("Rua A");

        auditor.criacao(cliente);

        EventoDeAuditoria evento = eventoRegistrado();
        assertEquals(TipoOperacao.CRIACAO_CLIENTE, evento.operacao());
        assertEquals(3, evento.alteracoes().size());
        assertTrue(evento.alteracoes().stream().allMatch(alteracao -> alteracao.valorAnterior() == null));
    }

    private EventoDeAuditoria eventoRegistrado() {
        ArgumentCaptor<EventoDeAuditoria> capturado = ArgumentCaptor.forClass(EventoDeAuditoria.class);
        verify(auditoriaService).registrar(capturado.capture());
        return capturado.getValue();
    }

    private Cliente clienteComLogradouro(String logradouro) {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        cliente.setTelefone("(11) 99999-1234");
        cliente.setLogradouro(logradouro);
        return cliente;
    }
}
