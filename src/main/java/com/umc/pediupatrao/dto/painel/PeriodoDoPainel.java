package com.umc.pediupatrao.dto.painel;

import com.umc.pediupatrao.exception.DadosInvalidosException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record PeriodoDoPainel(LocalDate de, LocalDate ate) {

    public static final int DIAS_DO_PERIODO_PADRAO = 30;
    public static final int DIAS_DO_PERIODO_MAXIMO = 366;

    public static PeriodoDoPainel entre(LocalDate de, LocalDate ate, LocalDate hoje) {
        LocalDate fim = ate != null ? ate : hoje;
        LocalDate inicio = de != null ? de : fim.minusDays(DIAS_DO_PERIODO_PADRAO - 1L);
        if (inicio.isAfter(fim)) {
            throw new DadosInvalidosException("A data inicial (de) não pode ser posterior à data final (ate)");
        }
        PeriodoDoPainel periodo = new PeriodoDoPainel(inicio, fim);
        if (periodo.dias() > DIAS_DO_PERIODO_MAXIMO) {
            throw new DadosInvalidosException("O período consultado pode ter no máximo "
                    + DIAS_DO_PERIODO_MAXIMO + " dias");
        }
        return periodo;
    }

    public long dias() {
        return ChronoUnit.DAYS.between(de, ate) + 1;
    }

    public LocalDateTime inicio() {
        return de.atStartOfDay();
    }

    public LocalDateTime fimExclusivo() {
        return ate.plusDays(1).atStartOfDay();
    }

    public PeriodoDoPainel anterior() {
        return new PeriodoDoPainel(de.minusDays(dias()), de.minusDays(1));
    }
}
