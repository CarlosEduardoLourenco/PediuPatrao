package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;

public class UsuarioFormulario {

    private String id;
    private String username;
    private String password;
    private Perfil perfil;

    public static UsuarioFormulario de(Usuario usuario) {
        UsuarioFormulario formulario = new UsuarioFormulario();
        formulario.id = usuario.getId();
        formulario.username = usuario.getUsername();
        formulario.perfil = usuario.getPerfil();
        return formulario;
    }

    public UsuarioRequisicao paraRequisicao() {
        return new UsuarioRequisicao(username, password, perfil);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }
}
