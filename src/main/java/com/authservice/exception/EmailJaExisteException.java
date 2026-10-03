package com.authservice.exception;

public class EmailJaExisteException extends RuntimeException {
    public EmailJaExisteException(String mensagem) {
        super(mensagem);
    }
}

