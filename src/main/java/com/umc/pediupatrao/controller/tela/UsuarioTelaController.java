package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.dto.UsuarioFormulario;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioTelaController {

    private static final String LISTA = "redirect:/usuarios";

    private final UsuarioService usuarioService;

    public UsuarioTelaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("usuarios", usuarioService.listar());
        return ComposicaoDePagina.comConteudo(modelo, "usuarios/lista");
    }

    @GetMapping("/novo")
    public String formularioDeCriacao(Model modelo) {
        return formulario(modelo, new UsuarioFormulario());
    }

    @GetMapping("/editar/{id}")
    public String formularioDeEdicao(@PathVariable String id, Model modelo) {
        return formulario(modelo, UsuarioFormulario.de(usuarioService.buscar(id)));
    }

    @PostMapping("/salvar")
    public String criar(@ModelAttribute("usuario") UsuarioFormulario formulario,
            RedirectAttributes atributos) {
        usuarioService.criar(formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        return LISTA;
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id, @ModelAttribute("usuario") UsuarioFormulario formulario,
            RedirectAttributes atributos) {
        usuarioService.atualizar(id, formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return LISTA;
    }

    @PostMapping("/deletar/{id}")
    public String remover(@PathVariable String id, RedirectAttributes atributos) {
        usuarioService.remover(id);
        atributos.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        return LISTA;
    }

    private String formulario(Model modelo, UsuarioFormulario formulario) {
        modelo.addAttribute("usuario", formulario);
        modelo.addAttribute("perfis", Perfil.values());
        return ComposicaoDePagina.comConteudo(modelo, "usuarios/form");
    }
}
