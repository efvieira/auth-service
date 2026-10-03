package com.authservice.exception;

public class TokenExpiradoException extends RuntimeException {
    public TokenExpiradoException(String mensagem) {
        super(mensagem);
    }
}

