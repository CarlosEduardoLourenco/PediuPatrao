package com.umc.pediupatrao.config.demonstracao;

import com.umc.pediupatrao.entity.Produto;

public record OpcaoDoCardapio(Produto produto, int popularidade) {

    private static final String CATEGORIA_DE_BEBIDA = "Bebida";
    private static final String CATEGORIA_VENDIDA_EM_UNIDADES = "Esfiha";

    public boolean ehBebida() {
        return CATEGORIA_DE_BEBIDA.equals(produto.getTipo());
    }

    public boolean vendidaEmUnidades() {
        return CATEGORIA_VENDIDA_EM_UNIDADES.equals(produto.getTipo());
    }
}
