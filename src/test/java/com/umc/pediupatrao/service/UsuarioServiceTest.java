package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.UsuarioRequisicao;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String LOGADO = "logado";

    @Mock
    private UsuarioRepository repositorio;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);

    private UsuarioService servico;

    @BeforeEach
    void preparar() {
        servico = new UsuarioService(repositorio, codificador, usuarioAutenticado);
    }

    @Test
    void naoDeveCriarUsuarioSemSenha() {
        UsuarioRequisicao requisicao = new UsuarioRequisicao("novo", "  ", Perfil.ATENDENTE);

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
        verify(repositorio, never()).save(any());
    }

    @Test
    void naoDeveCriarUsuarioSemPerfil() {
        UsuarioRequisicao requisicao = new UsuarioRequisicao("novo", "senha", null);

        assertThrows(DadosInvalidosException.class, () -> servico.criar(requisicao));
    }

    @Test
    void naoDeveCriarUsuarioComNomeJaExistente() {
        when(repositorio.existsByUsername("novo")).thenReturn(true);
        UsuarioRequisicao requisicao = new UsuarioRequisicao("novo", "senha", Perfil.GERENTE);

        assertThrows(RegraNegocioException.class, () -> servico.criar(requisicao));
        verify(repositorio, never()).save(any());
    }

    @Test
    void deveCriptografarASenhaAoCriarUsuario() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        servico.criar(new UsuarioRequisicao("novo", "senha123", Perfil.GERENTE));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(capturado.capture());
        assertNotEquals("senha123", capturado.getValue().getPassword());
        assertTrue(codificador.matches("senha123", capturado.getValue().getPassword()));
        assertEquals(Perfil.GERENTE, capturado.getValue().getPerfil());
    }

    @Test
    void naoDevePermitirQueUsuarioAltereOProprioPerfil() {
        when(repositorio.findById("1")).thenReturn(Optional.of(usuario(LOGADO, Perfil.ADMIN)));
        when(usuarioAutenticado.nome()).thenReturn(LOGADO);
        UsuarioRequisicao requisicao = new UsuarioRequisicao(LOGADO, null, Perfil.GERENTE);

        assertThrows(RegraNegocioException.class, () -> servico.atualizar("1", requisicao));
        verify(repositorio, never()).save(any());
    }

    @Test
    void naoDevePermitirRebaixarOUltimoAdministrador() {
        when(repositorio.findById("1")).thenReturn(Optional.of(usuario("outro", Perfil.ADMIN)));
        when(usuarioAutenticado.nome()).thenReturn(LOGADO);
        when(repositorio.countByPerfil(Perfil.ADMIN)).thenReturn(1L);
        UsuarioRequisicao requisicao = new UsuarioRequisicao("outro", null, Perfil.GERENTE);

        assertThrows(RegraNegocioException.class, () -> servico.atualizar("1", requisicao));
        verify(repositorio, never()).save(any());
    }

    @Test
    void devePermitirRebaixarAdministradorQuandoExisteOutro() {
        Usuario alvo = usuario("outro", Perfil.ADMIN);
        when(repositorio.findById("1")).thenReturn(Optional.of(alvo));
        when(usuarioAutenticado.nome()).thenReturn(LOGADO);
        when(repositorio.countByPerfil(Perfil.ADMIN)).thenReturn(2L);
        when(repositorio.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario atualizado = servico.atualizar("1", new UsuarioRequisicao("outro", null, Perfil.GERENTE));

        assertEquals(Perfil.GERENTE, atualizado.getPerfil());
    }

    @Test
    void deveManterASenhaQuandoNaoForInformadaNaAtualizacao() {
        Usuario alvo = usuario("outro", Perfil.ATENDENTE);
        alvo.setPassword("hash-existente");
        when(repositorio.findById("1")).thenReturn(Optional.of(alvo));
        when(repositorio.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario atualizado = servico.atualizar("1", new UsuarioRequisicao("outro", "", Perfil.ATENDENTE));

        assertEquals("hash-existente", atualizado.getPassword());
    }

    @Test
    void naoDevePermitirExcluirOProprioUsuario() {
        when(repositorio.findById("1")).thenReturn(Optional.of(usuario(LOGADO, Perfil.GERENTE)));
        when(usuarioAutenticado.nome()).thenReturn(LOGADO);

        assertThrows(RegraNegocioException.class, () -> servico.remover("1"));
        verify(repositorio, never()).delete(any());
    }

    @Test
    void naoDevePermitirExcluirOUltimoAdministrador() {
        when(repositorio.findById("1")).thenReturn(Optional.of(usuario("outro", Perfil.ADMIN)));
        when(usuarioAutenticado.nome()).thenReturn(LOGADO);
        when(repositorio.countByPerfil(Perfil.ADMIN)).thenReturn(1L);

        assertThrows(RegraNegocioException.class, () -> servico.remover("1"));
        verify(repositorio, never()).delete(any());
    }

    private Usuario usuario(String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId("1");
        usuario.setUsername(nome);
        usuario.setPerfil(perfil);
        return usuario;
    }
}
