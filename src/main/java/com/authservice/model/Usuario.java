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
public class Usuario {

    private String id;

    private String email;

    private String senha;

    private String nome;

    private Boolean ativo = true;

    private String perfilId;

    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    @DynamoDbAttribute("senha")
    public String getSenha() {
        return senha;
    }

    @DynamoDbAttribute("nome")
    public String getNome() {
        return nome;
    }

    @DynamoDbAttribute("ativo")
    public Boolean getAtivo() {
        return ativo;
    }

    @DynamoDbAttribute("perfil_id")
    public String getPerfilId() {
        return perfilId;
    }

    @DynamoDbAttribute("data_criacao")
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    @DynamoDbAttribute("data_atualizacao")
    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
