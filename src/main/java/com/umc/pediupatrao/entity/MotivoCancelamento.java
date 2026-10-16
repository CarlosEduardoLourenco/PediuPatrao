package com.umc.pediupatrao.entity;

public enum MotivoCancelamento {
    CLIENTE_DESISTIU("Cliente desistiu do pedido"),
    ERRO_NO_PEDIDO("Erro no registro do pedido"),
    ITEM_INDISPONIVEL("Item indisponível"),
    DEMORA_NA_PREPARACAO("Demora na preparação"),
    PAGAMENTO_RECUSADO("Pagamento recusado"),
    OUTRO("Outro motivo");

    private final String descricao;

    MotivoCancelamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean exigeObservacao() {
        return this == OUTRO;
    }
}
