package com.authservice.config;

import com.authservice.repository.AuditoriaAutenticacaoRepository;
import com.authservice.repository.PerfilRepository;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.UsuarioRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class ConfiguracoesTest {

    @Test
    void awsDynamoConfigApontaParaEndpointERegiaoConfigurados() {
        AwsDynamoConfig config = new AwsDynamoConfig();

        try (DynamoDbClient client = config.dynamoDbClient("sa-east-1", "http://localhost:4566", "k", "s")) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.SA_EAST_1);
            assertThat(client.serviceClientConfiguration().endpointOverride())
                    .hasValueSatisfying(uri -> assertThat(uri.toString()).isEqualTo("http://localhost:4566"));
            assertThat(config.dynamoDbEnhancedClient(client)).isInstanceOf(DynamoDbEnhancedClient.class);
        }
    }

    @Test
    void swaggerDeclaraEsquemaBearer() {
        OpenAPI openAPI = new SwaggerConfig().customOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Auth Service API");
        SecurityScheme bearer = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(bearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearer.getScheme()).isEqualTo("bearer");
    }

    @Test
    void tableInitializerCriaAsQuatroTabelasComSeusIndices() {
        DynamoDbTable<?> perfis = tabela("perfis");
        DynamoDbTable<?> usuarios = tabela("usuarios");
        DynamoDbTable<?> refresh = tabela("refresh_tokens");
        DynamoDbTable<?> auditoria = tabela("auditoria_autenticacao");

        inicializador(perfis, usuarios, refresh, auditoria).run();

        assertThat(indicesCriados(perfis)).containsExactly("tipo-index");
        assertThat(indicesCriados(usuarios)).containsExactly("email-index");
        assertThat(indicesCriados(refresh)).containsExactly("token-index", "usuario-index");
        assertThat(indicesCriados(auditoria)).containsExactly("usuario-index");
    }

    @Test
    void tableInitializerIgnoraTabelaQueJaExiste() {
        DynamoDbTable<?> perfis = tabela("perfis");
        doThrow(ResourceInUseException.builder().message("já existe").build())
                .when(perfis).createTable(any(CreateTableEnhancedRequest.class));
        DynamoDbTable<?> usuarios = tabela("usuarios");

        DynamoDbTableInitializer init = inicializador(perfis, usuarios, tabela("refresh_tokens"), tabela("auditoria_autenticacao"));

        assertThatCode(init::run).doesNotThrowAnyException();
        verify(usuarios).createTable(any(CreateTableEnhancedRequest.class));
    }

    private DynamoDbTable<?> tabela(String nome) {
        DynamoDbTable<Object> table = mock(DynamoDbTable.class);
        when(table.tableName()).thenReturn(nome);
        return table;
    }

    private DynamoDbTableInitializer inicializador(DynamoDbTable<?> perfis, DynamoDbTable<?> usuarios,
                                                   DynamoDbTable<?> refresh, DynamoDbTable<?> auditoria) {
        PerfilRepository perfilRepo = mock(PerfilRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        RefreshTokenRepository refreshRepo = mock(RefreshTokenRepository.class);
        AuditoriaAutenticacaoRepository auditoriaRepo = mock(AuditoriaAutenticacaoRepository.class);
        doReturn(perfis).when(perfilRepo).getTable();
        doReturn(usuarios).when(usuarioRepo).getTable();
        doReturn(refresh).when(refreshRepo).getTable();
        doReturn(auditoria).when(auditoriaRepo).getTable();
        return new DynamoDbTableInitializer(perfilRepo, usuarioRepo, refreshRepo, auditoriaRepo);
    }

    private java.util.List<String> indicesCriados(DynamoDbTable<?> table) {
        ArgumentCaptor<CreateTableEnhancedRequest> captor = ArgumentCaptor.forClass(CreateTableEnhancedRequest.class);
        verify(table).createTable(captor.capture());
        assertThat(captor.getValue().provisionedThroughput()).isNotNull();
        return captor.getValue().globalSecondaryIndices().stream()
                .map(EnhancedGlobalSecondaryIndex::indexName)
                .toList();
    }
}
