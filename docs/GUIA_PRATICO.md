# 🚀 Guia Prático: Sistema de Roles em Ação

## 📖 Exemplo Completo de Uso

Este guia mostra um exemplo real e prático do sistema de roles em funcionamento.

## 🎬 Cenário

Imagine uma empresa com:
- **Admin**: João (gerente de TI)
- **Usuarios**: Maria e Pedro (funcionários)

## 📍 Passo 1: Admin Cria Sua Conta (Temporário, apenas para demo)

Na prática, seria necessário uma conta admin de bootstrap. Para fins de demonstração, assumiremos que João já é admin.

## 📍 Passo 2: Maria se Registra (User)

Maria, funcionária nova, quer se registrar no sistema:

```bash
# Maria faz requisição de registro
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Maria Silva",
    "email": "maria.silva@empresa.com",
    "senha": "MariaSenha123"
  }'

# Resposta esperada (201 CREATED):
{
  "id": 1,
  "nome": "Maria Silva",
  "email": "maria.silva@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
    "ativo": true
  }
}
```

✅ **Maria recebeu perfil USER automaticamente!**

## 📍 Passo 3: Pedro se Registra (User)

Mesma coisa para Pedro:

```bash
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Pedro Santos",
    "email": "pedro.santos@empresa.com",
    "senha": "PedroSenha456"
  }'

# Resposta esperada (201 CREATED):
{
  "id": 2,
  "nome": "Pedro Santos",
  "email": "pedro.santos@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
    "ativo": true
  }
}
```

✅ **Pedro também recebeu perfil USER!**

## 📍 Passo 4: Maria Faz Login

Maria quer fazer login para acessar o sistema:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria.silva@empresa.com",
    "senha": "MariaSenha123"
  }'

# Resposta esperada (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

✅ **Maria autenticada com sucesso!**

**Importante:** Guardar esses tokens para próximas requisições.

## 📍 Passo 5: Maria Consulta Seu Próprio Perfil (USER com seu próprio ID)

Maria quer ver seus dados:

```bash
# Maria consulta seu perfil (ID 1)
curl http://localhost:8080/api/usuarios/1 \
  -H "X-Usuario-Id: 1"

# Resposta esperada (200 OK):
{
  "id": 1,
  "nome": "Maria Silva",
  "email": "maria.silva@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
    "ativo": true
  }
}
```

✅ **Maria pode consultar seus próprios dados!**

## 📍 Passo 6: Maria Tenta Consultar Perfil de Pedro (USER tentando acessar outro)

Maria tenta ver os dados de Pedro por curiosidade:

```bash
# Maria tenta consultar perfil de Pedro (ID 2)
curl http://localhost:8080/api/usuarios/2 \
  -H "X-Usuario-Id: 1"

# Resposta esperada (403 FORBIDDEN):
{
  "timestamp": "2026-08-16T14:30:22",
  "status": 403,
  "mensagem": "Acesso negado. Apenas administradores podem realizar esta ação."
}
```

❌ **Maria NÃO pode ver dados de outros usuários!**

## 📍 Passo 7: Maria Tenta Criar um Novo Usuário (USER sem permissão)

Maria tenta criar uma conta para seu estagiário:

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 1" \
  -d '{
    "nome": "João Estagiário",
    "email": "joao.estag@empresa.com",
    "senha": "EstagSenha789",
    "perfil": "USER"
  }'

# Resposta esperada (403 FORBIDDEN):
{
  "timestamp": "2026-08-16T14:32:15",
  "status": 403,
  "mensagem": "Acesso negado. Apenas administradores podem realizar esta ação."
}
```

❌ **Maria NÃO pode criar usuários!**

## 📍 Passo 8: João (Admin) Faz Login

Agora João, que é admin, faz login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@empresa.com",
    "senha": "JoaoAdmin123"
  }'

# Resposta esperada (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

✅ **João autenticado como ADMIN!**

## 📍 Passo 9: João (Admin) Consulta Qualquer Perfil

João quer ver os dados de Maria:

```bash
# João consulta perfil de Maria (ID 1)
curl http://localhost:8080/api/usuarios/1 \
  -H "X-Usuario-Id: 3"

