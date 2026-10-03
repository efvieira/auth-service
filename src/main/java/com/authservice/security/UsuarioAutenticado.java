package com.authservice.security;

import com.authservice.enums.TipoPerfil;

/**
 * Principal colocado no SecurityContext pelo {@link JwtAuthenticationFilter}.
 */
public record UsuarioAutenticado(String id, String email, TipoPerfil perfil) {

    public boolean isAdmin() {
        return perfil == TipoPerfil.ADMIN;
    }
}
