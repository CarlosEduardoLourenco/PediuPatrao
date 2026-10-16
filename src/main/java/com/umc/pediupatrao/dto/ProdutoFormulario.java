package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Produto;

public class ProdutoFormulario {

    private String id;
    private String nome;
    private String tipo;
    private Double preco;
    private boolean ativo = true;

    public static ProdutoFormulario de(Produto produto) {
        ProdutoFormulario formulario = new ProdutoFormulario();
        formulario.id = produto.getId();
        formulario.nome = produto.getNome();
        formulario.tipo = produto.getTipo();
        formulario.preco = produto.getPreco();
        formulario.ativo = Boolean.TRUE.equals(produto.getAtivo());
        return formulario;
    }

    public ProdutoRequisicao paraRequisicao() {
        return new ProdutoRequisicao(nome, tipo, preco, ativo);
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
