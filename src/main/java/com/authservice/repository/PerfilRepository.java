package com.authservice.repository;

import com.authservice.enums.TipoPerfil;
import com.authservice.model.Perfil;
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
import java.util.Optional;

@Repository
public class PerfilRepository {

    private final DynamoDbTable<Perfil> perfilTable;

    public PerfilRepository(DynamoDbEnhancedClient enhancedClient,
                            @Value("${dynamodb.tables.perfis}") String tableName) {
        this.perfilTable = enhancedClient.table(tableName, TableSchema.fromBean(Perfil.class));
    }

    public DynamoDbTable<Perfil> getTable() {
        return perfilTable;
    }

    public Perfil save(Perfil perfil) {
        perfilTable.putItem(perfil);
        return perfil;
    }

    public Optional<Perfil> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(perfilTable.getItem(key));
    }

    public Optional<Perfil> findByTipo(TipoPerfil tipo) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(tipo.name()).build()
        );
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        DynamoDbIndex<Perfil> tipoIndex = perfilTable.index("tipo-index");
        for (Page<Perfil> page : tipoIndex.query(request)) {
            if (page.items() != null && !page.items().isEmpty()) {
                return page.items().stream().findFirst();
            }
        }
        return Optional.empty();
    }

    public boolean existsByTipo(TipoPerfil tipo) {
        return findByTipo(tipo).isPresent();
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        perfilTable.deleteItem(key);
    }
}
