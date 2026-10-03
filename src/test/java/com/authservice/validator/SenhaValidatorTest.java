package com.authservice.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SenhaValidatorTest {

    @Test
    void aceitaSenhaQueAtendeRN003() {
        assertThat(SenhaValidator.isValida("Senha123")).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Sen123", "senha1234", "SenhaForte"})
    void rejeitaSenhaFraca(String senha) {
        assertThat(SenhaValidator.isValida(senha)).isFalse();
    }

    @Test
    void mensagemIndicaOMotivo() {
        assertThat(SenhaValidator.obterMensagemErro("Sen123")).contains("8 caracteres");
        assertThat(SenhaValidator.obterMensagemErro("senha1234")).contains("maiúscula");
        assertThat(SenhaValidator.obterMensagemErro("SenhaForte")).contains("número");
        assertThat(SenhaValidator.obterMensagemErro(null)).contains("nula");
        assertThat(SenhaValidator.obterMensagemErro("Senha123")).isEqualTo("Senha válida");
    }
}
