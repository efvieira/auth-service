package com.authservice.repository;

import com.authservice.model.AuditoriaAutenticacao;
import com.authservice.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AuditoriaAutenticacaoRepository {

    private final DynamoDbTable<AuditoriaAutenticacao> auditoriaTable;

    public AuditoriaAutenticacaoRepository(DynamoDbEnhancedClient enhancedClient,
                                           @Value("${dynamodb.tables.auditoria-autenticacao}") String tableName) {
        this.auditoriaTable = enhancedClient.table(tableName, TableSchema.fromBean(AuditoriaAutenticacao.class));
    }

    public DynamoDbTable<AuditoriaAutenticacao> getTable() {
        return auditoriaTable;
    }

    public AuditoriaAutenticacao save(AuditoriaAutenticacao auditoria) {
        auditoriaTable.putItem(auditoria);
        return auditoria;
    }

    public Optional<AuditoriaAutenticacao> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(auditoriaTable.getItem(key));
    }

    public List<AuditoriaAutenticacao> findByUsuario(Usuario usuario) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(usuario.getId()).build()
        );
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        DynamoDbIndex<AuditoriaAutenticacao> usuarioIndex = auditoriaTable.index("usuario-index");
        List<AuditoriaAutenticacao> results = new ArrayList<>();
        for (Page<AuditoriaAutenticacao> page : usuarioIndex.query(request)) {
            if (page.items() != null) {
                page.items().forEach(results::add);
            }
        }
        return results;
    }

    public List<AuditoriaAutenticacao> findByUsuarioAndDataHoraBetween(Usuario usuario, LocalDateTime inicio, LocalDateTime fim) {
        // data_hora é armazenada como string ISO-8601, que ordena lexicograficamente
        QueryConditional queryConditional = QueryConditional.sortBetween(
            Key.builder().partitionValue(usuario.getId()).sortValue(inicio.toString()).build(),
            Key.builder().partitionValue(usuario.getId()).sortValue(fim.toString()).build()
        );
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        DynamoDbIndex<AuditoriaAutenticacao> usuarioIndex = auditoriaTable.index("usuario-index");
        List<AuditoriaAutenticacao> results = new ArrayList<>();
        for (Page<AuditoriaAutenticacao> page : usuarioIndex.query(request)) {
            if (page.items() != null) {
                results.addAll(page.items());
            }
        }
        return results;
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        auditoriaTable.deleteItem(key);
    }
}
