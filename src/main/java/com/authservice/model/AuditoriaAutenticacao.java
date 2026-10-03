package com.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import java.time.LocalDateTime;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaAutenticacao {

    private String id;

    private String usuarioId;

    private String tipo; // LOGIN_SUCESSO, LOGIN_FALHA, LOGOUT, TOKEN_REFRESH

    private String enderecoIp;

    private String userAgent;

    private String descricaoErro;

    private LocalDateTime dataHora;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "usuario-index")
    @DynamoDbAttribute("usuario_id")
    public String getUsuarioId() {
        return usuarioId;
    }

    @DynamoDbSecondarySortKey(indexNames = "usuario-index")
    @DynamoDbAttribute("data_hora")
    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @DynamoDbAttribute("tipo")
    public String getTipo() {
        return tipo;
    }

    @DynamoDbAttribute("endereco_ip")
    public String getEnderecoIp() {
        return enderecoIp;
    }

    @DynamoDbAttribute("user_agent")
    public String getUserAgent() {
        return userAgent;
    }

    @DynamoDbAttribute("descricao_erro")
    public String getDescricaoErro() {
        return descricaoErro;
    }
}
