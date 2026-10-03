package com.authservice.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "chave-de-teste-com-pelo-menos-32-bytes!!";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 120_000);

    @Test
    void accessTokenValidoRetornaUsuarioId() {
        String token = provider.gerarToken("joao@example.com", "u-1");

        assertThat(provider.validarAccessToken(token))
                .map(provider::obterUsuarioId)
                .contains("u-1");
    }

    @Test
    void refreshTokenNaoServeComoAccessToken() {
        String refresh = provider.gerarRefreshToken("joao@example.com", "u-1");

        assertThat(provider.validarAccessToken(refresh)).isEmpty();
        assertThat(provider.validarRefreshToken(refresh)).isPresent();
    }

    @Test
    void accessTokenNaoServeComoRefreshToken() {
        String access = provider.gerarToken("joao@example.com", "u-1");

        assertThat(provider.validarRefreshToken(access)).isEmpty();
    }

    @Test
    void rejeitaTokenExpirado() {
        JwtTokenProvider expirado = new JwtTokenProvider(SECRET, -1_000, -1_000);
        String token = expirado.gerarToken("joao@example.com", "u-1");

        assertThat(provider.validarAccessToken(token)).isEmpty();
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        JwtTokenProvider outro = new JwtTokenProvider("outra-chave-de-teste-com-32-bytes-ou-mais", 60_000, 60_000);
        String token = outro.gerarToken("joao@example.com", "u-1");

        assertThat(provider.validarAccessToken(token)).isEmpty();
    }

    @Test
    void rejeitaLixo() {
        assertThat(provider.validarAccessToken("nao-e-um-jwt")).isEmpty();
        assertThat(provider.validarAccessToken("")).isEmpty();
    }

    @Test
    void tokensGeradosNoMesmoInstanteSaoDiferentes() {
        assertThat(provider.gerarRefreshToken("a@b.com", "u-1"))
                .isNotEqualTo(provider.gerarRefreshToken("a@b.com", "u-1"));
    }

    @Test
    void segredoCurtoFalhaNaInicializacao() {
        assertThatThrownBy(() -> new JwtTokenProvider("curto", 1, 1))
                .isInstanceOf(WeakKeyException.class);
    }
}
