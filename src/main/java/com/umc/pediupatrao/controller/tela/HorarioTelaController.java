package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.service.HorarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HorarioTelaController {

    private final HorarioService horarioService;

    public HorarioTelaController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping("/horarios")
    public String listar(Model modelo) {
        modelo.addAttribute("horarios", horarioService.listar());
        modelo.addAttribute("situacao", horarioService.situacaoAgora());
        return ComposicaoDePagina.comConteudo(modelo, "horarios");
    }
}
