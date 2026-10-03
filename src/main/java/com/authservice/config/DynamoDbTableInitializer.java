package com.authservice.config;

import com.authservice.repository.AuditoriaAutenticacaoRepository;
import com.authservice.repository.PerfilRepository;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import java.util.Arrays;

/**
 * Cria as tabelas do DynamoDB (com seus GSIs) caso ainda não existam.
 * Roda antes dos demais inicializadores. Desative com aws.dynamodb.criar-tabelas=false
 * quando as tabelas forem gerenciadas por infraestrutura (Terraform, CloudFormation...).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.dynamodb.criar-tabelas", havingValue = "true", matchIfMissing = true)
public class DynamoDbTableInitializer implements CommandLineRunner {

    private static final ProvisionedThroughput THROUGHPUT = ProvisionedThroughput.builder()
            .readCapacityUnits(2L)
            .writeCapacityUnits(2L)
            .build();

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditoriaAutenticacaoRepository auditoriaRepository;

    @Override
    public void run(String... args) {
        criarSeNaoExistir(perfilRepository.getTable(), "tipo-index");
        criarSeNaoExistir(usuarioRepository.getTable(), "email-index");
        criarSeNaoExistir(refreshTokenRepository.getTable(), "token-index", "usuario-index");
        criarSeNaoExistir(auditoriaRepository.getTable(), "usuario-index");
    }

    private void criarSeNaoExistir(DynamoDbTable<?> table, String... indices) {
        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                .provisionedThroughput(THROUGHPUT)
                .globalSecondaryIndices(Arrays.stream(indices)
                        .map(nome -> EnhancedGlobalSecondaryIndex.builder()
                                .indexName(nome)
                                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                .provisionedThroughput(THROUGHPUT)
                                .build())
                        .toList())
                .build();
        try {
            table.createTable(request);
            log.info("Tabela DynamoDB '{}' criada", table.tableName());
        } catch (ResourceInUseException e) {
            log.debug("Tabela DynamoDB '{}' já existe", table.tableName());
        }
    }
}
