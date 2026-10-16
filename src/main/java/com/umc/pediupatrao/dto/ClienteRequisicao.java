package com.umc.pediupatrao.dto;

public record ClienteRequisicao(String nome, String telefone, String cep, String logradouro, String numero,
        String complemento, String bairro, String cidade, String estado, Boolean termoAceito) {

    public boolean aceitouOTermo() {
        return Boolean.TRUE.equals(termoAceito);
    }
}
