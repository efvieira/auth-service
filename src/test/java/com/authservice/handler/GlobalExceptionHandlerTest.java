package com.authservice.handler;

import com.authservice.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapeiaExcecoesDeNegocioParaOStatusCorreto() {
        assertStatus(handler.handleEmailJaExiste(new EmailJaExisteException("x")), HttpStatus.CONFLICT, "x");
        assertStatus(handler.handleSenhaInvalida(new SenhaInvalidaException("x")), HttpStatus.BAD_REQUEST, "x");
        assertStatus(handler.handleUsuarioNaoEncontrado(new UsuarioNaoEncontradoException("x")), HttpStatus.NOT_FOUND, "x");
        assertStatus(handler.handleCredenciaisInvalidas(new CredenciaisInvalidasException("x")), HttpStatus.UNAUTHORIZED, "x");
        assertStatus(handler.handleUsuarioInativo(new UsuarioInativoException("x")), HttpStatus.FORBIDDEN, "x");
        assertStatus(handler.handleTokenExpirado(new TokenExpiradoException("x")), HttpStatus.UNAUTHORIZED, "x");
        assertStatus(handler.handlePermissaoNegada(new PermissaoNegadaException("x")), HttpStatus.FORBIDDEN, "x");
    }

    @Test
    void mapeiaExcecoesDoSpring() {
        assertStatus(handler.handleAccessDenied(new AccessDeniedException("negado")),
                HttpStatus.FORBIDDEN, "Acesso negado");
        assertStatus(handler.handleCorpoInvalido(new HttpMessageNotReadableException("json", new MockHttpInputMessage(new byte[0]))),
                HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou ausente");
        assertThat(handler.handleMetodoNaoSuportado(new HttpRequestMethodNotSupportedException("DELETE")).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void erroInesperadoNaoVazaDetalhes() {
        assertStatus(handler.handleException(new IllegalStateException("detalhe interno")),
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
    }

    private void assertStatus(ResponseEntity<Map<String, Object>> response, HttpStatus status, String mensagem) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody())
                .containsEntry("status", status.value())
                .containsEntry("mensagem", mensagem)
                .containsKey("timestamp");
    }
}
