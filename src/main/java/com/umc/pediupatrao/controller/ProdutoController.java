package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ProdutoRequisicao;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.service.ProdutoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<Produto> listar() {
        return produtoService.listar();
    }

    @GetMapping("/{id}")
    public Produto buscar(@PathVariable String id) {
        return produtoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Produto> criar(@RequestBody ProdutoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(requisicao));
    }

    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable String id, @RequestBody ProdutoRequisicao requisicao) {
        return produtoService.atualizar(id, requisicao);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