# Resposta esperada (200 OK):
{
  "id": 1,
  "nome": "Maria Silva",
  "email": "maria.silva@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
    "ativo": true
  }
}
```

✅ **João (ADMIN) pode consultar dados de qualquer usuário!**

## 📍 Passo 10: João Cria Novo Estagiário com Perfil USER

Agora João cria a conta do estagiário que Maria pediu:

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 3" \
  -d '{
    "nome": "João Estagiário",
    "email": "joao.estag@empresa.com",
    "senha": "EstagSenha789",
    "ativo": true,
    "perfil": "USER"
  }'

# Resposta esperada (201 CREATED):
{
  "id": 4,
  "nome": "João Estagiário",
  "email": "joao.estag@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
    "ativo": true
  }
}
```

✅ **João (ADMIN) criou novo usuário com perfil USER!**

## 📍 Passo 11: João Cria Novo Administrador

João precisa de outro admin na equipe, então promove Carlos:

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 3" \
  -d '{
    "nome": "Carlos Admin",
    "email": "carlos.admin@empresa.com",
    "senha": "CarlosAdmin999",
    "ativo": true,
    "perfil": "ADMIN"
  }'

# Resposta esperada (201 CREATED):
{
  "id": 5,
  "nome": "Carlos Admin",
  "email": "carlos.admin@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 1,
    "tipo": "ADMIN",
    "descricao": "Administrador - Pode criar contas, gerenciar usuários e acessar todas as funcionalidades",
    "ativo": true
  }
}
```

✅ **João criou novo ADMIN!**

## 📍 Passo 12: Carlos (Novo Admin) Cria Usuário

Carlos, agora admin, cria uma nova conta:

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 5" \
  -d '{
    "nome": "Ana Silva",
    "email": "ana.silva@empresa.com",
    "senha": "AnaSilva111",
    "ativo": true,
    "perfil": "USER"
  }'

# Resposta esperada (201 CREATED):
{
  "id": 6,
  "nome": "Ana Silva",
  "email": "ana.silva@empresa.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
    "ativo": true
  }
}
```

✅ **Carlos (novo ADMIN) também pode criar usuários!**

## 📋 Resumo de Permissões em Ação

| Ação | Maria (USER) | João (ADMIN) | Carlos (ADMIN) | Resultado |
|------|--------------|-------------|----------------|-----------|
| Ver seu próprio perfil | ✅ | ✅ | ✅ | Sucesso |
| Ver perfil de outro | ❌ | ✅ | ✅ | Maria: Erro 403 |
| Criar usuário USER | ❌ | ✅ | ✅ | Maria: Erro 403 |
| Criar usuário ADMIN | ❌ | ✅ | ✅ | Maria: Erro 403 |
| Fazer login | ✅ | ✅ | ✅ | Sucesso |
| Registrar novo | ✅ | ✅ | ✅ | Sucesso (mas vira USER) |

## 🔒 Segurança Demonstrada

1. **Isolamento de Dados**: Maria não pode ver dados de Pedro
2. **Controle de Acesso**: Apenas admins podem criar usuários
3. **Múltiplos Admins**: Carlos, sendo admin, tem mesmas permissões que João
4. **Auditoria**: Todas as tentativas de acesso são registradas

## 📊 Banco de Dados Resultante

Após esses passos, o banco teria:

```sql
-- Perfis
SELECT * FROM perfis;
-- ID | TIPO  | DESCRICAO                           | ATIVO
-- 1  | ADMIN | Administrador - Pode criar contas  | true
-- 2  | USER  | Usuário - Pode autenticar          | true

-- Usuários
SELECT * FROM usuarios;
-- ID | NOME              | EMAIL                    | PERFIL_ID | ATIVO
-- 1  | Maria Silva       | maria.silva@empresa.com  | 2         | true
-- 2  | Pedro Santos      | pedro.santos@empresa.com | 2         | true
-- 3  | João Admin        | joao@empresa.com         | 1         | true
-- 4  | João Estagiário   | joao.estag@empresa.com   | 2         | true
-- 5  | Carlos Admin      | carlos.admin@empresa.com | 1         | true
-- 6  | Ana Silva         | ana.silva@empresa.com    | 2         | true
```

## 🎯 Conclusão

Este exemplo demonstra que:

✅ **USER**: Acesso limitado e seguro
✅ **ADMIN**: Controle total do sistema
✅ **Permissões**: Validadas em múltiplas camadas
✅ **Segurança**: Dados isolados por role
✅ **Escalabilidade**: Múltiplos admins possíveis

## 🔧 Próximas Ações

1. ✅ Testar localmente seguindo este guia
2. ✅ Modificar dados de teste conforme necessário
3. ✅ Integrar com seu frontend
4. ✅ Implementar refresh token automático
5. ✅ Adicionar dashboard de auditoria

