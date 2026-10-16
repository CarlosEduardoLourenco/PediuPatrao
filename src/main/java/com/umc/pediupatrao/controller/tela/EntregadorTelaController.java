package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.dto.EntregadorFormulario;
import com.umc.pediupatrao.service.EntregadorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/entregadores")
public class EntregadorTelaController {

    private static final String LISTA = "redirect:/entregadores";

    private final EntregadorService entregadorService;

    public EntregadorTelaController(EntregadorService entregadorService) {
        this.entregadorService = entregadorService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("entregadores", entregadorService.listar());
        return ComposicaoDePagina.comConteudo(modelo, "entregadores/lista");
    }

    @GetMapping("/novo")
    public String formularioDeCriacao(Model modelo) {
        return formulario(modelo, new EntregadorFormulario());
    }

    @GetMapping("/editar/{id}")
    public String formularioDeEdicao(@PathVariable String id, Model modelo) {
        return formulario(modelo, EntregadorFormulario.de(entregadorService.buscar(id)));
    }

    @PostMapping("/salvar")
    public String criar(@ModelAttribute("entregador") EntregadorFormulario formulario,
            RedirectAttributes atributos) {
        entregadorService.criar(formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Entregador cadastrado com sucesso!");
        return LISTA;
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id,
            @ModelAttribute("entregador") EntregadorFormulario formulario, RedirectAttributes atributos) {
        entregadorService.atualizar(id, formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Entregador atualizado com sucesso!");
        return LISTA;
    }

    @PostMapping("/alternar/{id}")
    public String alternarAtivo(@PathVariable String id, RedirectAttributes atributos) {
        entregadorService.alternarAtivo(id);
        atributos.addFlashAttribute("sucesso", "Situação do entregador alterada.");
        return LISTA;
    }

    private String formulario(Model modelo, EntregadorFormulario formulario) {
        modelo.addAttribute("entregador", formulario);
        return ComposicaoDePagina.comConteudo(modelo, "entregadores/form");
    }
}
