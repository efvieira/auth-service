package com.authservice.exception;

public class PermissaoNegadaException extends RuntimeException {
    public PermissaoNegadaException(String mensagem) {
        super(mensagem);
    }
}

