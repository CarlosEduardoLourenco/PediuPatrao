package com.umc.pediupatrao.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, String identificador) {
        super(recurso + " não encontrado: " + identificador);
    }
}
