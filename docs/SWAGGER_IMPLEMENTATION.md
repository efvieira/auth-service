# 🎉 Swagger/OpenAPI - Implementação Completa

## ✅ O que foi implementado

### 1. **Dependência Springdoc OpenAPI**
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.0.2</version>
</dependency>
```

### 2. **Configuração do Swagger**
- Arquivo: `SwaggerConfig.java`
- Título: Auth Service API
- Descrição: Serviço de autenticação com RBAC
- Versão: 1.0.0
- Servidores: Dev (localhost:8080) e Prod

### 3. **Anotações nos Controllers**
Todos os endpoints documentados com:
- `@Tag` - Grupo de operações
- `@Operation` - Descrição da operação
- `@ApiResponse` - Respostas possíveis
- `@Parameter` - Parâmetros e headers
- `@Schema` - Estrutura de dados

### 4. **Anotações nos DTOs**
Todos os objetos de transferência documentados com:
- `@Schema` - Descrição da estrutura
- Exemplos de valores
- Validações (Email, NotBlank, etc)

### 5. **Configuração no application.yaml**
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    theme: light
    filter: true
```

## 🌐 URLs de Acesso

| Recurso | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/api-docs |
| **OpenAPI YAML** | http://localhost:8080/api-docs.yaml |

## 📚 Documentação dos Endpoints

### Autenticação

#### POST /api/auth/registrar
- **Descrição**: Registrar novo usuário
- **Validações**: Email único, senha forte
- **Resposta**: Usuário com perfil USER
- **Status**: 201 Created, 409 Conflict, 400 Bad Request

#### POST /api/auth/login
- **Descrição**: Fazer login
- **Params**: Email, Senha
- **Resposta**: Access Token, Refresh Token
- **Status**: 200 OK, 403 Forbidden, 404 Not Found

#### POST /api/auth/refresh
- **Descrição**: Renovar Access Token
- **Params**: Refresh Token
- **Resposta**: Novo Access Token
- **Status**: 200 OK, 401 Unauthorized

#### POST /api/auth/logout
- **Descrição**: Fazer logout
- **Params**: usuarioId (query), Refresh Token (body)
- **Status**: 200 OK, 401 Unauthorized

### Usuários

#### POST /api/usuarios
- **Descrição**: Criar novo usuário (ADMIN only)
- **Headers**: X-Usuario-Id (obrigatório)
- **Validações**: ADMIN, email único, senha forte
- **Resposta**: Novo usuário com perfil
- **Status**: 201 Created, 403 Forbidden, 409 Conflict

#### GET /api/usuarios/{id}
- **Descrição**: Obter usuário por ID
- **Headers**: X-Usuario-Id (opcional)
- **Restrições**: USER só pode ver seu próprio perfil
- **Status**: 200 OK, 403 Forbidden, 404 Not Found

#### GET /api/usuarios/email/{email}
- **Descrição**: Obter usuário por email
- **Headers**: X-Usuario-Id (opcional)
- **Restrições**: USER só pode ver seu próprio email
- **Status**: 200 OK, 403 Forbidden, 404 Not Found

## 🎯 Recursos

### ✅ Características Implementadas
- [x] Documentação automática
- [x] Swagger UI interativo
- [x] OpenAPI 3.0 completo
- [x] Exemplos de requisição/resposta
- [x] Validações documentadas
- [x] Headers documentados
- [x] Códigos de status documentados
- [x] Tema claro/escuro
- [x] Filtro de operações
- [x] Download em JSON/YAML

### ✅ Integrações Suportadas
- [x] Postman
- [x] Insomnia
- [x] Redoc
- [x] Swagger Editor
- [x] Newman (CLI)

## 🚀 Como Usar

### 1. Iniciar a Aplicação
```bash
cd C:\workspace\untitled
mvn clean install
mvn spring-boot:run
```

### 2. Acessar Swagger UI
Abra no navegador:
```
http://localhost:8080/swagger-ui.html
```

### 3. Testar um Endpoint
1. Expanda um grupo (ex: "Autenticação")
2. Clique em um endpoint
3. Clique em "Try it out"
4. Preencha os campos
5. Clique em "Execute"

### 4. Visualizar Resposta
- Status HTTP
- Tempo de resposta
- Headers
- Body JSON

## 📊 Exemplo Visual

```
┌─────────────────────────────────────────┐
│         Swagger UI Home                 │
├─────────────────────────────────────────┤
│ Auth Service API v1.0.0                 │
├─────────────────────────────────────────┤
│ ✓ Autenticação                          │
│   ├─ POST   /api/auth/registrar        │
│   ├─ POST   /api/auth/login            │
│   ├─ POST   /api/auth/refresh          │
│   └─ POST   /api/auth/logout           │
│                                         │
│ ✓ Usuários                              │
│   ├─ POST   /api/usuarios              │
│   ├─ GET    /api/usuarios/{id}         │
│   └─ GET    /api/usuarios/email/{email}│
└─────────────────────────────────────────┘
```

## 📥 Importar em Outras Ferramentas

### Postman
```
File → Import → Link
URL: http://localhost:8080/api-docs
```

### Insomnia
```
Pasta → Importar → OpenAPI 3.0
URL: http://localhost:8080/api-docs
```

### Redoc (Documentação Estática)
Crie um arquivo HTML:
```html
<html>
  <head>
    <script src="https://cdn.jsdelivr.net/npm/redoc@latest/bundles/redoc.standalone.js"></script>
  </head>
  <body>
    <redoc spec-url='http://localhost:8080/api-docs'></redoc>
  </body>
</html>
```

## 🔒 Segurança no Swagger

### Headers Documentados
Todos os endpoints que requerem autenticação têm o header `X-Usuario-Id` documentado.

### Exemplo no Swagger
```
X-Usuario-Id (header) - ID do usuário autenticado [Required]
```

### Testando com Headers
1. No Swagger UI, procure por "X-Usuario-Id"
2. Preencha com o valor (ex: 1)
3. Clique em "Execute"

## 📋 Checklist de Teste

- [ ] Abrir Swagger UI
- [ ] Registrar usuário
- [ ] Fazer login
- [ ] Copiar accessToken
- [ ] Obter usuário por ID
- [ ] Tentar criar usuário (deve falhar se USER)
- [ ] Renovar token
- [ ] Fazer logout
- [ ] Importar no Postman
- [ ] Exportar OpenAPI JSON

## 🎨 Customizações Disponíveis

### Tema
```yaml
theme: "light"  # ou "dark"
```

### Syntax Highlight
```yaml
syntax-highlight: "highlight"
```

### Filtro
```yaml
filter: true  # ou false
```

### Request Duration
```yaml
display-request-duration: true
```

## 🔗 Links Úteis

- [Springdoc Docs](https://springdoc.org/)
- [OpenAPI 3.0 Spec](https://spec.openapis.org/oas/v3.0.3)
- [Swagger Annotations](https://swagger.io/docs/open-api/annotations-javadoc/)
- [Postman Import](https://learning.postman.com/docs/integrations/available-integrations/working-with-openapi/)

## ✨ Próximos Passos

1. ✅ Testar no Swagger UI
2. ✅ Importar no Postman
3. ✅ Gerar documentação estática com Redoc
4. ✅ Usar para testes automatizados
5. ✅ Compartilhar com equipe

## 📞 Suporte

Para dúvidas:
- Leia [SWAGGER.md](SWAGGER.md)
- Leia [POSTMAN.md](POSTMAN.md)
- Consulte [README.md](../README.md)

---

**Status**: ✅ Implementado e Funcional  
**Versão**: 1.0.0  
**Data**: 16 de Agosto de 2026

