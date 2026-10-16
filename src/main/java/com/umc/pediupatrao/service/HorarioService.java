package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.HorarioRequisicao;
import com.umc.pediupatrao.dto.SituacaoDeFuncionamento;
import com.umc.pediupatrao.entity.HorarioAtendimento;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.repository.HorarioAtendimentoRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class HorarioService {

    static final LocalTime ABERTURA_PADRAO = LocalTime.of(18, 0);
    static final LocalTime FECHAMENTO_PADRAO = LocalTime.of(23, 0);
    private static final DateTimeFormatter FORMATO_DE_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final HorarioAtendimentoRepository repositorio;
    private final Clock relogio;

    public HorarioService(HorarioAtendimentoRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    @PreAuthorize("isAuthenticated()")
    public List<HorarioAtendimento> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(HorarioAtendimento::getDiaDaSemana))
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public SituacaoDeFuncionamento situacaoAgora() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        Optional<HorarioAtendimento> hoje = repositorio.findByDiaDaSemana(agora.getDayOfWeek());
        Optional<HorarioAtendimento> ontem = repositorio.findByDiaDaSemana(agora.getDayOfWeek().minus(1));

        boolean abertoPeloTurnoDeOntem = ontem.map(horario -> horario.prolongaAteMadrugada(agora.toLocalTime()))
                .orElse(false);
        if (abertoPeloTurnoDeOntem) {
            return new SituacaoDeFuncionamento(true,
                    "Aberto agora — fecha às " + ontem.get().getFechamento().format(FORMATO_DE_HORA));
        }
        boolean abertoHoje = hoje.map(horario -> horario.funcionaNoHorario(agora.toLocalTime())).orElse(false);
        if (abertoHoje) {
            return new SituacaoDeFuncionamento(true,
                    "Aberto agora — fecha às " + hoje.get().getFechamento().format(FORMATO_DE_HORA));
        }
        return new SituacaoDeFuncionamento(false, "Fechado agora");
    }

    @PreAuthorize("hasRole('GERENTE')")
    public List<HorarioAtendimento> atualizar(List<HorarioRequisicao> requisicoes) {
        exigirGradeCompleta(requisicoes);
        requisicoes.forEach(this::exigirHorarioCoerente);
        requisicoes.forEach(this::gravar);
        return listar();
    }

    public void criarGradePadraoSeVazia() {
        if (repositorio.count() > 0) {
            return;
        }
        for (DayOfWeek dia : DayOfWeek.values()) {
            repositorio.save(montar(new HorarioAtendimento(),
                    new HorarioRequisicao(dia, true, ABERTURA_PADRAO, FECHAMENTO_PADRAO)));
        }
    }

    private void gravar(HorarioRequisicao requisicao) {
        HorarioAtendimento horario = repositorio.findByDiaDaSemana(requisicao.diaDaSemana())
                .orElseGet(HorarioAtendimento::new);
        repositorio.save(montar(horario, requisicao));
    }

    private HorarioAtendimento montar(HorarioAtendimento horario, HorarioRequisicao requisicao) {
        horario.setDiaDaSemana(requisicao.diaDaSemana());
        horario.setAberto(requisicao.aberto());
        horario.setAbertura(requisicao.aberto() ? requisicao.abertura() : null);
        horario.setFechamento(requisicao.aberto() ? requisicao.fechamento() : null);
        return horario;
    }

    private void exigirGradeCompleta(List<HorarioRequisicao> requisicoes) {
        if (requisicoes == null) {
            throw new DadosInvalidosException("Informe os horários de todos os dias da semana");
        }
        Set<DayOfWeek> informados = EnumSet.noneOf(DayOfWeek.class);
        for (HorarioRequisicao requisicao : requisicoes) {
            if (requisicao == null || requisicao.diaDaSemana() == null || !informados.add(requisicao.diaDaSemana())) {
                throw new DadosInvalidosException("Informe cada dia da semana uma única vez");
            }
        }
        if (informados.size() != DayOfWeek.values().length) {
            throw new DadosInvalidosException("Informe os horários de todos os dias da semana");
        }
    }

    private void exigirHorarioCoerente(HorarioRequisicao requisicao) {
        if (!requisicao.aberto()) {
            return;
        }
        if (requisicao.abertura() == null || requisicao.fechamento() == null) {
            throw new DadosInvalidosException("Informe abertura e fechamento nos dias em que há atendimento");
        }
        if (requisicao.abertura().equals(requisicao.fechamento())) {
            throw new DadosInvalidosException("Abertura e fechamento não podem ser iguais");
        }
    }
}
