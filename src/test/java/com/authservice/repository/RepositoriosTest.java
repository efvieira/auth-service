package com.authservice.repository;

import com.authservice.enums.TipoPerfil;
import com.authservice.model.AuditoriaAutenticacao;
import com.authservice.model.Perfil;
import com.authservice.model.RefreshToken;
import com.authservice.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testa os repositórios com o Enhanced Client simulado: verifica tabela, índice,
 * chaves usadas nas consultas e o tratamento das páginas retornadas.
 */
@SuppressWarnings("unchecked")
class RepositoriosTest {

    private DynamoDbEnhancedClient client;
    private DynamoDbTable<Object> table;
    private DynamoDbIndex<Object> index;

    @BeforeEach
    void setUp() {
        client = mock(DynamoDbEnhancedClient.class);
        table = mock(DynamoDbTable.class);
        index = mock(DynamoDbIndex.class);
        when(client.table(anyString(), any(TableSchema.class))).thenReturn(table);
        when(table.index(anyString())).thenReturn(index);
    }

    /**
     * Simula o resultado paginado de uma consulta em índice
     */
    private void indiceRetorna(List<?>... paginas) {
        SdkIterable<Page<Object>> resultado = () -> Arrays.stream(paginas)
                .map(itens -> Page.create((List<Object>) itens))
                .iterator();
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(resultado);
    }

    private String chaveConsultada() {
        ArgumentCaptor<QueryEnhancedRequest> captor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
        verify(index).query(captor.capture());
        return captor.getValue().queryConditional()
                .expression(TableSchema.fromBean(Usuario.class), "email-index")
                .expressionValues().values().iterator().next().s();
    }

    private Key chaveDoId(String id) {
        return Key.builder().partitionValue(id).build();
    }

    @Nested
    class Usuarios {

        private UsuarioRepository repo;

        @BeforeEach
        void setUp() {
            repo = new UsuarioRepository(client, "usuarios");
        }

        @Test
        void usaTabelaConfigurada() {
            verify(client).table(eq("usuarios"), any(TableSchema.class));
            assertThat((Object) repo.getTable()).isSameAs(table);
        }

        @Test
        void saveFindDelete() {
            Usuario usuario = new Usuario();
            usuario.setId("u-1");
            when(table.getItem(chaveDoId("u-1"))).thenReturn(usuario);

            assertThat(repo.save(usuario)).isSameAs(usuario);
            verify(table).putItem(usuario);
            assertThat(repo.findById("u-1")).containsSame(usuario);
            assertThat(repo.findById("outro")).isEmpty();

            repo.deleteById("u-1");
            verify(table).deleteItem(chaveDoId("u-1"));
        }

        @Test
        void findByEmailConsultaEmailIndexEPulaPaginasVazias() {
            Usuario usuario = new Usuario();
            indiceRetorna(List.of(), List.of(usuario));

            assertThat(repo.findByEmail("joao@example.com")).containsSame(usuario);
            verify(table).index("email-index");
            assertThat(chaveConsultada()).isEqualTo("joao@example.com");
        }

        @Test
        void existsByEmail() {
            indiceRetorna(List.of());
            assertThat(repo.existsByEmail("x@example.com")).isFalse();

            indiceRetorna(List.of(new Usuario()));
            assertThat(repo.existsByEmail("x@example.com")).isTrue();
        }
    }

    @Nested
    class Perfis {

        private PerfilRepository repo;

        @BeforeEach
        void setUp() {
            repo = new PerfilRepository(client, "perfis");
        }

        @Test
        void saveFindDelete() {
            Perfil perfil = new Perfil("p-1", TipoPerfil.USER, "Usuário", true);
            when(table.getItem(chaveDoId("p-1"))).thenReturn(perfil);

            assertThat((Object) repo.getTable()).isSameAs(table);
            assertThat(repo.save(perfil)).isSameAs(perfil);
            verify(table).putItem(perfil);
            assertThat(repo.findById("p-1")).containsSame(perfil);

            repo.deleteById("p-1");
            verify(table).deleteItem(chaveDoId("p-1"));
        }

        @Test
        void findByTipoConsultaTipoIndexPeloNomeDoEnum() {
            Perfil perfil = new Perfil("p-1", TipoPerfil.ADMIN, "Admin", true);
            indiceRetorna(List.of(perfil));

            assertThat(repo.findByTipo(TipoPerfil.ADMIN)).containsSame(perfil);
            verify(table).index("tipo-index");
            assertThat(chaveConsultada()).isEqualTo("ADMIN");
        }

