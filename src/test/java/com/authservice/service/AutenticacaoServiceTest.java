package com.authservice.service;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.dto.LoginDTO;
import com.authservice.dto.RegistroDTO;
import com.authservice.dto.RefreshTokenDTO;
import com.authservice.dto.TokenResponseDTO;
import com.authservice.enums.TipoPerfil;
import com.authservice.exception.CredenciaisInvalidasException;
import com.authservice.exception.TokenExpiradoException;
import com.authservice.exception.UsuarioInativoException;
import com.authservice.model.AuditoriaAutenticacao;
import com.authservice.model.RefreshToken;
import com.authservice.model.Usuario;
import com.authservice.repository.AuditoriaAutenticacaoRepository;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.UsuarioRepository;
import com.authservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutenticacaoServiceTest {

    private static final String SENHA = "Senha123";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("chave-de-teste-com-pelo-menos-32-bytes!!", 60_000, 120_000);

    private UsuarioRepository usuarioRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AuditoriaAutenticacaoRepository auditoriaRepository;
    private UsuarioService usuarioService;
    private AutenticacaoService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        auditoriaRepository = mock(AuditoriaAutenticacaoRepository.class);
        usuarioService = mock(UsuarioService.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");

        service = new AutenticacaoService(usuarioRepository, refreshTokenRepository, auditoriaRepository,
                usuarioService, jwtTokenProvider, passwordEncoder, request);

        usuario = new Usuario();
        usuario.setId("u-1");
        usuario.setEmail("joao@example.com");
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setAtivo(true);
    }

    @Test
    void loginComSucessoGeraTokensSalvaRefreshEAudita() {
        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));

        TokenResponseDTO resposta = service.autenticar(new LoginDTO("  Joao@Example.com ", SENHA));

        assertThat(jwtTokenProvider.validarAccessToken(resposta.getAccessToken())).isPresent();
        assertThat(jwtTokenProvider.validarRefreshToken(resposta.getRefreshToken())).isPresent();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(auditoriaSalva().getTipo()).isEqualTo("LOGIN_SUCESSO");
        assertThat(auditoriaSalva().getEnderecoIp()).isEqualTo("203.0.113.7");
    }

    @Test
    void senhaErradaLancaCredenciaisInvalidasEAudita() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.autenticar(new LoginDTO("joao@example.com", "Errada123")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(auditoriaSalva().getTipo()).isEqualTo("LOGIN_FALHA");
    }

    @Test
    void emailInexistenteTemMesmaRespostaQueSenhaErrada() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autenticar(new LoginDTO("x@example.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Email ou senha inválidos");
        assertThat(auditoriaSalva().getTipo()).isEqualTo("LOGIN_FALHA");
    }

    @Test
    void usuarioInativoNaoAutentica() {
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.autenticar(new LoginDTO("joao@example.com", SENHA)))
                .isInstanceOf(UsuarioInativoException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshValidoGeraNovoAccessToken() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(refreshSalvo(refresh, false)));
        when(usuarioRepository.findById("u-1")).thenReturn(Optional.of(usuario));

        TokenResponseDTO resposta = service.refrescarToken(new RefreshTokenDTO(refresh));

        assertThat(jwtTokenProvider.validarAccessToken(resposta.getAccessToken())).isPresent();
    }

    @Test
    void refreshRevogadoERejeitado() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(refreshSalvo(refresh, true)));

        assertThatThrownBy(() -> service.refrescarToken(new RefreshTokenDTO(refresh)))
                .isInstanceOf(TokenExpiradoException.class);
    }

    @Test
    void accessTokenNaoPodeSerUsadoComoRefresh() {
        String access = jwtTokenProvider.gerarToken(usuario.getEmail(), usuario.getId());

        assertThatThrownBy(() -> service.refrescarToken(new RefreshTokenDTO(access)))
                .isInstanceOf(TokenExpiradoException.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logoutComRefreshDeOutroUsuarioERejeitado() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(refreshSalvo(refresh, false)));

        assertThatThrownBy(() -> service.logout("outro-usuario", refresh))
                .isInstanceOf(TokenExpiradoException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logoutRevogaRefreshToken() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        RefreshToken salvo = refreshSalvo(refresh, false);
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(salvo));

        service.logout("u-1", refresh);

        assertThat(salvo.getRevogado()).isTrue();
        verify(refreshTokenRepository).save(salvo);
        assertThat(auditoriaSalva().getTipo()).isEqualTo("LOGOUT");
    }

    @Test
    void registrarDelegaComPerfilUser() {
        service.registrar(new RegistroDTO("João", "joao@example.com", SENHA));

        ArgumentCaptor<CriarUsuarioDTO> captor = ArgumentCaptor.forClass(CriarUsuarioDTO.class);
        verify(usuarioService).criarUsuario(captor.capture());
        assertThat(captor.getValue().getPerfil()).isEqualTo(TipoPerfil.USER);
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    @Test
    void refreshValidoMasAusenteNoBancoERejeitado() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refrescarToken(new RefreshTokenDTO(refresh)))
                .isInstanceOf(TokenExpiradoException.class);
    }

    @Test
    void refreshExpiradoNoBancoERejeitado() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        RefreshToken salvo = refreshSalvo(refresh, false);
        salvo.setDataExpiracao(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(salvo));

        assertThatThrownBy(() -> service.refrescarToken(new RefreshTokenDTO(refresh)))
                .isInstanceOf(TokenExpiradoException.class)
                .hasMessage("Refresh token expirou");
    }

    @Test
    void refreshDeUsuarioRemovidoERejeitado() {
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(refreshSalvo(refresh, false)));
        when(usuarioRepository.findById("u-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refrescarToken(new RefreshTokenDTO(refresh)))
                .isInstanceOf(TokenExpiradoException.class);
    }

    @Test
    void refreshDeUsuarioInativoERejeitadoEAuditado() {
        usuario.setAtivo(false);
        String refresh = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());
        when(refreshTokenRepository.findByToken(refresh)).thenReturn(Optional.of(refreshSalvo(refresh, false)));
        when(usuarioRepository.findById("u-1")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.refrescarToken(new RefreshTokenDTO(refresh)))
                .isInstanceOf(UsuarioInativoException.class);
        assertThat(auditoriaSalva().getTipo()).isEqualTo("TOKEN_REFRESH_FALHA");
    }

    @Test
    void semXForwardedForUsaRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        service = new AutenticacaoService(usuarioRepository, refreshTokenRepository, auditoriaRepository,
                usuarioService, jwtTokenProvider, passwordEncoder, request);
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autenticar(new LoginDTO("x@example.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(auditoriaSalva().getEnderecoIp()).isEqualTo("192.0.2.10");
        assertThat(auditoriaSalva().getUsuarioId()).isNull();
    }

    private RefreshToken refreshSalvo(String token, boolean revogado) {
        RefreshToken rt = new RefreshToken();
        rt.setId("rt-1");
        rt.setUsuarioId("u-1");
        rt.setToken(token);
        rt.setRevogado(revogado);
        rt.setDataExpiracao(LocalDateTime.now().plusDays(1));
        return rt;
    }

    private AuditoriaAutenticacao auditoriaSalva() {
        ArgumentCaptor<AuditoriaAutenticacao> captor = ArgumentCaptor.forClass(AuditoriaAutenticacao.class);
        verify(auditoriaRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
