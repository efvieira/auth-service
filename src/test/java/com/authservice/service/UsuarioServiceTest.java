package com.authservice.service;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.dto.UsuarioResponseDTO;
import com.authservice.enums.TipoPerfil;
import com.authservice.exception.EmailJaExisteException;
import com.authservice.exception.SenhaInvalidaException;
import com.authservice.model.Perfil;
import com.authservice.model.Usuario;
import com.authservice.repository.PerfilRepository;
import com.authservice.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Perfil perfilUser = new Perfil("p-user", TipoPerfil.USER, "Usuário", true);
    private final Perfil perfilAdmin = new Perfil("p-admin", TipoPerfil.ADMIN, "Administrador", true);

    private UsuarioRepository usuarioRepository;
    private PerfilRepository perfilRepository;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        perfilRepository = mock(PerfilRepository.class);
        service = new UsuarioService(usuarioRepository, perfilRepository, passwordEncoder);

        when(perfilRepository.findByTipo(TipoPerfil.USER)).thenReturn(Optional.of(perfilUser));
        when(perfilRepository.findByTipo(TipoPerfil.ADMIN)).thenReturn(Optional.of(perfilAdmin));
        when(perfilRepository.findById("p-user")).thenReturn(Optional.of(perfilUser));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criaUsuarioComSenhaCriptografadaEmailNormalizadoEDatas() {
        UsuarioResponseDTO dto = service.criarUsuario(
                new CriarUsuarioDTO("Maria", " Maria@Example.COM ", "Senha123", null, TipoPerfil.ADMIN));

        Usuario salvo = usuarioSalvo();
        assertThat(salvo.getEmail()).isEqualTo("maria@example.com");
        assertThat(passwordEncoder.matches("Senha123", salvo.getSenha())).isTrue();
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(salvo.getPerfilId()).isEqualTo("p-admin");
        assertThat(salvo.getDataCriacao()).isNotNull().isEqualTo(salvo.getDataAtualizacao());

        assertThat(dto.getId()).isEqualTo(salvo.getId());
        assertThat(dto.getPerfil().getTipo()).isEqualTo(TipoPerfil.ADMIN);
    }

    @Test
    void perfilPadraoEUserERespeitaAtivoInformado() {
        service.criarUsuario(new CriarUsuarioDTO("Maria", "maria@example.com", "Senha123", false, null));

        assertThat(usuarioSalvo().getPerfilId()).isEqualTo("p-user");
        assertThat(usuarioSalvo().getAtivo()).isFalse();
    }

    @Test
    void emailDuplicadoERejeitado() {
        when(usuarioRepository.existsByEmail("maria@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criarUsuario(
                new CriarUsuarioDTO("Maria", "MARIA@example.com", "Senha123", true, null)))
                .isInstanceOf(EmailJaExisteException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaFracaERejeitada() {
        assertThatThrownBy(() -> service.criarUsuario(
                new CriarUsuarioDTO("Maria", "maria@example.com", "fraca", true, null)))
                .isInstanceOf(SenhaInvalidaException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void perfilInexistenteFalha() {
        when(perfilRepository.findByTipo(TipoPerfil.USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarUsuario(
                new CriarUsuarioDTO("Maria", "maria@example.com", "Senha123", true, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void obterPorIdConverteComPerfil() {
        when(usuarioRepository.findById("u-1")).thenReturn(Optional.of(usuario("u-1", "p-user")));

        assertThat(service.obterPorId("u-1"))
                .hasValueSatisfying(dto -> assertThat(dto.getPerfil().getTipo()).isEqualTo(TipoPerfil.USER));
        assertThat(service.obterPorId("inexistente")).isEmpty();
    }

    @Test
    void obterPorEmailSemPerfilOuComPerfilRemovido() {
        when(usuarioRepository.findByEmail("a@example.com")).thenReturn(Optional.of(usuario("u-1", null)));
        when(usuarioRepository.findByEmail("b@example.com")).thenReturn(Optional.of(usuario("u-2", "apagado")));

        assertThat(service.obterPorEmail("a@example.com")).hasValueSatisfying(dto -> assertThat(dto.getPerfil()).isNull());
        assertThat(service.obterPorEmail("b@example.com")).hasValueSatisfying(dto -> assertThat(dto.getPerfil()).isNull());
    }

    @Test
    void existePorEmailNormaliza() {
        when(usuarioRepository.existsByEmail("joao@example.com")).thenReturn(true);

        assertThat(service.existePorEmail(" JOAO@example.com")).isTrue();
    }

    @Test
    void normalizarEmailAceitaNull() {
        assertThat(UsuarioService.normalizarEmail(null)).isNull();
    }

    private Usuario usuario(String id, String perfilId) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setPerfilId(perfilId);
        usuario.setAtivo(true);
        return usuario;
    }

    private Usuario usuarioSalvo() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
