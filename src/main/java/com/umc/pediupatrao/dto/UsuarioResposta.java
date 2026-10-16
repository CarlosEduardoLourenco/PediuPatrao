package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;

public record UsuarioResposta(String id, String username, Perfil perfil) {

    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(usuario.getId(), usuario.getUsername(), usuario.getPerfil());
    }
}
