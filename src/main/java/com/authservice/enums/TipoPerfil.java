package com.authservice.enums;

public enum TipoPerfil {
    ADMIN("Administrador"),
    USER("Usuário");

    private final String descricao;

    TipoPerfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

