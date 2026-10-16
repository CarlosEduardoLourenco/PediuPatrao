package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ComparadorDeCampos {

    private ComparadorDeCampos() {
    }

    public static List<AlteracaoCampo> diferencas(Map<String, String> antes, Map<String, String> depois) {
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        depois.forEach((campo, valorNovo) -> {
            String valorAnterior = antes.get(campo);
            if (!Objects.equals(vazioComoNulo(valorAnterior), vazioComoNulo(valorNovo))) {
                alteracoes.add(new AlteracaoCampo(campo, vazioComoNulo(valorAnterior), vazioComoNulo(valorNovo)));
            }
        });
        return alteracoes;
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
