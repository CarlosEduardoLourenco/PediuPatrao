package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.dto.FuncionarioFormulario;
import com.umc.pediupatrao.service.FuncionarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/funcionarios")
public class FuncionarioTelaController {

    private static final String LISTA = "redirect:/funcionarios";

    private final FuncionarioService funcionarioService;

    public FuncionarioTelaController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("funcionarios", funcionarioService.listar());
        return ComposicaoDePagina.comConteudo(modelo, "funcionarios/lista");
    }

    @GetMapping("/novo")
    public String formularioDeCriacao(Model modelo) {
        return formulario(modelo, new FuncionarioFormulario());
    }

    @GetMapping("/editar/{id}")
    public String formularioDeEdicao(@PathVariable String id, Model modelo) {
        return formulario(modelo, FuncionarioFormulario.de(funcionarioService.buscar(id)));
    }

    @PostMapping("/salvar")
    public String criar(@ModelAttribute("funcionario") FuncionarioFormulario formulario,
            RedirectAttributes atributos) {
        funcionarioService.criar(formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Funcionário cadastrado com sucesso!");
        return LISTA;
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id,
            @ModelAttribute("funcionario") FuncionarioFormulario formulario, RedirectAttributes atributos) {
        funcionarioService.atualizar(id, formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Funcionário atualizado com sucesso!");
        return LISTA;
    }

    @PostMapping("/alternar/{id}")
    public String alternarAtivo(@PathVariable String id, RedirectAttributes atributos) {
        funcionarioService.alternarAtivo(id);
        atributos.addFlashAttribute("sucesso", "Situação do funcionário alterada.");
        return LISTA;
    }

    private String formulario(Model modelo, FuncionarioFormulario formulario) {
        modelo.addAttribute("funcionario", formulario);
        return ComposicaoDePagina.comConteudo(modelo, "funcionarios/form");
    }
}
