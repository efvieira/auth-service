# 📚 Documentação Swagger/OpenAPI

## 🎯 Visão Geral

O Auth Service utiliza **Springdoc OpenAPI** (anteriormente conhecido como Swagger 3) para gerar documentação automática da API em tempo real.

## 🚀 Acessando o Swagger

### URL do Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### URL da Documentação JSON (OpenAPI)
```
http://localhost:8080/api-docs
```

## 📖 Estrutura da Documentação

### Grupo 1: Autenticação
Endpoints para autenticação e autorização de usuários.

#### Endpoints:
- `POST /api/auth/registrar` - Registrar novo usuário
- `POST /api/auth/login` - Fazer login
- `POST /api/auth/refresh` - Renovar token
- `POST /api/auth/logout` - Fazer logout

### Grupo 2: Usuários
Endpoints para gerenciamento de usuários.

#### Endpoints:
- `POST /api/usuarios` - Criar novo usuário (ADMIN only)
- `GET /api/usuarios/{id}` - Obter usuário por ID
- `GET /api/usuarios/email/{email}` - Obter usuário por email

## 📝 Exemplo de Endpoint Documentado

### POST /api/auth/login

**Descrição:**
Autentica o usuário com email e senha, retornando Access Token e Refresh Token.

**Request Body:**
```json
{
  "email": "joao@example.com",
  "senha": "Senha123"
}
```

**Responses:**
- **200 OK**: Login realizado com sucesso
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

- **404 Not Found**: Usuário não encontrado
- **403 Forbidden**: Usuário inativo ou credenciais inválidas

## 🔐 Segurança no Swagger

### Headers de Autenticação
Alguns endpoints requerem o header `X-Usuario-Id`:

```
X-Usuario-Id: 1
```

Este header deve ser incluído em requisições para:
- `POST /api/usuarios` - Criar usuário
- `GET /api/usuarios/{id}` - Obter usuário
- `GET /api/usuarios/email/{email}` - Obter por email

## 🎨 Personalização do Swagger

### Arquivo de Configuração
`src/main/java/com/authservice/config/SwaggerConfig.java`

### Informações da API
```yaml
title: "Auth Service API"
description: "Serviço robusto de autenticação e autorização com controle de acesso baseado em roles"
version: "1.0.0"
```

### Servidores Configurados
- **Desenvolvimento**: http://localhost:8080
- **Produção**: https://api.authservice.com

## 📊 Anotações Utilizadas

### No Controller
```java
@Tag(name = "Autenticação", description = "Endpoints para autenticação...")
@Operation(summary = "Fazer login", description = "Autentica o usuário...")
@ApiResponses(value = { ... })
```

### No DTO
```java
@Schema(description = "Email do usuário", example = "joao@example.com")
```

### No Parâmetro
```java
@Parameter(description = "ID do usuário a consultar")
@PathVariable Long id
```

## 🔍 Testando via Swagger

### 1. Registrar Usuário
1. Abra http://localhost:8080/swagger-ui.html
2. Navegue até "Autenticação" → "POST /api/auth/registrar"
3. Clique em "Try it out"
4. Preencha o formulário:
   ```json
   {
     "nome": "João Silva",
     "email": "joao@example.com",
     "senha": "Senha123"
   }
   ```
5. Clique em "Execute"

### 2. Fazer Login
1. Navegue até "Autenticação" → "POST /api/auth/login"
2. Clique em "Try it out"
3. Preencha o formulário:
   ```json
   {
     "email": "joao@example.com",
     "senha": "Senha123"
   }
   ```
4. Clique em "Execute"
5. Copie o `accessToken` retornado

### 3. Obter Usuário
1. Navegue até "Usuários" → "GET /api/usuarios/{id}"
2. Clique em "Try it out"
3. Preencha o campo `id` com `1`
4. Em "X-Usuario-Id" (header), cole o ID do usuário autenticado
5. Clique em "Execute"

## 📥 Download da Documentação

### OpenAPI JSON
```
http://localhost:8080/api-docs
```

Salve este arquivo para usar com ferramentas como:
- Postman
- Insomnia
- Stoplight
- Redoc

### Exemplo de Download com cURL
```bash
curl http://localhost:8080/api-docs > openapi.json
```

## 🛠️ Ferramentas Externas que Usam OpenAPI

### 1. **Postman**
- Importe o arquivo `openapi.json`
- Automaticamente cria coleções de requisições

### 2. **Insomnia**
- Pasta → Importar → OpenAPI 3.0
- Cole a URL: http://localhost:8080/api-docs

### 3. **Redoc** (Documentação estática)
```html
<!DOCTYPE html>
<html>
<head>
  <title>Auth Service API</title>
  <script src="https://cdn.jsdelivr.net/npm/redoc@latest/bundles/redoc.standalone.js"></script>
  <style>
    body { margin: 0; padding: 0; }
  </style>
</head>
<body>
  <redoc spec-url='http://localhost:8080/api-docs'></redoc>
</body>
</html>
```

## 📋 Estrutura de Respostas

### Sucesso (2xx)
```json
{
  "id": 1,
  "nome": "João Silva",
  "email": "joao@example.com",
  "ativo": true,
  "perfil": {
    "id": 2,
    "tipo": "USER",
    "descricao": "Usuário - Pode autenticar...",
    "ativo": true
  }
}
```

### Erro (4xx/5xx)
```json
{
  "timestamp": "2026-08-16T14:30:22",
  "status": 403,
  "mensagem": "Acesso negado. Apenas administradores podem realizar esta ação."
}
```

## 🔄 Fluxo de Teste Completo

1. **Registrar**
   - POST /api/auth/registrar
   - Recebe perfil USER

2. **Login**
   - POST /api/auth/login
   - Guarde os tokens

3. **Consultar Perfil**
   - GET /api/usuarios/1
   - Use X-Usuario-Id: 1

4. **Tentar Criar Usuário (deve falhar)**
   - POST /api/usuarios
   - Resposta: 403 Forbidden

5. **Renovar Token**
   - POST /api/auth/refresh
   - Use o refreshToken

6. **Logout**
   - POST /api/auth/logout
   - Revoga o token

## 📱 Responsivo

O Swagger UI é totalmente responsivo e funciona em:
- ✅ Desktop
- ✅ Tablet
- ✅ Mobile

## 🎨 Temas

Configuração no `application.yaml`:
```yaml
springdoc:
  swagger-ui:
    theme: "light"  # ou "dark"
```

## 🚀 Recursos Adicionais

- **Syntax Highlight**: Colorização de código JSON
- **Request Duration**: Mostra tempo de resposta
- **Filter**: Busca por operações
- **Try it Out**: Executa requisições direto do navegador

## 📞 Contato e Suporte

Para dúvidas sobre a documentação, consulte:
- README.md
- INSTALACAO.md
- ROLES.md

## ✨ Benefícios do Swagger

✅ Documentação atualizada automaticamente  
✅ Testes direto do navegador  
✅ Geração de SDKs  
✅ Versionamento de API  
✅ Integração com ferramentas de teste  
✅ Padrão OpenAPI 3.0  

---

**Última atualização:** 16 de Agosto de 2026  
**Versão Swagger:** 3.0  
**Status:** ✅ Funcional

