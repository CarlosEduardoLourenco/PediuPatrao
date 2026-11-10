package com.umc.pediupatrao.dto.painel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.umc.pediupatrao.exception.DadosInvalidosException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PeriodoDoPainelTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);

    @Test
    void deveUsarOsUltimosTrintaDiasQuandoNadaForInformado() {
        PeriodoDoPainel periodo = PeriodoDoPainel.entre(null, null, HOJE);

        assertEquals(new PeriodoDoPainel(LocalDate.of(2026, 9, 16), HOJE), periodo);
        assertEquals(30, periodo.dias());
    }

    @Test
    void deveContarTrintaDiasParaTrasAPartirDaDataFinalInformada() {
        PeriodoDoPainel periodo = PeriodoDoPainel.entre(null, LocalDate.of(2026, 8, 30), HOJE);

        assertEquals(LocalDate.of(2026, 8, 1), periodo.de());
    }

    @Test
    void deveTerminarHojeQuandoSoADataInicialForInformada() {
        PeriodoDoPainel periodo = PeriodoDoPainel.entre(LocalDate.of(2026, 10, 1), null, HOJE);

        assertEquals(HOJE, periodo.ate());
    }

    @Test
    void deveRecusarDataInicialPosteriorAFinal() {
        assertThrows(DadosInvalidosException.class,
                () -> PeriodoDoPainel.entre(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), HOJE));
    }

    @Test
    void deveAceitarExatamenteOPeriodoMaximo() {
        assertDoesNotThrow(() -> PeriodoDoPainel.entre(LocalDate.of(2025, 10, 15), LocalDate.of(2026, 10, 15), HOJE));
    }

    @Test
    void deveRecusarPeriodoAcimaDoMaximo() {
        assertThrows(DadosInvalidosException.class,
                () -> PeriodoDoPainel.entre(LocalDate.of(2025, 10, 14), LocalDate.of(2026, 10, 15), HOJE));
    }

    @Test
    void deveIncluirODiaFinalInteiroNoIntervaloDeConsulta() {
        PeriodoDoPainel periodo = new PeriodoDoPainel(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), periodo.inicio());
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), periodo.fimExclusivo());
    }

    @Test
    void deveCalcularOPeriodoAnteriorComOMesmoTamanho() {
        PeriodoDoPainel periodo = new PeriodoDoPainel(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 14));

        assertEquals(new PeriodoDoPainel(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)), periodo.anterior());
    }
}
