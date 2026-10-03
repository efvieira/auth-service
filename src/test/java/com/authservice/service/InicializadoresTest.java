package com.authservice.service;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.enums.TipoPerfil;
import com.authservice.model.Perfil;
import com.authservice.repository.PerfilRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InicializadoresTest {

    @Test
    void perfilInitializerCriaPerfisAusentes() {
        PerfilRepository repo = mock(PerfilRepository.class);

        new PerfilInitializer(repo).run();

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Perfil::getTipo)
                .containsExactly(TipoPerfil.ADMIN, TipoPerfil.USER);
        assertThat(captor.getAllValues()).allSatisfy(p -> {
            assertThat(p.getId()).isNotBlank();
            assertThat(p.getAtivo()).isTrue();
        });
    }

    @Test
    void perfilInitializerNaoDuplicaPerfisExistentes() {
        PerfilRepository repo = mock(PerfilRepository.class);
        when(repo.existsByTipo(any())).thenReturn(true);

        new PerfilInitializer(repo).run();

        verify(repo, never()).save(any());
    }

    @Test
    void adminInitializerCriaAdminQuandoNaoExiste() {
        UsuarioService usuarioService = mock(UsuarioService.class);

        new AdminInitializer(usuarioService, "admin@example.com", "AdminSenha123", "Admin").run();

        ArgumentCaptor<CriarUsuarioDTO> captor = ArgumentCaptor.forClass(CriarUsuarioDTO.class);
        verify(usuarioService).criarUsuario(captor.capture());
        assertThat(captor.getValue().getPerfil()).isEqualTo(TipoPerfil.ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    @Test
    void adminInitializerNaoRecriaAdminExistente() {
        UsuarioService usuarioService = mock(UsuarioService.class);
        when(usuarioService.existePorEmail("admin@example.com")).thenReturn(true);

        new AdminInitializer(usuarioService, "admin@example.com", "AdminSenha123", "Admin").run();

        verify(usuarioService, never()).criarUsuario(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void adminInitializerDesativadoSemEmail(String email) {
        UsuarioService usuarioService = mock(UsuarioService.class);

        new AdminInitializer(usuarioService, email, "AdminSenha123", "Admin").run();

        verifyNoInteractions(usuarioService);
    }
}
