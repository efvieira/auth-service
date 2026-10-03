package com.authservice.controller;

import com.authservice.dto.LoginDTO;
import com.authservice.dto.RefreshTokenDTO;
import com.authservice.dto.RegistroDTO;
import com.authservice.dto.TokenResponseDTO;
import com.authservice.dto.UsuarioResponseDTO;
import com.authservice.security.UsuarioAutenticado;
import com.authservice.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação e autorização de usuários")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    /**
     * Registra um novo usuário com perfil USER
     */
    @PostMapping("/registrar")
    @Operation(
        summary = "Registrar novo usuário",
        description = "Cria uma nova conta de usuário com perfil USER. A senha deve conter no mínimo 8 caracteres, 1 letra maiúscula e 1 número."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuário registrado com sucesso",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))
        ),
        @ApiResponse(responseCode = "409", description = "Email já existe", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha fraca", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroDTO registroDTO) {
        UsuarioResponseDTO usuario = autenticacaoService.registrar(registroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    /**
     * Autentica o usuário e retorna JWT + Refresh Token
     */
    @PostMapping("/login")
    @Operation(
        summary = "Fazer login",
        description = "Autentica o usuário com email e senha, retornando Access Token e Refresh Token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login realizado com sucesso",
            content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Email ou senha inválidos", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Usuário inativo", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(autenticacaoService.autenticar(loginDTO));
    }

    /**
     * Atualiza o access token usando refresh token
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar Access Token",
        description = "Usa o Refresh Token para gerar um novo Access Token sem necessidade de re-autenticar"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token renovado com sucesso",
            content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Refresh Token inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Usuário inativo", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<TokenResponseDTO> refresh(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        return ResponseEntity.ok(autenticacaoService.refrescarToken(refreshTokenDTO));
    }

    /**
     * Faz logout revogando o refresh token do usuário autenticado
     */
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Fazer logout",
        description = "Revoga o Refresh Token do usuário autenticado (Bearer) e encerra a sessão"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Access Token ou Refresh Token inválido", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        autenticacaoService.logout(autenticado.id(), refreshTokenDTO.getRefreshToken());
        return ResponseEntity.ok(Map.of("mensagem", "Logout realizado com sucesso"));
    }
}
