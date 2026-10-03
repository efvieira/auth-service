package com.authservice.repository;

import com.authservice.model.RefreshToken;
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
public class RefreshTokenRepository {

    private final DynamoDbTable<RefreshToken> refreshTokenTable;

    public RefreshTokenRepository(DynamoDbEnhancedClient enhancedClient,
                                  @Value("${dynamodb.tables.refresh-tokens}") String tableName) {
        this.refreshTokenTable = enhancedClient.table(tableName, TableSchema.fromBean(RefreshToken.class));
    }

    public DynamoDbTable<RefreshToken> getTable() {
        return refreshTokenTable;
    }

    public RefreshToken save(RefreshToken refreshToken) {
        refreshTokenTable.putItem(refreshToken);
        return refreshToken;
    }

    public Optional<RefreshToken> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(refreshTokenTable.getItem(key));
    }

    public Optional<RefreshToken> findByToken(String token) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(token).build()
        );
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        DynamoDbIndex<RefreshToken> tokenIndex = refreshTokenTable.index("token-index");
        for (Page<RefreshToken> page : tokenIndex.query(request)) {
            if (page.items() != null && !page.items().isEmpty()) {
                return Optional.of(page.items().get(0));
            }
        }
        return Optional.empty();
    }

    public void deleteByToken(String token) {
        findByToken(token).ifPresent(rt -> {
            Key key = Key.builder().partitionValue(rt.getId()).build();
            refreshTokenTable.deleteItem(key);
        });
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        refreshTokenTable.deleteItem(key);
    }
}
