package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(ContasDemonstracao.class)
public class SemeadorDeContas implements CommandLineRunner {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificadorDeSenha;
    private final ContasDemonstracao contas;

    public SemeadorDeContas(UsuarioRepository repositorio, PasswordEncoder codificadorDeSenha,
            ContasDemonstracao contas) {
        this.repositorio = repositorio;
        this.codificadorDeSenha = codificadorDeSenha;
        this.contas = contas;
    }

    @Override
    public void run(String... argumentos) {
        garantirConta("admin", contas.senhaAdmin(), Perfil.ADMIN);
        garantirConta("gerente01", contas.senhaGerente(), Perfil.GERENTE);
        garantirConta("atendente01", contas.senhaAtendente(), Perfil.ATENDENTE);
    }

    private void garantirConta(String username, String senha, Perfil perfil) {
        Optional<Usuario> existente = repositorio.findByUsername(username);
        boolean jaPossuiPerfil = existente.map(Usuario::getPerfil).isPresent();
        if (jaPossuiPerfil) {
            return;
        }
        Usuario usuario = existente.orElseGet(Usuario::new);
        usuario.setUsername(username);
        usuario.setPassword(codificadorDeSenha.encode(senha));
        usuario.setPerfil(perfil);
        repositorio.save(usuario);
    }
}
