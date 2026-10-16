package com.umc.pediupatrao.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "horariosAtendimento")
public class HorarioAtendimento {

    @Id
    private String id;
    private DayOfWeek diaDaSemana;
    private boolean aberto;
    private LocalTime abertura;
    private LocalTime fechamento;

    public boolean cruzaAMeiaNoite() {
        return aberto && abertura != null && fechamento != null && fechamento.isBefore(abertura);
    }

    public boolean funcionaNoHorario(LocalTime horario) {
        if (!aberto || abertura == null || fechamento == null) {
            return false;
        }
        if (cruzaAMeiaNoite()) {
            return !horario.isBefore(abertura);
        }
        return !horario.isBefore(abertura) && horario.isBefore(fechamento);
    }

    public boolean prolongaAteMadrugada(LocalTime horario) {
        return cruzaAMeiaNoite() && horario.isBefore(fechamento);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DayOfWeek getDiaDaSemana() {
        return diaDaSemana;
    }

    public void setDiaDaSemana(DayOfWeek diaDaSemana) {
        this.diaDaSemana = diaDaSemana;
    }

    public boolean isAberto() {
        return aberto;
    }

    public void setAberto(boolean aberto) {
        this.aberto = aberto;
    }

    public LocalTime getAbertura() {
        return abertura;
    }

    public void setAbertura(LocalTime abertura) {
        this.abertura = abertura;
    }

    public LocalTime getFechamento() {
        return fechamento;
    }

    public void setFechamento(LocalTime fechamento) {
        this.fechamento = fechamento;
    }
}
