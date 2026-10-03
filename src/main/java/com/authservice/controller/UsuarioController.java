package com.authservice.controller;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.dto.UsuarioResponseDTO;
import com.authservice.exception.PermissaoNegadaException;
import com.authservice.exception.UsuarioNaoEncontradoException;
import com.authservice.security.UsuarioAutenticado;
import com.authservice.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de usuários (exigem access token)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Cria um novo usuário (apenas ADMIN pode criar)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Criar novo usuário",
        description = "Cria um novo usuário no sistema. Apenas usuários com perfil ADMIN podem realizar esta ação."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuário criado com sucesso",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas ADMIN pode criar usuários", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Email já existe", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha fraca", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<UsuarioResponseDTO> criarUsuario(@Valid @RequestBody CriarUsuarioDTO criarDTO) {
        UsuarioResponseDTO usuario = usuarioService.criarUsuario(criarDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    /**
     * Retorna o perfil do próprio usuário autenticado
     */
    @GetMapping("/me")
    @Operation(summary = "Obter meu perfil", description = "Retorna os dados do usuário dono do access token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário autenticado",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<UsuarioResponseDTO> obterMeuPerfil(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(buscarPorId(autenticado.id()));
    }

    /**
     * Obtém um usuário por ID.
     * USER só pode consultar o próprio perfil; ADMIN pode consultar qualquer um.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Obter usuário por ID",
        description = "Retorna os dados de um usuário específico. Usuários USER só podem consultar seu próprio perfil, enquanto ADMIN podem consultar qualquer usuário."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Acesso negado - USER só pode consultar seu próprio perfil", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<UsuarioResponseDTO> obterPorId(
            @Parameter(description = "ID do usuário a consultar") @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        verificarAcesso(autenticado, id);
        return ResponseEntity.ok(buscarPorId(id));
    }

    /**
     * Obtém um usuário por email.
     * USER só pode consultar o próprio email; ADMIN pode consultar qualquer um.
     */
    @GetMapping("/email/{email}")
    @Operation(
        summary = "Obter usuário por email",
        description = "Retorna os dados de um usuário pelo seu email. Usuários USER só podem consultar seu próprio email, enquanto ADMIN podem consultar qualquer email."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Acesso negado - USER só pode consultar seu próprio email", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<UsuarioResponseDTO> obterPorEmail(
            @Parameter(description = "Email do usuário a consultar") @PathVariable String email,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        // Verifica antes de consultar, para USER não descobrir quais emails existem
        if (!autenticado.isAdmin() && !autenticado.email().equalsIgnoreCase(email.trim())) {
            throw new PermissaoNegadaException("Acesso negado. Você só pode consultar o seu próprio perfil.");
        }

        return usuarioService.obterPorEmail(UsuarioService.normalizarEmail(email))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
    }

    private void verificarAcesso(UsuarioAutenticado autenticado, String usuarioIdConsultado) {
        if (!autenticado.isAdmin() && !autenticado.id().equals(usuarioIdConsultado)) {
            throw new PermissaoNegadaException("Acesso negado. Você só pode consultar o seu próprio perfil.");
        }
    }

    private UsuarioResponseDTO buscarPorId(String id) {
        return usuarioService.obterPorId(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
    }
}
