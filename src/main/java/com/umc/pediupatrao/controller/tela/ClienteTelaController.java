package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.dto.ClienteFormulario;
import com.umc.pediupatrao.service.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteTelaController {

    private static final String LISTA = "redirect:/clientes";
    private static final String MENSAGEM_TERMOS_ENCAMINHADOS = "Termos de uso encaminhados ao cliente.";

    private final ClienteService clienteService;

    public ClienteTelaController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model modelo) {
        return ComposicaoDePagina.comConteudo(modelo, "clientes/lista");
    }

    @GetMapping("/novo")
    public String formularioDeCriacao(Model modelo) {
        return formulario(modelo, new ClienteFormulario());
    }

    @GetMapping("/editar/{id}")
    public String formularioDeEdicao(@PathVariable String id, Model modelo) {
        return formulario(modelo, ClienteFormulario.de(clienteService.buscar(id)));
    }

    @PostMapping("/salvar")
    public String criar(@ModelAttribute("cliente") ClienteFormulario formulario,
            RedirectAttributes atributos) {
        clienteService.criar(formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        atributos.addFlashAttribute("toast", MENSAGEM_TERMOS_ENCAMINHADOS);
        return LISTA;
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id, @ModelAttribute("cliente") ClienteFormulario formulario,
            RedirectAttributes atributos) {
        clienteService.atualizar(id, formulario.paraRequisicao());
        atributos.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return LISTA;
    }

    private String formulario(Model modelo, ClienteFormulario formulario) {
        modelo.addAttribute("cliente", formulario);
        return ComposicaoDePagina.comConteudo(modelo, "clientes/form");
    }
}
