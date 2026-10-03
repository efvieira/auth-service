package com.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    static final String CLAIM_USUARIO_ID = "usuarioId";
    static final String CLAIM_TIPO = "tipo";
    static final String TIPO_ACCESS = "access";
    static final String TIPO_REFRESH = "refresh";

    private final SecretKey chave;
    private final long jwtExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration:3600000}") long jwtExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpiration) {
        // Lança WeakKeyException na inicialização se o segredo tiver menos de 256 bits
        this.chave = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Gera um JWT (access token) para o usuário
     */
    public String gerarToken(String email, String usuarioId) {
        return criarToken(email, usuarioId, TIPO_ACCESS, jwtExpiration);
    }

    /**
     * Gera um refresh token
     */
    public String gerarRefreshToken(String email, String usuarioId) {
        return criarToken(email, usuarioId, TIPO_REFRESH, refreshTokenExpiration);
    }

    /**
     * RN-005: Valida assinatura e expiração de um access token.
     * Refresh tokens são rejeitados aqui, para não poderem ser usados como access token.
     */
    public Optional<Claims> validarAccessToken(String token) {
        return validar(token, TIPO_ACCESS);
    }

    /**
     * Valida assinatura e expiração de um refresh token.
     */
    public Optional<Claims> validarRefreshToken(String token) {
        return validar(token, TIPO_REFRESH);
    }

    public String obterUsuarioId(Claims claims) {
        return claims.get(CLAIM_USUARIO_ID, String.class);
    }

    /**
     * Retorna o tempo de expiração do access token em segundos
     */
    public long obterTempoExpiracaoEmSegundos() {
        return jwtExpiration / 1000;
    }

    /**
     * Retorna o tempo de expiração do refresh token em milissegundos
     */
    public long obterExpiracaoRefreshEmMs() {
        return refreshTokenExpiration;
    }

    private Optional<Claims> validar(String token, String tipoEsperado) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String criarToken(String subject, String usuarioId, String tipo, long expirationTime) {
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + expirationTime);

        return Jwts.builder()
                // jti garante tokens distintos mesmo quando gerados no mesmo segundo
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USUARIO_ID, usuarioId)
                .claim(CLAIM_TIPO, tipo)
                .subject(subject)
                .issuedAt(agora)
                .expiration(dataExpiracao)
                .signWith(chave)
                .compact();
    }
}
