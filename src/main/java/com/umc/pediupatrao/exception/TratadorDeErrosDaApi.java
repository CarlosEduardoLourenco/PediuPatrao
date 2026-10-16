package com.umc.pediupatrao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class TratadorDeErrosDaApi {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException excecao) {
        return resposta(HttpStatus.CONFLICT, excecao.getMessage());
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResposta> dadosInvalidos(DadosInvalidosException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, excecao.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException excecao) {
        return resposta(HttpStatus.NOT_FOUND, excecao.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResposta(mensagem));
    }
}
