package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequisicao;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificadorDeSenha;
    private final UsuarioAutenticado usuarioAutenticado;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder codificadorDeSenha,
            UsuarioAutenticado usuarioAutenticado) {
        this.repositorio = repositorio;
        this.codificadorDeSenha = codificadorDeSenha;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    public List<Usuario> listar() {
        return repositorio.findAll();
    }

    public Usuario buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    public Usuario criar(UsuarioRequisicao requisicao) {
        exigirIdentificacaoEPerfil(requisicao);
        exigirSenha(requisicao.password());
        String nome = requisicao.username().trim();
        exigirNomeDisponivel(nome);

        Usuario usuario = new Usuario();
        usuario.setUsername(nome);
        usuario.setPassword(codificadorDeSenha.encode(requisicao.password()));
        usuario.setPerfil(requisicao.perfil());
        return repositorio.save(usuario);
    }

    public Usuario atualizar(String id, UsuarioRequisicao requisicao) {
        exigirIdentificacaoEPerfil(requisicao);
        Usuario usuario = buscar(id);
        String novoNome = requisicao.username().trim();

        if (!usuario.getUsername().equals(novoNome)) {
            exigirNomeDisponivel(novoNome);
        }
        if (usuario.getPerfil() != requisicao.perfil()) {
            impedirAlteracaoDoProprioPerfil(usuario);
            impedirPerdaDoUltimoAdministrador(usuario);
        }

        usuario.setUsername(novoNome);
        usuario.setPerfil(requisicao.perfil());
        if (possuiTexto(requisicao.password())) {
            usuario.setPassword(codificadorDeSenha.encode(requisicao.password()));
        }
        return repositorio.save(usuario);
    }

    public void remover(String id) {
        Usuario usuario = buscar(id);
        impedirAutoExclusao(usuario);
        impedirPerdaDoUltimoAdministrador(usuario);
        repositorio.delete(usuario);
    }

    private void exigirIdentificacaoEPerfil(UsuarioRequisicao requisicao) {
        if (!possuiTexto(requisicao.username())) {
            throw new DadosInvalidosException("Informe o nome de usuário");
        }
        if (requisicao.perfil() == null) {
            throw new DadosInvalidosException("Informe o perfil do usuário");
        }
    }

    private void exigirSenha(String senha) {
        if (!possuiTexto(senha)) {
            throw new DadosInvalidosException("Informe a senha do usuário");
        }
    }

    private void exigirNomeDisponivel(String nome) {
        if (repositorio.existsByUsername(nome)) {
            throw new RegraNegocioException("Já existe um usuário com o nome " + nome);
        }
    }

    private void impedirAlteracaoDoProprioPerfil(Usuario alvo) {
        if (ehUsuarioLogado(alvo)) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil");
        }
    }

    private void impedirAutoExclusao(Usuario alvo) {
        if (ehUsuarioLogado(alvo)) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio usuário");
        }
    }

    private void impedirPerdaDoUltimoAdministrador(Usuario alvo) {
        boolean ehAdministrador = alvo.getPerfil() == Perfil.ADMIN;
        if (ehAdministrador && repositorio.countByPerfil(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("O sistema precisa manter ao menos um administrador");
        }
    }

    private boolean ehUsuarioLogado(Usuario usuario) {
        return usuario.getUsername().equals(usuarioAutenticado.nome());
    }

    private boolean possuiTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
