package com.authservice.service;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.dto.LoginDTO;
import com.authservice.dto.RefreshTokenDTO;
import com.authservice.dto.RegistroDTO;
import com.authservice.dto.TokenResponseDTO;
import com.authservice.dto.UsuarioResponseDTO;
import com.authservice.enums.TipoPerfil;
import com.authservice.exception.*;
import com.authservice.model.AuditoriaAutenticacao;
import com.authservice.model.RefreshToken;
import com.authservice.model.Usuario;
import com.authservice.repository.AuditoriaAutenticacaoRepository;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.UsuarioRepository;
import com.authservice.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Email ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditoriaAutenticacaoRepository auditoriaRepository;
    private final UsuarioService usuarioService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest httpRequest;

    /**
     * Registra um novo usuário com perfil USER (RN-001, RN-002, RN-003)
     */
    public UsuarioResponseDTO registrar(RegistroDTO registroDTO) {
        CriarUsuarioDTO criarDTO = new CriarUsuarioDTO(
                registroDTO.getNome(),
                registroDTO.getEmail(),
                registroDTO.getSenha(),
                true,
                TipoPerfil.USER);
        return usuarioService.criarUsuario(criarDTO);
    }

    /**
     * Autentica o usuário e retorna JWT + Refresh Token
     * RN-004: Usuário inativo não pode autenticar
     * RN-007: Toda autenticação deve ser auditada
     */
    public TokenResponseDTO autenticar(LoginDTO loginDTO) {
        String email = UsuarioService.normalizarEmail(loginDTO.getEmail());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        // Mesma resposta para email inexistente e senha errada, para não revelar quais emails existem
        if (usuario == null) {
            auditarAutenticacao(null, "LOGIN_FALHA", "Email não cadastrado: " + email);
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        if (!passwordEncoder.matches(loginDTO.getSenha(), usuario.getSenha())) {
            auditarAutenticacao(usuario.getId(), "LOGIN_FALHA", "Senha incorreta");
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        // RN-004: Verificar se usuário está ativo (só após a senha conferir)
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            auditarAutenticacao(usuario.getId(), "LOGIN_FALHA", "Usuário inativo");
            throw new UsuarioInativoException("Usuário está inativo");
        }

        String accessToken = jwtTokenProvider.gerarToken(usuario.getEmail(), usuario.getId());
        String refreshToken = jwtTokenProvider.gerarRefreshToken(usuario.getEmail(), usuario.getId());

        // Salvar refresh token no banco
        LocalDateTime agora = LocalDateTime.now();
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setId(UUID.randomUUID().toString());
        refreshTokenEntity.setUsuarioId(usuario.getId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setDataCriacao(agora);
        refreshTokenEntity.setDataExpiracao(agora.plus(jwtTokenProvider.obterExpiracaoRefreshEmMs(), ChronoUnit.MILLIS));
        refreshTokenEntity.setRevogado(false);
        refreshTokenRepository.save(refreshTokenEntity);

        // RN-007: Auditar autenticação bem-sucedida
        auditarAutenticacao(usuario.getId(), "LOGIN_SUCESSO", null);

        return criarResposta(accessToken, refreshToken);
    }

    /**
     * Atualiza o access token usando o refresh token
     * RN-005: JWT expirado deve ser rejeitado
     * RN-006: Refresh Token deve ser validado antes da emissão de novo JWT
     */
    public TokenResponseDTO refrescarToken(RefreshTokenDTO refreshTokenDTO) {
        String token = refreshTokenDTO.getRefreshToken();

        // RN-006: Validar assinatura/expiração do JWT e o registro no banco
        if (jwtTokenProvider.validarRefreshToken(token).isEmpty()) {
            throw new TokenExpiradoException("Refresh token inválido ou expirado");
        }
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenExpiradoException("Refresh token inválido"));

        if (Boolean.TRUE.equals(refreshTokenEntity.getRevogado())) {
            throw new TokenExpiradoException("Refresh token foi revogado");
        }

        if (LocalDateTime.now().isAfter(refreshTokenEntity.getDataExpiracao())) {
            throw new TokenExpiradoException("Refresh token expirou");
        }

        Usuario usuario = usuarioRepository.findById(refreshTokenEntity.getUsuarioId())
                .orElseThrow(() -> new TokenExpiradoException("Refresh token inválido"));

        // RN-004: usuário desativado após o login não pode renovar a sessão
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            auditarAutenticacao(usuario.getId(), "TOKEN_REFRESH_FALHA", "Usuário inativo");
            throw new UsuarioInativoException("Usuário está inativo");
        }

        String novoAccessToken = jwtTokenProvider.gerarToken(usuario.getEmail(), usuario.getId());

        // RN-007: Auditar refresh de token
        auditarAutenticacao(usuario.getId(), "TOKEN_REFRESH", null);

        return criarResposta(novoAccessToken, token);
    }

    /**
     * Faz logout revogando o refresh token do usuário autenticado
     * RN-007: Auditar logout
     */
    public void logout(String usuarioId, String refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .filter(rt -> usuarioId.equals(rt.getUsuarioId()))
                .orElseThrow(() -> new TokenExpiradoException("Refresh token inválido"));

        refreshTokenEntity.setRevogado(true);
        refreshTokenRepository.save(refreshTokenEntity);

        auditarAutenticacao(usuarioId, "LOGOUT", null);
    }

    private TokenResponseDTO criarResposta(String accessToken, String refreshToken) {
        TokenResponseDTO response = new TokenResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.obterTempoExpiracaoEmSegundos());
        return response;
    }

    /**
     * RN-007: Auditoria de autenticação
     */
    private void auditarAutenticacao(String usuarioId, String tipo, String descricaoErro) {
        AuditoriaAutenticacao auditoria = new AuditoriaAutenticacao();
        auditoria.setId(UUID.randomUUID().toString());
        auditoria.setUsuarioId(usuarioId);
        auditoria.setTipo(tipo);
        auditoria.setEnderecoIp(obterEnderecoIpDoRequest());
        auditoria.setUserAgent(httpRequest.getHeader("User-Agent"));
        auditoria.setDescricaoErro(descricaoErro);
        auditoria.setDataHora(LocalDateTime.now());

        auditoriaRepository.save(auditoria);
    }

    private String obterEnderecoIpDoRequest() {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            return httpRequest.getRemoteAddr();
        }
        // X-Forwarded-For pode conter uma lista "cliente, proxy1, proxy2"
        return ip.split(",")[0].trim();
    }
}
