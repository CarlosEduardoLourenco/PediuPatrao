package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.service.AuditoriaService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaTelaController {

    private static final int REGISTROS_POR_PAGINA = 20;

    private final AuditoriaService auditoriaService;

    public AuditoriaTelaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String consultar(
            @RequestParam(required = false) TipoEntidade entidade,
            @RequestParam(required = false) String entidadeId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina,
            Model modelo) {
        FiltroAuditoria filtro = new FiltroAuditoria(entidade, entidadeId, usuario, de, ate);
        modelo.addAttribute("registros", auditoriaService.consultar(filtro, pagina, REGISTROS_POR_PAGINA));
        modelo.addAttribute("filtro", filtro);
        modelo.addAttribute("entidades", TipoEntidade.values());
        return ComposicaoDePagina.comConteudo(modelo, "auditoria");
    }
}
