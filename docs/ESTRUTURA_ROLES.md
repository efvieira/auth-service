# Estrutura do Sistema de Roles - Auth Service

## 📊 Diagrama de Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENTE / FRONTEND                           │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ HTTP Request + Header X-Usuario-Id
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REST CONTROLLER                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ - UsuarioController                                      │  │
│  │ - AutenticacaoController                                 │  │
│  └────────────────┬─────────────────────────────────────────┘  │
└────────────────────┼──────────────────────────────────────────────┘
                     │ Extrai X-Usuario-Id
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PERMISSAO SERVICE                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ verificarAdmin(usuarioId) ─┐                             │  │
│  │ isAdmin(usuarioId) ────────┼─→ Valida Permissão         │  │
│  │ isUser(usuarioId) ─────────┘                             │  │
│  └────────────────┬─────────────────────────────────────────┘  │
└────────────────────┼──────────────────────────────────────────────┘
                     │
       ┌─────────────┼──────────────┐
       │ Aprovado    │ Negado       │
       ▼             ▼              │
   ┌─────────┐   ┌──────────────┐  │
   │ BUSINESS│   │ PermissaoNeg │  │
   │ LOGIC   │   │ Exception    │  │
   └────┬────┘   └──────┬───────┘  │
        │                │          │
        └────────┬───────┴──────────┘
                 │
                 ▼
        ┌────────────────┐
        │ BANCO DE DADOS │
        │  - Perfis      │
        │  - Usuarios    │
        │  - Auditoria   │
        └────────────────┘
```

## 🔑 Componentes do Sistema

### 1. **Entidades (Models)**

```
┌─────────────────┐         ┌──────────────────┐
│   Perfil        │         │     Usuario      │
├─────────────────┤         ├──────────────────┤
│ id: Long        │◄────────│ id: Long         │
│ tipo: Enum      │ 1   *   │ email: String    │
│ descricao: Str  │         │ senha: String    │
│ ativo: Bool     │         │ nome: String     │
└─────────────────┘         │ ativo: Bool      │
                            │ perfil_id: FK    │
                            │ dataCriacao      │
                            │ dataAtualizacao  │
                            └──────────────────┘
```

### 2. **Tipos de Perfil (Enum)**

```java
enum TipoPerfil {
    ADMIN("Administrador"),     // Acesso completo
    USER("Usuário")             // Acesso limitado
}
```

### 3. **Permissões por Endpoint**

```
POST /api/auth/registrar
├─ Acesso: ✅ Público
├─ Role Necessária: Nenhuma
└─ Resultado: Novo usuário com role USER

POST /api/usuarios
├─ Acesso: 🔒 Protegido
├─ Role Necessária: ADMIN
├─ Header: X-Usuario-Id
└─ Resultado: Novo usuário (ADMIN ou USER)

GET /api/usuarios/{id}
├─ Acesso: 🔒 Protegido
├─ Role ADMIN: ✅ Pode consultar qualquer ID
├─ Role USER: ✅ Só seu próprio ID
└─ Header: X-Usuario-Id

GET /api/usuarios/email/{email}
├─ Acesso: 🔒 Protegido
├─ Role ADMIN: ✅ Pode consultar qualquer email
├─ Role USER: ✅ Só seu próprio email
└─ Header: X-Usuario-Id

POST /api/auth/login
├─ Acesso: ✅ Público
├─ Role Necessária: Nenhuma (apenas validação de credenciais)
└─ Resultado: JWT + Refresh Token
```

## 🔐 Fluxo de Autenticação e Autorização

### Fluxo 1: Registrar Novo Usuário (USER)

```
1. Usuário envia POST /api/auth/registrar
                ↓
2. AutenticacaoService.registrar()
   - Valida email único (RN-001)
   - Valida senha forte (RN-003)
                ↓
3. Busca Perfil USER em PerfilRepository
   - Se não encontrar, PerfilInitializer cria
                ↓
4. Cria entidade Usuario com:
   - email, nome, senha (hash BCrypt)
   - perfil = USER
   - ativo = true
                ↓
5. Salva no banco e retorna DTO com perfil
                ↓
✅ Novo usuário com role USER criado
```

### Fluxo 2: Admin Criar Usuário (ADMIN ou USER)

```
1. Admin envia POST /api/usuarios
   com header X-Usuario-Id: 1
                ↓
2. UsuarioController verifica header
   extrai X-Usuario-Id
                ↓
3. Chama PermissaoService.verificarAdmin(1)
   - Busca usuário ID 1 no banco
   - Verifica se perfil é ADMIN
                ↓
