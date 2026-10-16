package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ClienteRequisicao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.service.ClienteService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listar();
    }

    @GetMapping("/{id}")
    public Cliente buscar(@PathVariable String id) {
        return clienteService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Cliente> criar(@RequestBody ClienteRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(requisicao));
    }

    @PutMapping("/{id}")
    public Cliente atualizar(@PathVariable String id, @RequestBody ClienteRequisicao requisicao) {
        return clienteService.atualizar(id, requisicao);
    }

    @PostMapping("/{id}/inativar")
    public Cliente inativar(@PathVariable String id) {
        return clienteService.inativar(id);
    }
}
