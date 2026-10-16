package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.EventoDeAuditoria;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO = Clock.fixed(Instant.parse("2026-09-23T17:35:00Z"), FUSO);

    @Mock
    private AuditoriaRepository repositorio;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    private AuditoriaService servico;

    @BeforeEach
    void preparar() {
        servico = new AuditoriaService(repositorio, mongoTemplate, usuarioAutenticado, RELOGIO_FIXO);
        when(usuarioAutenticado.nome()).thenReturn("atendente01");
        when(usuarioAutenticado.perfil()).thenReturn(Perfil.ATENDENTE);
    }

    @Test
    void deveRegistrarUsuarioPerfilDataEOperacao() {
        servico.registrar(new EventoDeAuditoria(TipoOperacao.ALTERACAO_CLIENTE, TipoEntidade.CLIENTE, "c1",
                List.of(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil")), null));

        RegistroAuditoria registro = registroSalvo();
        assertEquals("atendente01", registro.usuario());
        assertEquals(Perfil.ATENDENTE, registro.perfil());
        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 35), registro.dataHora());
        assertEquals(TipoOperacao.ALTERACAO_CLIENTE, registro.operacao());
        assertEquals(TipoEntidade.CLIENTE, registro.entidade());
        assertEquals("c1", registro.entidadeId());
        assertEquals(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil"),
                registro.alteracoes().get(0));
        assertNull(registro.justificativa());
    }

    @Test
    void deveGuardarAJustificativaQuandoInformada() {
        servico.registrar(new EventoDeAuditoria(TipoOperacao.CANCELAMENTO, TipoEntidade.PEDIDO, "p1",
                List.of(new AlteracaoCampo("status", "PRONTO", "CANCELADO")), "Cliente desistiu do pedido"));

        assertEquals("Cliente desistiu do pedido", registroSalvo().justificativa());
    }

    @Test
    void naoDeveGravarCamposSensiveis() {
        servico.registrar(new EventoDeAuditoria(TipoOperacao.ALTERACAO_CLIENTE, TipoEntidade.CLIENTE, "c1",
                List.of(new AlteracaoCampo("password", "antiga", "nova"),
                        new AlteracaoCampo("Senha", "a", "b"),
                        new AlteracaoCampo("accessToken", "t1", "t2"),
                        new AlteracaoCampo("bairro", "Centro", "Vila"))
                , null));

        List<AlteracaoCampo> gravadas = registroSalvo().alteracoes();
        assertEquals(1, gravadas.size());
        assertEquals("bairro", gravadas.get(0).campo());
    }

    private RegistroAuditoria registroSalvo() {
        ArgumentCaptor<RegistroAuditoria> capturado = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorio).save(capturado.capture());
        return capturado.getValue();
    }
}
