package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.dto.ProdutoFormulario;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produtos")
public class ProdutoTelaController {

    private static final String LISTA = "redirect:/produtos";

    private final ProdutoService produtoService;

    public ProdutoTelaController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("produtos", produtoService.listar());
        return ComposicaoDePagina.comConteudo(modelo, "produtos/lista");
    }

    @GetMapping("/novo")
    public String formularioDeCriacao(Model modelo) {
        return formulario(modelo, new ProdutoFormulario());
    }

    @GetMapping("/editar/{id}")
    public String formularioDeEdicao(@PathVariable String id, Model modelo) {
        return formulario(modelo, ProdutoFormulario.de(produtoService.buscar(id)));
    }

    @PostMapping("/salvar")
    public String criar(@ModelAttribute("produto") ProdutoFormulario formulario, RedirectAttributes atributos) {
        produtoService.criar(formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Produto criado com sucesso!");
        return LISTA;
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id, @ModelAttribute("produto") ProdutoFormulario formulario,
            RedirectAttributes atributos) {
        produtoService.atualizar(id, formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Produto atualizado com sucesso!");
        return LISTA;
    }

    @PostMapping("/alternar/{id}")
    public String alternarAtivo(@PathVariable String id, RedirectAttributes atributos) {
        produtoService.alternarAtivo(id);
        atributos.addFlashAttribute("sucesso", "Situação do produto alterada.");
        return LISTA;
    }

    @PostMapping("/deletar/{id}")
    public String excluir(@PathVariable String id, RedirectAttributes atributos) {
        produtoService.excluir(id);
        atributos.addFlashAttribute("sucesso", "Produto removido com sucesso!");
        return LISTA;
    }

    private String formulario(Model modelo, ProdutoFormulario formulario) {
        modelo.addAttribute("produto", formulario);
        return ComposicaoDePagina.comConteudo(modelo, "produtos/form");
    }
}