        @Test
        void existsByTipo() {
            indiceRetorna(List.of(), List.of());
            assertThat(repo.existsByTipo(TipoPerfil.USER)).isFalse();

            indiceRetorna(List.of(new Perfil()));
            assertThat(repo.existsByTipo(TipoPerfil.USER)).isTrue();
        }
    }

    @Nested
    class RefreshTokens {

        private RefreshTokenRepository repo;

        @BeforeEach
        void setUp() {
            repo = new RefreshTokenRepository(client, "refresh_tokens");
        }

        @Test
        void saveFindDelete() {
            RefreshToken rt = new RefreshToken();
            rt.setId("rt-1");
            when(table.getItem(chaveDoId("rt-1"))).thenReturn(rt);

            assertThat((Object) repo.getTable()).isSameAs(table);
            assertThat(repo.save(rt)).isSameAs(rt);
            verify(table).putItem(rt);
            assertThat(repo.findById("rt-1")).containsSame(rt);

            repo.deleteById("rt-1");
            verify(table).deleteItem(chaveDoId("rt-1"));
        }

        @Test
        void findByTokenConsultaTokenIndex() {
            RefreshToken rt = new RefreshToken();
            indiceRetorna(List.of(), List.of(rt));

            assertThat(repo.findByToken("jwt")).containsSame(rt);
            verify(table).index("token-index");
            assertThat(chaveConsultada()).isEqualTo("jwt");
        }

        @Test
        void findByTokenInexistente() {
            indiceRetorna();
            assertThat(repo.findByToken("jwt")).isEmpty();
        }

        @Test
        void deleteByTokenRemovePeloId() {
            RefreshToken rt = new RefreshToken();
            rt.setId("rt-9");
            indiceRetorna(List.of(rt));

            repo.deleteByToken("jwt");

            verify(table).deleteItem(chaveDoId("rt-9"));
        }

        @Test
        void deleteByTokenInexistenteNaoRemoveNada() {
            indiceRetorna(List.of());

            repo.deleteByToken("jwt");

            verify(table, never()).deleteItem(any(Key.class));
        }
    }

    @Nested
    class Auditoria {

        private AuditoriaAutenticacaoRepository repo;
        private Usuario usuario;

        @BeforeEach
        void setUp() {
            repo = new AuditoriaAutenticacaoRepository(client, "auditoria_autenticacao");
            usuario = new Usuario();
            usuario.setId("u-1");
        }

        @Test
        void saveFindDelete() {
            AuditoriaAutenticacao a = new AuditoriaAutenticacao();
            a.setId("a-1");
            when(table.getItem(chaveDoId("a-1"))).thenReturn(a);

            assertThat((Object) repo.getTable()).isSameAs(table);
            assertThat(repo.save(a)).isSameAs(a);
            verify(table).putItem(a);
            assertThat(repo.findById("a-1")).containsSame(a);

            repo.deleteById("a-1");
            verify(table).deleteItem(chaveDoId("a-1"));
        }

        @Test
        void findByUsuarioJuntaTodasAsPaginas() {
            AuditoriaAutenticacao a1 = new AuditoriaAutenticacao();
            AuditoriaAutenticacao a2 = new AuditoriaAutenticacao();
            indiceRetorna(List.of(a1), List.of(), List.of(a2));

            assertThat(repo.findByUsuario(usuario)).containsExactly(a1, a2);
            verify(table).index("usuario-index");
        }

        @Test
        void findByPeriodoUsaSortBetween() {
            AuditoriaAutenticacao a = new AuditoriaAutenticacao();
            indiceRetorna(List.of(a));
            LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime fim = LocalDateTime.of(2026, 1, 31, 23, 59);

            assertThat(repo.findByUsuarioAndDataHoraBetween(usuario, inicio, fim)).containsExactly(a);

            ArgumentCaptor<QueryEnhancedRequest> captor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
            verify(index).query(captor.capture());
            var expressao = captor.getValue().queryConditional()
                    .expression(TableSchema.fromBean(AuditoriaAutenticacao.class), "usuario-index");
            assertThat(expressao.expression()).contains("BETWEEN");
            assertThat(expressao.expressionValues().values())
                    .extracting(v -> v.s())
                    .contains("u-1", inicio.toString(), fim.toString());
        }
    }
}
