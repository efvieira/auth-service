package com.authservice.dto;

import com.authservice.enums.TipoPerfil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para criar novo usuário (apenas ADMIN)")
public class CriarUsuarioDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Nome completo do usuário", example = "Maria Silva")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Schema(description = "Email do usuário (único)", example = "maria@example.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha (mín 8 chars, 1 maiúscula, 1 número)", example = "Senha123")
    private String senha;

    @Schema(description = "Status do usuário", example = "true", defaultValue = "true")
    private Boolean ativo;

    @Schema(description = "Tipo de perfil/role", example = "USER", allowableValues = {"ADMIN", "USER"})
    private TipoPerfil perfil; // Se não informado, usa USER como padrão
}

