package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioRequisicao;
import com.umc.pediupatrao.dto.UsuarioResposta;
import com.umc.pediupatrao.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResposta> listar() {
        return usuarioService.listar().stream().map(UsuarioResposta::de).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResposta buscar(@PathVariable String id) {
        return UsuarioResposta.de(usuarioService.buscar(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResposta> criar(@RequestBody UsuarioRequisicao requisicao) {
        UsuarioResposta criado = UsuarioResposta.de(usuarioService.criar(requisicao));
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioResposta atualizar(@PathVariable String id, @RequestBody UsuarioRequisicao requisicao) {
        return UsuarioResposta.de(usuarioService.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable String id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
