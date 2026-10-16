package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.EntregadorRequisicao;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.service.EntregadorService;
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
@RequestMapping("/api/entregadores")
public class EntregadorController {

    private final EntregadorService entregadorService;

    public EntregadorController(EntregadorService entregadorService) {
        this.entregadorService = entregadorService;
    }

    @GetMapping
    public List<Entregador> listar() {
        return entregadorService.listar();
    }

    @GetMapping("/{id}")
    public Entregador buscar(@PathVariable String id) {
        return entregadorService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Entregador> criar(@RequestBody EntregadorRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entregadorService.criar(requisicao));
    }

    @PutMapping("/{id}")
    public Entregador atualizar(@PathVariable String id, @RequestBody EntregadorRequisicao requisicao) {
        return entregadorService.atualizar(id, requisicao);
    }
}
