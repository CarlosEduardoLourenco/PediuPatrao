package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;

public record UsuarioRequisicao(String username, String password, Perfil perfil) {
}
