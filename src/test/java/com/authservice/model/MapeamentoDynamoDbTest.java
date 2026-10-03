package com.authservice.model;

import com.authservice.enums.TipoPerfil;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que os models são mapeáveis pelo DynamoDB Enhanced Client, sem precisar de banco.
 */
class MapeamentoDynamoDbTest {

    @Test
    void perfilGravaEnumComoStringNoIndiceTipo() {
        TableSchema<Perfil> schema = TableSchema.fromBean(Perfil.class);
        Perfil perfil = new Perfil("p-1", TipoPerfil.ADMIN, "Administrador", true);

        Map<String, AttributeValue> item = schema.itemToMap(perfil, true);

        // PerfilRepository.findByTipo consulta o tipo-index usando tipo.name()
        assertThat(item.get("tipo").s()).isEqualTo("ADMIN");
        assertThat(schema.mapToItem(item)).isEqualTo(perfil);
    }

    @Test
    void usuarioFazIdaEVoltaComNomesDeAtributos() {
        TableSchema<Usuario> schema = TableSchema.fromBean(Usuario.class);
        LocalDateTime agora = LocalDateTime.now();
        Usuario usuario = new Usuario("u-1", "joao@example.com", "hash", "João", true, "p-1", agora, agora);

        Map<String, AttributeValue> item = schema.itemToMap(usuario, true);

        assertThat(item).containsKeys("id", "email", "perfil_id", "data_criacao", "data_atualizacao");
        assertThat(schema.mapToItem(item)).isEqualTo(usuario);
    }

    @Test
    void auditoriaSemUsuarioOmiteChaveDoIndice() {
        TableSchema<AuditoriaAutenticacao> schema = TableSchema.fromBean(AuditoriaAutenticacao.class);
        AuditoriaAutenticacao auditoria = new AuditoriaAutenticacao(
                "a-1", null, "LOGIN_FALHA", "127.0.0.1", "curl", "Email não cadastrado", LocalDateTime.now());

        Map<String, AttributeValue> item = schema.itemToMap(auditoria, true);

        // Atributo de GSI não pode ser gravado como NULL; ausente é permitido (índice esparso)
        assertThat(item).doesNotContainKey("usuario_id");
        assertThat(item.get("data_hora").s()).isNotBlank();
    }

    @Test
    void refreshTokenFazIdaEVolta() {
        TableSchema<RefreshToken> schema = TableSchema.fromBean(RefreshToken.class);
        LocalDateTime agora = LocalDateTime.now();
        RefreshToken rt = new RefreshToken("rt-1", "u-1", "jwt", agora.plusDays(7), agora, false);

        assertThat(schema.mapToItem(schema.itemToMap(rt, true))).isEqualTo(rt);
    }
}
