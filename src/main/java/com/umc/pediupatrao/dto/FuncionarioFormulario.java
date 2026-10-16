package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Funcionario;

public class FuncionarioFormulario {

    private String id;
    private String nome;
    private String cargo;
    private String telefone;
    private boolean ativo = true;

    public static FuncionarioFormulario de(Funcionario funcionario) {
        FuncionarioFormulario formulario = new FuncionarioFormulario();
        formulario.id = funcionario.getId();
        formulario.nome = funcionario.getNome();
        formulario.cargo = funcionario.getCargo();
        formulario.telefone = funcionario.getTelefone();
        formulario.ativo = !Boolean.FALSE.equals(funcionario.getAtivo());
        return formulario;
    }

    public FuncionarioRequisicao paraRequisicao() {
        return new FuncionarioRequisicao(nome, cargo, telefone, ativo);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
