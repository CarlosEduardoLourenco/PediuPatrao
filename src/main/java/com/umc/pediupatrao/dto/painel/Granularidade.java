package com.umc.pediupatrao.dto.painel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public enum Granularidade {
    DIA("dd/MM"),
    SEMANA("dd/MM"),
    MES("MM/yyyy");

    private final DateTimeFormatter formatoDoRotulo;

    Granularidade(String padraoDoRotulo) {
        this.formatoDoRotulo = DateTimeFormatter.ofPattern(padraoDoRotulo);
    }

    public LocalDate inicioDoIntervalo(LocalDate dia) {
        return switch (this) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }

    public LocalDate proximoIntervalo(LocalDate inicio) {
        return switch (this) {
            case DIA -> inicio.plusDays(1);
            case SEMANA -> inicio.plusWeeks(1);
            case MES -> inicio.plusMonths(1);
        };
    }

    public String rotulo(LocalDate inicio) {
        return inicio.format(formatoDoRotulo);
    }
}
