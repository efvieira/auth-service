package com.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import java.time.LocalDateTime;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private String id;

    private String usuarioId;

    private String token;

    private LocalDateTime dataExpiracao;

    private LocalDateTime dataCriacao;

    private Boolean revogado = false;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "token-index")
    @DynamoDbAttribute("token")
    public String getToken() {
        return token;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "usuario-index")
    @DynamoDbAttribute("usuario_id")
    public String getUsuarioId() {
        return usuarioId;
    }

    @DynamoDbAttribute("data_expiracao")
    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    @DynamoDbAttribute("data_criacao")
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    @DynamoDbAttribute("revogado")
    public Boolean getRevogado() {
        return revogado;
    }
}
