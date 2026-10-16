package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Entregador;

public class EntregadorFormulario {

    private String id;
    private String nome;
    private String telefone;
    private String veiculo;
    private boolean ativo = true;

    public static EntregadorFormulario de(Entregador entregador) {
        EntregadorFormulario formulario = new EntregadorFormulario();
        formulario.id = entregador.getId();
        formulario.nome = entregador.getNome();
        formulario.telefone = entregador.getTelefone();
        formulario.veiculo = entregador.getVeiculo();
        formulario.ativo = !Boolean.FALSE.equals(entregador.getAtivo());
        return formulario;
    }

    public EntregadorRequisicao paraRequisicao() {
        return new EntregadorRequisicao(nome, telefone, veiculo, ativo);
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(String veiculo) {
        this.veiculo = veiculo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
