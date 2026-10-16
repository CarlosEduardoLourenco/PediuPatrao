package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.service.AuditoriaService;
import java.time.LocalDate;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PagedModel<RegistroAuditoria> consultar(
            @RequestParam(required = false) TipoEntidade entidade,
            @RequestParam(required = false) String entidadeId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        FiltroAuditoria filtro = new FiltroAuditoria(entidade, entidadeId, usuario, de, ate);
        return new PagedModel<>(auditoriaService.consultar(filtro, pagina, tamanho));
    }
}
