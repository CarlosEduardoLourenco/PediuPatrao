package com.umc.pediupatrao.controller.tela;

import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(basePackages = "com.umc.pediupatrao.controller.tela")
public class TratadorDeErrosDasTelas {

    private static final String CAMINHO_PADRAO = "/";

    @ExceptionHandler({
        RegraNegocioException.class,
        DadosInvalidosException.class,
        RecursoNaoEncontradoException.class
    })
    public String falhaDeNegocio(RuntimeException excecao, HttpServletRequest requisicao,
            RedirectAttributes atributos) {
        atributos.addFlashAttribute("erro", excecao.getMessage());
        return "redirect:" + caminhoDeOrigem(requisicao);
    }

    private String caminhoDeOrigem(HttpServletRequest requisicao) {
        String origem = requisicao.getHeader(HttpHeaders.REFERER);
        if (origem == null) {
            return CAMINHO_PADRAO;
        }
        try {
            String caminho = URI.create(origem).getPath();
            return caminho == null || caminho.isBlank() ? CAMINHO_PADRAO : caminho;
        } catch (IllegalArgumentException origemInvalida) {
            return CAMINHO_PADRAO;
        }
    }
}
