package com.umc.pediupatrao.entity;

import java.util.Arrays;
import java.util.Optional;

public enum Perfil {
    ADMIN,
    GERENTE,
    ATENDENTE;

    private static final String PREFIXO_AUTORIDADE = "ROLE_";

    public String autoridade() {
        return PREFIXO_AUTORIDADE + name();
    }

    public static Optional<Perfil> deAutoridade(String autoridade) {
        return Arrays.stream(values())
                .filter(perfil -> perfil.autoridade().equals(autoridade))
                .findFirst();
    }
}
