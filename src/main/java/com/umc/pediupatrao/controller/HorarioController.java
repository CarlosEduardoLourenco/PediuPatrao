package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.HorarioRequisicao;
import com.umc.pediupatrao.dto.SituacaoDeFuncionamento;
import com.umc.pediupatrao.entity.HorarioAtendimento;
import com.umc.pediupatrao.service.HorarioService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping
    public List<HorarioAtendimento> listar() {
        return horarioService.listar();
    }

    @GetMapping("/situacao")
    public SituacaoDeFuncionamento situacao() {
        return horarioService.situacaoAgora();
    }

    @PutMapping
    public List<HorarioAtendimento> atualizar(@RequestBody List<HorarioRequisicao> requisicoes) {
        return horarioService.atualizar(requisicoes);
    }
}
