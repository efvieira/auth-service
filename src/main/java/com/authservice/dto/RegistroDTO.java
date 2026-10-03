package com.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request de Registro de novo usuário")
public class RegistroDTO {
    
    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String nome;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Schema(description = "Email do usuário (único)", example = "joao@example.com")
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha (mín 8 chars, 1 maiúscula, 1 número)", example = "Senha123")
    private String senha;
}

