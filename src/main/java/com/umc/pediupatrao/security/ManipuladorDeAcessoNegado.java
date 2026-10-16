package com.umc.pediupatrao.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.exception.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ManipuladorDeAcessoNegado implements AccessDeniedHandler {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManipuladorDeAcessoNegado.class);
    private static final String MENSAGEM = "Você não tem permissão para executar esta operação";

    private final ObjectMapper conversorJson;

    public ManipuladorDeAcessoNegado(ObjectMapper conversorJson) {
        this.conversorJson = conversorJson;
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta,
            AccessDeniedException excecao) throws IOException {
        REGISTRO.warn("Acesso negado: usuario={} metodo={} caminho={}",
                requisicao.getRemoteUser(), requisicao.getMethod(), requisicao.getRequestURI());
        if (ehRequisicaoDeApi(requisicao)) {
            resposta.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resposta.setCharacterEncoding("UTF-8");
            conversorJson.writeValue(resposta.getOutputStream(), new ErroResposta(MENSAGEM));
            return;
        }
        resposta.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    static boolean ehRequisicaoDeApi(HttpServletRequest requisicao) {
        return requisicao.getRequestURI().startsWith("/api/");
    }
}
