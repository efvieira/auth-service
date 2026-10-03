package com.authservice.repository;

import com.authservice.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    private final DynamoDbTable<Usuario> usuarioTable;

    public UsuarioRepository(DynamoDbEnhancedClient enhancedClient,
                             @Value("${dynamodb.tables.usuarios}") String tableName) {
        this.usuarioTable = enhancedClient.table(tableName, TableSchema.fromBean(Usuario.class));
    }

    public DynamoDbTable<Usuario> getTable() {
        return usuarioTable;
    }

    public Usuario save(Usuario usuario) {
        usuarioTable.putItem(usuario);
        return usuario;
    }

    public Optional<Usuario> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(usuarioTable.getItem(key));
    }

    public Optional<Usuario> findByEmail(String email) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(email).build()
        );
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        DynamoDbIndex<Usuario> emailIndex = usuarioTable.index("email-index");
        // The query returns pages; iterate pages and return the first matching item if present
        for (Page<Usuario> page : emailIndex.query(request)) {
            if (page.items() != null && !page.items().isEmpty()) {
                return page.items().stream().findFirst();
            }
        }
        return Optional.empty();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        usuarioTable.deleteItem(key);
    }
}
