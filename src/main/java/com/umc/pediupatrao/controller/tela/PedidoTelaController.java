package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.EntregadorService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pedidos")
public class PedidoTelaController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final EntregadorService entregadorService;

    public PedidoTelaController(PedidoService pedidoService, ClienteService clienteService,
            ProdutoService produtoService, EntregadorService entregadorService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.entregadorService = entregadorService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("pedidos", pedidoService.listar());
        return ComposicaoDePagina.comConteudo(modelo, "pedidos");
    }

    @GetMapping("/novo")
    public String formularioDeCriacao(Model modelo) {
        modelo.addAttribute("clientes", clienteService.listarAtivos());
        modelo.addAttribute("produtos", produtoService.listarAtivos());
        modelo.addAttribute("tiposSaida", TipoSaida.values());
        return ComposicaoDePagina.comConteudo(modelo, "pedidos/form");
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable String id, Model modelo) {
        Pedido pedido = pedidoService.buscar(id);
        modelo.addAttribute("pedido", pedido);
        modelo.addAttribute("proximoStatus", pedido.getStatus().proximo(pedido.getTipoSaida()).orElse(null));
        modelo.addAttribute("motivos", MotivoCancelamento.values());
        modelo.addAttribute("entregadores", entregadorService.listarAtivos());
        return ComposicaoDePagina.comConteudo(modelo, "pedidos/detalhe");
    }
}
