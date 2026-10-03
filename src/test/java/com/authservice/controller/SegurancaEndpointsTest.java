package com.authservice.controller;

import com.authservice.config.SecurityConfig;
import com.authservice.dto.TokenResponseDTO;
import com.authservice.dto.UsuarioResponseDTO;
import com.authservice.enums.TipoPerfil;
import com.authservice.handler.ErrorResponseWriter;
import com.authservice.model.Perfil;
import com.authservice.model.Usuario;
import com.authservice.repository.PerfilRepository;
import com.authservice.repository.UsuarioRepository;
import com.authservice.security.JwtTokenProvider;
import com.authservice.service.AutenticacaoService;
import com.authservice.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica as regras de acesso da API com o SecurityConfig e o filtro JWT reais.
 */
@WebMvcTest(controllers = {AutenticacaoController.class, UsuarioController.class})
@Import({SecurityConfig.class, ErrorResponseWriter.class, JwtTokenProvider.class})
class SegurancaEndpointsTest {

    private static final String NOVO_USUARIO_JSON =
            "{\"nome\":\"Maria\",\"email\":\"maria@example.com\",\"senha\":\"Senha123\",\"perfil\":\"USER\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AutenticacaoService autenticacaoService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private UsuarioRepository usuarioRepository;
    @MockBean
    private PerfilRepository perfilRepository;

    @BeforeEach
    void setUp() {
        cadastrar("admin-1", "admin@example.com", "perfil-admin", TipoPerfil.ADMIN, true);
        cadastrar("user-1", "joao@example.com", "perfil-user", TipoPerfil.USER, true);
        cadastrar("inativo-1", "inativo@example.com", "perfil-user", TipoPerfil.USER, false);
        when(usuarioService.obterPorId(anyString()))
                .thenAnswer(inv -> Optional.of(new UsuarioResponseDTO(inv.getArgument(0), "Nome", "x@example.com", true, null)));
    }

    @Test
    void loginEPublico() throws Exception {
        when(autenticacaoService.autenticar(any())).thenReturn(new TokenResponseDTO("a", "r", "Bearer", 3600));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@example.com\",\"senha\":\"Senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a"));
    }

    @Test
    void registroComDadosInvalidosRetorna400ComErrosPorCampo() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"Senha123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.nome").exists());
    }

    @Test
    void swaggerEPublico() throws Exception {
        // O springdoc não é carregado neste slice de teste: basta a segurança não bloquear (401)
        mockMvc.perform(get("/api-docs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void headerXUsuarioIdNaoAutenticaMais() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("X-Usuario-Id", "admin-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOVO_USUARIO_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNaoServeComoBearer() throws Exception {
        String refresh = jwtTokenProvider.gerarRefreshToken("joao@example.com", "user-1");

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInativoComTokenAindaValidoRecebe401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", bearer("inativo-1", "inativo@example.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userNaoPodeCriarUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", bearer("user-1", "joao@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOVO_USUARIO_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void adminPodeCriarUsuario() throws Exception {
        when(usuarioService.criarUsuario(any()))
                .thenReturn(new UsuarioResponseDTO("novo", "Maria", "maria@example.com", true, null));

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", bearer("admin-1", "admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOVO_USUARIO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("novo"));
    }

    @Test
    void userConsultaApenasOProprioPerfil() throws Exception {
        String token = bearer("user-1", "joao@example.com");

        mockMvc.perform(get("/api/usuarios/user-1").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios/admin-1").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios/email/admin@example.com").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsultaQualquerPerfil() throws Exception {
        mockMvc.perform(get("/api/usuarios/user-1").header("Authorization", bearer("admin-1", "admin@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void registroValidoRetorna201() throws Exception {
        when(autenticacaoService.registrar(any()))
                .thenReturn(new UsuarioResponseDTO("novo", "João", "joao2@example.com", true, null));

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"João\",\"email\":\"joao2@example.com\",\"senha\":\"Senha123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("novo"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void corpoMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nao-e-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshEPublico() throws Exception {
        when(autenticacaoService.refrescarToken(any())).thenReturn(new TokenResponseDTO("novo", "r", "Bearer", 3600));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"r\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("novo"));
    }

    @Test
    void logoutExigeTokenEUsaUsuarioDoToken() throws Exception {
        String corpo = "{\"refreshToken\":\"r\"}";

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer("user-1", "joao@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Logout realizado com sucesso"));
        verify(autenticacaoService).logout("user-1", "r");
    }

    @Test
    void meRetornaUsuarioDoToken() throws Exception {
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", bearer("user-1", "joao@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"));
    }

    @Test
    void usuarioInexistenteRetorna404() throws Exception {
        when(usuarioService.obterPorId("sumiu")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/usuarios/sumiu").header("Authorization", bearer("admin-1", "admin@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultaPorEmail() throws Exception {
        when(usuarioService.obterPorEmail("joao@example.com"))
                .thenReturn(Optional.of(new UsuarioResponseDTO("user-1", "João", "joao@example.com", true, null)));

        // USER pode consultar o próprio email (sem diferenciar maiúsculas)
        mockMvc.perform(get("/api/usuarios/email/JOAO@example.com").header("Authorization", bearer("user-1", "joao@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"));

        // ADMIN consulta qualquer email; inexistente dá 404
        mockMvc.perform(get("/api/usuarios/email/ninguem@example.com").header("Authorization", bearer("admin-1", "admin@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioSemPerfilAutenticaMasNaoEAdmin() throws Exception {
        Usuario semPerfil = new Usuario();
        semPerfil.setId("sem-perfil");
        semPerfil.setEmail("sp@example.com");
        semPerfil.setAtivo(true);
        when(usuarioRepository.findById("sem-perfil")).thenReturn(Optional.of(semPerfil));

        String token = bearer("sem-perfil", "sp@example.com");
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/usuarios").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_USUARIO_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void headerAuthorizationSemBearerEIgnorado() throws Exception {
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Basic dXNlcjpzZW5oYQ=="))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(String usuarioId, String email) {
        return "Bearer " + jwtTokenProvider.gerarToken(email, usuarioId);
    }

    private void cadastrar(String id, String email, String perfilId, TipoPerfil tipo, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setAtivo(ativo);
        usuario.setPerfilId(perfilId);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        Perfil perfil = new Perfil(perfilId, tipo, tipo.getDescricao(), true);
        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
    }
}
