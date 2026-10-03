package com.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import com.authservice.enums.TipoPerfil;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Perfil {

    private String id;

    private TipoPerfil tipo;

    private String descricao;

    private Boolean ativo = true;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "tipo-index")
    @DynamoDbAttribute("tipo")
    public TipoPerfil getTipo() {
        return tipo;
    }

    @DynamoDbAttribute("descricao")
    public String getDescricao() {
        return descricao;
    }

    @DynamoDbAttribute("ativo")
    public Boolean getAtivo() {
        return ativo;
    }
}
