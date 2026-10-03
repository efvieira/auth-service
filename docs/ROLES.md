# Sistema de Roles/Perfis - Auth Service

## Visão Geral

O Auth Service implementa um sistema robusto de controle de acesso baseado em roles (RBAC - Role-Based Access Control) que permite gerenciar permissões de usuários de forma granular e segura.

## 📊 Estrutura de Roles

### Perfil: ADMIN
**Tipo:** `ADMIN`

**Permissões:**
- ✅ Criar novos usuários no sistema
- ✅ Consultar qualquer perfil de usuário
- ✅ Gerenciar usuários (ativar/desativar)
- ✅ Assignar roles a usuários
- ✅ Acesso completo a todas as funcionalidades

**Casos de Uso:**
- Gerentes de sistema
- Gerentes de TI
- Administratores da aplicação

**Exemplo de Criação:**
```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 1" \
  -d '{
    "nome": "Admin User",
    "email": "admin@example.com",
    "senha": "AdminSenha123",
    "ativo": true,
    "perfil": "ADMIN"
  }'
```

### Perfil: USER
**Tipo:** `USER`

**Permissões:**
- ✅ Autenticar com email e senha
- ✅ Consultar seu próprio perfil
- ✅ Buscar seu próprio email
- ❌ Criar usuários
- ❌ Consultar perfis de outros usuários
- ❌ Modificar dados de outros usuários

**Casos de Uso:**
- Usuários finais
- Clientes
- Equipes internas com acesso limitado

**Exemplo de Registro:**
```bash
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao@example.com",
    "senha": "Senha123"
  }'
```

## 🔒 Controle de Acesso nos Endpoints

### Endpoints Públicos (Sem Autenticação)
- `POST /api/auth/registrar` - Registrar novo usuário
- `POST /api/auth/login` - Fazer login

### Endpoints Protegidos (Requer Header X-Usuario-Id)

#### Apenas ADMIN
| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/api/usuarios` | `POST` | Criar novo usuário |
| `/api/usuarios/{id}` | `GET` | Consultar qualquer usuário |
| `/api/usuarios/email/{email}` | `GET` | Buscar por email |

#### ADMIN + USER (com restrições)
| Endpoint | Método | ADMIN | USER |
|----------|--------|-------|------|
| `/api/usuarios/{id}` | `GET` | ✅ Qualquer um | ✅ Apenas seu próprio |
| `/api/usuarios/email/{email}` | `GET` | ✅ Qualquer um | ✅ Apenas seu próprio |

## 🛡️ Validação de Permissões

### Como Funciona

1. **Cliente Envia Requisição**
   ```http
   POST /api/usuarios
   Content-Type: application/json
   X-Usuario-Id: 1
   
   {
     "nome": "New User",
     "email": "newuser@example.com",
     "senha": "Password123",
     "perfil": "USER"
   }
   ```

2. **Sistema Valida Permissão**
   - Extrai `X-Usuario-Id` do header
   - Busca o usuário no banco
   - Verifica o perfil do usuário
   - Valida se tem permissão para a ação

3. **Resposta**
   ```json
   {
     "id": 2,
     "nome": "New User",
     "email": "newuser@example.com",
     "ativo": true,
     "perfil": {
       "id": 2,
       "tipo": "USER",
       "descricao": "Usuário - Pode autenticar e buscar seu próprio perfil",
       "ativo": true
     }
   }
   ```

### Erro: Sem Permissão
```json
{
  "timestamp": "2026-08-16T10:30:00",
  "status": 403,
  "mensagem": "Acesso negado. Apenas administradores podem realizar esta ação."
}
```

## 📋 Fluxo de Atribuição de Roles

### Opção 1: Auto-registro como USER
```
Usuário → POST /api/auth/registrar → Recebe role USER automaticamente
```

### Opção 2: Admin criar com role específica
```
Admin → POST /api/usuarios → Define role na requisição (ADMIN ou USER)
```

## 🔄 Serviço de Permissões

O `PermissaoService` fornece métodos auxiliares:

```java
// Verificar se é ADMIN
permissaoService.isAdmin(usuarioId);

// Lançar exceção se não for ADMIN
permissaoService.verificarAdmin(usuarioId);

// Verificar se é USER
permissaoService.isUser(usuarioId);

// Obter tipo de perfil
TipoPerfil tipo = permissaoService.obterTipoPerfil(usuarioId);
```

## 📝 Auditoria de Acesso

Todas as tentativas de acesso (sucesso e falha) são auditadas:

```sql
SELECT * FROM auditoria_autenticacao 
WHERE usuario_id = 1 
ORDER BY data_hora DESC;
```

Registra:
- Tipo de evento (LOGIN_SUCESSO, LOGIN_FALHA, etc)
- Data e hora
- IP do cliente
- User-Agent
- Descrição de erro (se houver)

## 🚀 Extensibilidade

Para adicionar novos roles:

1. Adicionar novo tipo em `TipoPerfil.java`:
```java
public enum TipoPerfil {
    ADMIN("Administrador"),
    USER("Usuário"),
    MANAGER("Gerente");  // Novo
}
```

2. Atualizar `PerfilInitializer.java`:
```java
if (!perfilRepository.existsByTipo(TipoPerfil.MANAGER)) {
    Perfil managerPerfil = new Perfil();
    managerPerfil.setTipo(TipoPerfil.MANAGER);
    managerPerfil.setDescricao("Gerente - Acesso intermediário");
    perfilRepository.save(managerPerfil);
}
```

3. Adicionar validações em `PermissaoService.java`:
```java
public boolean isManager(Long usuarioId) {
    return usuarioRepository.findById(usuarioId)
        .map(u -> TipoPerfil.MANAGER == u.getPerfil().getTipo())
        .orElse(false);
}
```

## ⚙️ Configuração

### Header de Autenticação
- Nome: `X-Usuario-Id`
- Tipo: `Long`
- Obrigatório para endpoints protegidos

### Base de Dados
Tabelas criadas automaticamente:
- `perfis` - Armazena tipos de roles
- `usuarios` - Armazena usuários com referência a perfil_id

## 🔐 Boas Práticas de Segurança

1. **Validação em Camadas**
   - Controller: Valida permissão no header
   - Service: Verifica permissão no banco
   - Database: Constraint de integridade referencial

2. **Princípio do Menor Privilégio**
   - USER tem acesso mínimo necessário
   - ADMIN tem acesso quando necessário

3. **Auditoria Completa**
   - Todas as ações autenticadas são registradas
   - Rastreabilidade completa de quem fez o quê

4. **Tokens JWT**
   - Inclui identificador do usuário
   - Inclui tipo de perfil (possível extensão futura)

## 📞 Suporte e Exemplos

Para exemplos completos de integração, consulte:
- `INSTALACAO.md` - Exemplos de cURL
- `README.md` - Documentação de API
- `src/main/java/com/authservice/` - Código-fonte

