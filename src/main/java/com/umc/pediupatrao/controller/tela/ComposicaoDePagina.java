package com.umc.pediupatrao.controller.tela;

import org.springframework.ui.Model;

final class ComposicaoDePagina {

    private static final String LAYOUT = "layout";
    private static final String SUFIXO_FRAGMENTO = " :: content";

    private ComposicaoDePagina() {
    }

    static String comConteudo(Model modelo, String pagina) {
        modelo.addAttribute("content", pagina + SUFIXO_FRAGMENTO);
        return LAYOUT;
    }
}
