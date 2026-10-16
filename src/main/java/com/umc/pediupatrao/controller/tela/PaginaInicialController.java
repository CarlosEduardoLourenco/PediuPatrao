package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.service.HorarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaInicialController {

    private final HorarioService horarioService;

    public PaginaInicialController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String inicio(Model modelo) {
        modelo.addAttribute("situacao", horarioService.situacaoAgora());
        return ComposicaoDePagina.comConteudo(modelo, "home");
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model modelo) {
        return ComposicaoDePagina.comConteudo(modelo, "configuracoes");
    }

    @GetMapping("/admin")
    public String painelAdministrativo() {
        return "admin";
    }
}
