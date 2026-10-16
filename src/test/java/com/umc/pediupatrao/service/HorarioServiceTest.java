package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.HorarioRequisicao;
import com.umc.pediupatrao.dto.SituacaoDeFuncionamento;
import com.umc.pediupatrao.entity.HorarioAtendimento;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.repository.HorarioAtendimentoRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private HorarioAtendimentoRepository repositorio;

    @Test
    void deveEstarAbertoDentroDoHorarioDoDia() {
        when(repositorio.findByDiaDaSemana(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.WEDNESDAY, true, "18:00", "23:00")));
        when(repositorio.findByDiaDaSemana(DayOfWeek.TUESDAY)).thenReturn(Optional.empty());

        SituacaoDeFuncionamento situacao = servicoEm("2026-09-23T20:00:00").situacaoAgora();

        assertTrue(situacao.aberto());
        assertEquals("Aberto agora — fecha às 23:00", situacao.mensagem());
    }

    @Test
    void deveEstarFechadoForaDoHorario() {
        when(repositorio.findByDiaDaSemana(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.WEDNESDAY, true, "18:00", "23:00")));
        when(repositorio.findByDiaDaSemana(DayOfWeek.TUESDAY)).thenReturn(Optional.empty());

        SituacaoDeFuncionamento situacao = servicoEm("2026-09-23T15:00:00").situacaoAgora();

        assertFalse(situacao.aberto());
        assertEquals("Fechado agora", situacao.mensagem());
    }

    @Test
    void deveEstarFechadoNoHorarioExatoDeFechamento() {
        when(repositorio.findByDiaDaSemana(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.WEDNESDAY, true, "18:00", "23:00")));
        when(repositorio.findByDiaDaSemana(DayOfWeek.TUESDAY)).thenReturn(Optional.empty());

        assertFalse(servicoEm("2026-09-23T23:00:00").situacaoAgora().aberto());
    }

    @Test
    void deveEstarFechadoEmDiaSemAtendimento() {
        when(repositorio.findByDiaDaSemana(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.WEDNESDAY, false, null, null)));
        when(repositorio.findByDiaDaSemana(DayOfWeek.TUESDAY)).thenReturn(Optional.empty());

        assertFalse(servicoEm("2026-09-23T20:00:00").situacaoAgora().aberto());
    }

    @Test
    void deveEstarAbertoNaMadrugadaQuandoOTurnoDeOntemCruzaAMeiaNoite() {
        when(repositorio.findByDiaDaSemana(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.WEDNESDAY, true, "18:00", "23:00")));
        when(repositorio.findByDiaDaSemana(DayOfWeek.TUESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.TUESDAY, true, "18:00", "02:00")));

        SituacaoDeFuncionamento situacao = servicoEm("2026-09-23T01:00:00").situacaoAgora();

        assertTrue(situacao.aberto());
        assertEquals("Aberto agora — fecha às 02:00", situacao.mensagem());
    }

    @Test
    void deveEstarAbertoAntesDaMeiaNoiteEmTurnoQueCruzaAMeiaNoite() {
        when(repositorio.findByDiaDaSemana(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(horario(DayOfWeek.WEDNESDAY, true, "18:00", "02:00")));
        when(repositorio.findByDiaDaSemana(DayOfWeek.TUESDAY)).thenReturn(Optional.empty());

        assertTrue(servicoEm("2026-09-23T23:30:00").situacaoAgora().aberto());
    }

    @Test
    void deveCriarAGradePadraoApenasQuandoNaoHaHorarios() {
        when(repositorio.count()).thenReturn(0L);

        servicoEm("2026-09-23T20:00:00").criarGradePadraoSeVazia();

        verify(repositorio, times(7)).save(any(HorarioAtendimento.class));
    }

    @Test
    void naoDeveSobrescreverGradeExistente() {
        when(repositorio.count()).thenReturn(7L);

        servicoEm("2026-09-23T20:00:00").criarGradePadraoSeVazia();

        verify(repositorio, never()).save(any());
    }

    @Test
    void deveRecusarGradeIncompleta() {
        List<HorarioRequisicao> incompleta = gradeCompleta();
        incompleta.remove(0);

        assertThrows(DadosInvalidosException.class,
                () -> servicoEm("2026-09-23T20:00:00").atualizar(incompleta));
        verify(repositorio, never()).save(any());
    }

    @Test
    void deveRecusarDiaRepetido() {
        List<HorarioRequisicao> repetida = gradeCompleta();
        repetida.set(1, repetida.get(0));

        assertThrows(DadosInvalidosException.class,
                () -> servicoEm("2026-09-23T20:00:00").atualizar(repetida));
    }

    @Test
    void deveRecusarDiaAbertoSemHorarios() {
        List<HorarioRequisicao> grade = gradeCompleta();
        grade.set(0, new HorarioRequisicao(DayOfWeek.MONDAY, true, null, null));

        assertThrows(DadosInvalidosException.class,
                () -> servicoEm("2026-09-23T20:00:00").atualizar(grade));
    }

    @Test
    void deveRecusarAberturaIgualAoFechamento() {
        List<HorarioRequisicao> grade = gradeCompleta();
        grade.set(0, new HorarioRequisicao(DayOfWeek.MONDAY, true, LocalTime.of(18, 0), LocalTime.of(18, 0)));

        assertThrows(DadosInvalidosException.class,
                () -> servicoEm("2026-09-23T20:00:00").atualizar(grade));
    }

    @Test
    void deveGravarGradeCompletaLimpandoHorariosDosDiasFechados() {
        List<HorarioRequisicao> grade = gradeCompleta();
        grade.set(0, new HorarioRequisicao(DayOfWeek.MONDAY, false, LocalTime.of(18, 0), LocalTime.of(23, 0)));
        when(repositorio.findByDiaDaSemana(any(DayOfWeek.class))).thenReturn(Optional.empty());

        servicoEm("2026-09-23T20:00:00").atualizar(grade);

        verify(repositorio, times(7)).save(any(HorarioAtendimento.class));
    }

    private HorarioService servicoEm(String dataHoraLocal) {
        Instant instante = java.time.LocalDateTime.parse(dataHoraLocal).atZone(FUSO).toInstant();
        return new HorarioService(repositorio, Clock.fixed(instante, FUSO));
    }

    private HorarioAtendimento horario(DayOfWeek dia, boolean aberto, String abertura, String fechamento) {
        HorarioAtendimento horario = new HorarioAtendimento();
        horario.setDiaDaSemana(dia);
        horario.setAberto(aberto);
        horario.setAbertura(abertura == null ? null : LocalTime.parse(abertura));
        horario.setFechamento(fechamento == null ? null : LocalTime.parse(fechamento));
        return horario;
    }

    private List<HorarioRequisicao> gradeCompleta() {
        List<HorarioRequisicao> grade = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            grade.add(new HorarioRequisicao(dia, true, LocalTime.of(18, 0), LocalTime.of(23, 0)));
        }
        return grade;
    }
}
