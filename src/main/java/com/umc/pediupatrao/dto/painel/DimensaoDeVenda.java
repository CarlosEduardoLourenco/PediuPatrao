package com.umc.pediupatrao.dto.painel;

import java.util.List;
import java.util.stream.IntStream;

public enum DimensaoDeVenda {
    HORA,
    DIA_DA_SEMANA,
    TIPO_SAIDA;

    private static final List<String> NOMES_DOS_DIAS = List.of("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom");

    public List<String> chaves() {
        return switch (this) {
            case HORA -> IntStream.range(0, 24).mapToObj(String::valueOf).toList();
            case DIA_DA_SEMANA -> IntStream.rangeClosed(1, 7).mapToObj(String::valueOf).toList();
            case TIPO_SAIDA -> List.of("ENTREGA", "RETIRADA");
        };
    }

    public String rotulo(String chave) {
        return switch (this) {
            case HORA -> chave + "h";
            case DIA_DA_SEMANA -> NOMES_DOS_DIAS.get(Integer.parseInt(chave) - 1);
            case TIPO_SAIDA -> chave.equals("ENTREGA") ? "Entrega" : "Retirada";
        };
    }
}
