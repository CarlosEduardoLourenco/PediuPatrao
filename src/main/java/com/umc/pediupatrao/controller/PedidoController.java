package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AvancoRequisicao;
import com.umc.pediupatrao.dto.CancelamentoRequisicao;
import com.umc.pediupatrao.dto.DescontoRequisicao;
import com.umc.pediupatrao.dto.PedidoRequisicao;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listar() {
        return pedidoService.listar();
    }

    @GetMapping("/{id}")
    public Pedido buscar(@PathVariable String id) {
        return pedidoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody PedidoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(requisicao));
    }

    @PostMapping("/{id}/avancar")
    public Pedido avancar(@PathVariable String id,
            @RequestBody(required = false) AvancoRequisicao requisicao) {
        AvancoRequisicao avanco = requisicao == null ? AvancoRequisicao.semDestino() : requisicao;
        return pedidoService.avancar(id, avanco);
    }

    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable String id, @RequestBody CancelamentoRequisicao requisicao) {
        return pedidoService.cancelar(id, requisicao);
    }

    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody DescontoRequisicao requisicao) {
        return pedidoService.aplicarDesconto(id, requisicao);
    }
}