4. Se ADMIN: continua
   Se USER: lança PermissaoNegadaException
                ↓
5. UsuarioService.criarUsuario()
   - Valida email único
   - Valida senha forte
   - Busca Perfil (informado ou USER padrão)
   - Cria e salva Usuario
                ↓
6. Retorna novo usuário com perfil atribuído
                ↓
✅ Novo usuário criado com perfil especificado
```

### Fluxo 3: Fazer Login

```
1. Usuário envia POST /api/auth/login
   com email e senha
                ↓
2. AutenticacaoService.autenticar()
   - Busca usuário pelo email
   - Verifica se está ativo (RN-004)
   - Valida senha com BCrypt
                ↓
3. RN-007: Auditoria
   - Registra LOGIN_SUCESSO ou LOGIN_FALHA
   - Registra IP, User-Agent, timestamp
                ↓
4. Gera JWT Token
   - Inclui: email, usuarioId
   - Expira em: 1 hora
                ↓
5. Gera Refresh Token
   - Armazena no banco
   - Expira em: 7 dias
                ↓
6. Retorna TokenResponseDTO
   - accessToken
   - refreshToken
   - expiresIn
                ↓
✅ Usuário autenticado
```

### Fluxo 4: Consultar Perfil (Controle de Acesso)

```
1. Cliente envia GET /api/usuarios/123
   com header X-Usuario-Id: 5
                ↓
2. UsuarioController extrai ambos IDs
   userId = 5 (quem está consultando)
   targetId = 123 (quem quer consultar)
                ↓
3. Se userId ≠ targetId:
   - Chama PermissaoService.verificarAdmin(5)
   - Valida se usuario 5 é ADMIN
                ↓
4. Se USER: lança PermissaoNegadaException
   Se ADMIN: continua
                ↓
5. Retorna dados do usuario 123
                ↓
✅ Consulta autorizada (ou erro 403)
```

## 📁 Estrutura de Arquivos Criados

```
src/main/java/com/authservice/
├── enums/
│   └── TipoPerfil.java           ← Tipos de role
├── model/
│   ├── Perfil.java               ← Entidade perfil
│   └── Usuario.java              ← Atualizado com perfil
├── repository/
│   └── PerfilRepository.java      ← Acesso a perfis
├── service/
│   ├── PermissaoService.java      ← Validação de permissões
│   ├── PerfilInitializer.java     ← Cria roles no startup
│   └── UsuarioService.java        ← Atualizado
├── exception/
│   └── PermissaoNegadaException.java
└── controller/
    └── UsuarioController.java     ← Atualizado com validações

src/main/resources/
├── data.sql                       ← Dados de exemplo
└── application.yaml               ← Existente

docs/
├── ROLES.md                       ← Este arquivo
└── README.md                      ← Atualizado
```

## 🎯 Matriz de Permissões

| Ação | USER | ADMIN |
|------|------|-------|
| Registrar (próprio) | ✅ Sim | ✅ Sim |
| Login | ✅ Sim | ✅ Sim |
| Consultar próprio perfil | ✅ Sim | ✅ Sim |
| Consultar outro perfil | ❌ Não | ✅ Sim |
| Criar usuário | ❌ Não | ✅ Sim |
| Criar admin | ❌ Não | ✅ Sim |
| Modificar usuário | ❌ Não | ✅ Sim |
| Deletar usuário | ❌ Não | ✅ Sim |

## 🛡️ Segurança

### Camadas de Validação

1. **Controller**: Valida header X-Usuario-Id
2. **Service**: Verifica role no banco de dados
3. **Database**: Constraints de integridade
4. **Exception Handler**: Retorna erro 403 apropriado

### Auditoria

- ✅ Todas as tentativas registradas
- ✅ IP do cliente
- ✅ User-Agent
- ✅ Timestamp
- ✅ Descrição de erro

## 🔄 Extensão Futura

Para adicionar novo role "MANAGER":

1. Adicionar em `TipoPerfil.java`
2. Atualizar `PerfilInitializer.java`
3. Criar método em `PermissaoService.java`
4. Adicionar validações em `UsuarioController.java`
5. Atualizar documentação

## 📞 Referências Rápidas

- **RBAC:** Role-Based Access Control (Controle de Acesso Baseado em Função)
- **JWT:** JSON Web Token para autenticação stateless
- **BCrypt:** Algoritmo de hash para senhas
- **Header X-Usuario-Id:** Identifica o usuário autenticado

