package com.umc.pediupatrao.security;

import com.umc.pediupatrao.entity.Perfil;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAutenticado {

    public String nome() {
        return autenticacaoAtual().getName();
    }

    public Perfil perfil() {
        return autenticacaoAtual().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Perfil::deAutoridade)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Usuário sem perfil válido"));
    }

    private Authentication autenticacaoAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || autenticacao instanceof AnonymousAuthenticationToken
                || !autenticacao.isAuthenticated()) {
            throw new AccessDeniedException("Nenhum usuário autenticado");
        }
        return autenticacao;
    }
}
