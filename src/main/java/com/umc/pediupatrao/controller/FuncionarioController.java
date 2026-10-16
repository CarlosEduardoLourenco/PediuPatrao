package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.FuncionarioRequisicao;
import com.umc.pediupatrao.entity.Funcionario;
import com.umc.pediupatrao.service.FuncionarioService;
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
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public List<Funcionario> listar() {
        return funcionarioService.listar();
    }

    @GetMapping("/{id}")
    public Funcionario buscar(@PathVariable String id) {
        return funcionarioService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Funcionario> criar(@RequestBody FuncionarioRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criar(requisicao));
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable String id, @RequestBody FuncionarioRequisicao requisicao) {
        return funcionarioService.atualizar(id, requisicao);
    }
}
