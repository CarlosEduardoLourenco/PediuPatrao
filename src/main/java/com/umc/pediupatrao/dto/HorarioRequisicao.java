package com.umc.pediupatrao.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioRequisicao(DayOfWeek diaDaSemana, boolean aberto, LocalTime abertura, LocalTime fechamento) {
}
