# 📋 Resumo das Mudanças - Sistema de Roles

## ✅ O que foi implementado

### 1. **Entidades de Banco de Dados**

#### Nova Entidade: `Perfil`
```sql
CREATE TABLE perfis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tipo VARCHAR(50) UNIQUE NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    ativo BOOLEAN DEFAULT true
);
```

#### Atualização: `Usuario` agora tem relacionamento com `Perfil`
```sql
ALTER TABLE usuarios ADD COLUMN perfil_id BIGINT NOT NULL;
ALTER TABLE usuarios ADD FOREIGN KEY (perfil_id) REFERENCES perfis(id);
```

### 2. **Novos Tipos (Enums)**
- `TipoPerfil.ADMIN` - Administrador com acesso completo
- `TipoPerfil.USER` - Usuário comum com acesso limitado

### 3. **Novos Serviços**

#### `PermissaoService`
Responsável por validar permissões:
- `isAdmin(usuarioId)` - Verifica se é admin
- `verificarAdmin(usuarioId)` - Lança exceção se não for admin
- `isUser(usuarioId)` - Verifica se é user
- `obterTipoPerfil(usuarioId)` - Retorna o tipo de perfil

#### `PerfilInitializer`
Inicializa os perfis ADMIN e USER no startup da aplicação (implementa `CommandLineRunner`)

### 4. **Novos Repositories**
- `PerfilRepository` - CRUD para perfis

### 5. **Novos DTOs**
- `PerfilDTO` - Transfer object para retornar dados de perfil
- `CriarUsuarioDTO` - Atualizado para incluir campo `perfil`

### 6. **Novas Exceções**
- `PermissaoNegadaException` - Lançada quando usuário sem permissão tenta acessar endpoint

### 7. **Atualização do Controller**
- Endpoint `POST /api/usuarios` agora valida se usuário é ADMIN
- Endpoints GET agora validam permissão baseado em role
- Adicionado header `X-Usuario-Id` para identificar usuário autenticado

### 8. **Atualização do Handler de Exceções**
- Adicionado tratamento para `PermissaoNegadaException`

## 📊 Fluxo de Funcionamento

### Antes (sem Roles)
```
Usuário → Registrar → Criar Conta
          Login → Autenticar
          
Qualquer um poderia criar usuário (sem validação)
```

### Depois (com Roles)
```
Usuário → Registrar → USER role (automático)
          Login → Autenticar
          
Admin → Login → Pode criar contas
        Header X-Usuario-Id → Criar Usuário → USER ou ADMIN role

USER → Login → Pode consultar seu próprio perfil
       Header X-Usuario-Id → GET /api/usuarios/1 → Se próprio ID: OK
                                                   Se outro ID: 403 Forbidden
```

## 🔐 Segurança Implementada

### ✅ Validação em Múltiplas Camadas
1. **Header Validation**: Verifica presença de `X-Usuario-Id`
2. **Database Validation**: Busca usuário e verifica perfil
3. **Exception Handling**: Retorna 403 Forbidden se sem permissão
4. **Auditoria**: Registra todas as tentativas

### ✅ Princípio do Menor Privilégio
- USER: Acesso mínimo necessário (só seu próprio perfil)
- ADMIN: Acesso necessário para administrar sistema

### ✅ Separação de Responsabilidades
- Controller: Validação de headers e permissão
- Service: Lógica de negócio
- Repository: Acesso a dados

## 📝 Documentação Criada

| Arquivo | Descrição |
|---------|-----------|
| `ROLES.md` | Documentação completa do sistema de roles |
| `ESTRUTURA_ROLES.md` | Diagrama de arquitetura e fluxos |
| `README.md` | Atualizado com informações de roles |
| `INSTALACAO.md` | Exemplos de cURL com roles |

## 🧪 Testando o Sistema

### 1. Iniciar Aplicação
```bash
mvn spring-boot:run
```

A aplicação criará automaticamente os perfis ADMIN e USER

### 2. Registrar Usuário (Recebe USER)
```bash
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João",
    "email": "joao@example.com",
    "senha": "Senha123"
  }'
```

### 3. Fazer Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "senha": "Senha123"
  }'
```

### 4. Tentar Criar Usuário com USER (Deve falhar)
```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 1" \
  -d '{
    "nome": "Maria",
    "email": "maria@example.com",
    "senha": "Senha456",
    "perfil": "USER"
  }'

# Retorno esperado (403):
# "Acesso negado. Apenas administradores podem realizar esta ação."
```

### 5. Consultar Próprio Perfil com USER (Deve suceder)
```bash
curl http://localhost:8080/api/usuarios/1 \
  -H "X-Usuario-Id: 1"

# Retorno esperado (200): Seus dados do perfil
```

### 6. Consultar Outro Perfil com USER (Deve falhar)
```bash
curl http://localhost:8080/api/usuarios/2 \
  -H "X-Usuario-Id: 1"

# Retorno esperado (403):
# "Acesso negado. Apenas administradores podem realizar esta ação."
```

## 🎯 Próximos Passos

### Curto Prazo
- [ ] Testar todos os endpoints
- [ ] Validar fluxos de permissão
- [ ] Verificar auditoria

### Médio Prazo
- [ ] Integrar com Spring Security
- [ ] Implementar OAuth2
- [ ] Adicionar mais roles (MANAGER, SUPPORT, etc)

### Longo Prazo
- [ ] Dashboard de auditoria
- [ ] API de gerenciamento de roles
- [ ] Integração com LDAP/Active Directory

## 📊 Métricas de Implementação

| Métrica | Valor |
|---------|-------|
| Novos Arquivos Criados | 15+ |
| Arquivos Modificados | 8+ |
| Entidades Criadas | 1 (Perfil) |
| Serviços Criados | 2 (PermissaoService, PerfilInitializer) |
| Exceções Criadas | 1 (PermissaoNegadaException) |
| Repositórios Criados | 1 (PerfilRepository) |
| DTOs Criados | 2 (PerfilDTO, atualizado CriarUsuarioDTO) |

## 🔍 Cobertura de Regras de Negócio

| RN | Descrição | Status |
|----|-----------|--------|
| RN-001 | Email único | ✅ Mantido |
| RN-002 | Senha com BCrypt | ✅ Mantido |
| RN-003 | Validação de senha | ✅ Mantido |
| RN-004 | Usuário inativo não autentica | ✅ Mantido |
| RN-005 | JWT expirado rejeitado | ✅ Mantido |
| RN-006 | Refresh Token validado | ✅ Mantido |
| RN-007 | Auditoria completa | ✅ Mantido |
| RN-008 | ADMIN cria contas | ✅ NOVO |
| RN-009 | USER acesso limitado | ✅ NOVO |

## 📞 Suporte

Para dúvidas:
1. Consulte `ROLES.md` para detalhes de roles
2. Consulte `INSTALACAO.md` para exemplos
3. Verifique `ESTRUTURA_ROLES.md` para fluxos
4. Revise o código-fonte em `src/main/java/com/authservice/`

