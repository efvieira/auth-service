package com.authservice.dto;

import com.authservice.enums.TipoPerfil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDTO {
    private String id;
    private TipoPerfil tipo;
    private String descricao;
    private Boolean ativo;
}
