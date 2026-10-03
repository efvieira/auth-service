# Auth Service

[![CI](https://github.com/efvieira/auth-service/actions/workflows/ci.yml/badge.svg)](https://github.com/efvieira/auth-service/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1-6DB33F?logo=springboot&logoColor=white)
![DynamoDB](https://img.shields.io/badge/AWS-DynamoDB-4053D6?logo=amazondynamodb&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?logo=jsonwebtokens&logoColor=white)
![Coverage](https://img.shields.io/badge/cobertura-%E2%89%A590%25-brightgreen)
[![License: MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-blue.svg)](LICENSE)

Serviço de Autenticação e Autorização desenvolvido com Spring Boot 3, JWT e DynamoDB.

### ✨ Destaques

- 🔐 **JWT com access + refresh token**, com rotação, revogação no logout e rejeição de tokens expirados
- 👥 **RBAC** (perfis `ADMIN` / `USER`), com perfil recarregado do banco a cada requisição
- 📊 **Auditoria** de todas as autenticações (IP, User-Agent, sucesso/falha)
- 🗄️ **DynamoDB** via Enhanced Client, com tabelas e GSIs criadas automaticamente
- 📘 **Swagger / OpenAPI** com autenticação Bearer integrada
- 🧪 **Cobertura de testes ≥ 90%** exigida no build (JaCoCo) e verificada no CI
- 🐳 **Ambiente local com um comando** (`docker compose up -d`, via LocalStack)

### 🛠️ Stack

Java 17 · Spring Boot 3 · Spring Security · JJWT · AWS SDK v2 (DynamoDB) · springdoc-openapi · Lombok · JUnit 5 · Mockito · JaCoCo · Docker / LocalStack · GitHub Actions

## 📋 Sobre o Projeto

O **Auth Service** gerencia identidade, autenticação e autorização de usuários, com controle de acesso baseado em perfis (RBAC) e auditoria de todas as autenticações.

## 📋 Regras de Negócio

- **RN-001**: Usuário deve possuir e-mail único (comparação sem diferenciar maiúsculas/minúsculas).
- **RN-002**: Senha deve ser armazenada utilizando BCrypt.
- **RN-003**: Senha deve conter: mínimo 8 caracteres, 1 letra maiúscula e 1 número.
- **RN-004**: Usuário inativo não pode autenticar (nem usar tokens já emitidos).
- **RN-005**: JWT expirado deve ser rejeitado.
- **RN-006**: Refresh Token deve ser validado antes da emissão de novo JWT.
- **RN-007**: Toda autenticação deve ser auditada.

## 🔐 Perfis e Permissões

| Ação | USER | ADMIN |
|---|---|---|
| Registrar-se (`POST /api/auth/registrar`) | ✅ público | ✅ público |
| Login / refresh | ✅ público | ✅ público |
| Consultar o próprio perfil | ✅ | ✅ |
| Consultar outros usuários | ❌ 403 | ✅ |
| Criar usuários (inclusive ADMIN) | ❌ 403 | ✅ |

O usuário é identificado **exclusivamente pelo access token** (`Authorization: Bearer <token>`). O perfil é recarregado do banco a cada requisição, então desativar um usuário ou mudar seu perfil vale imediatamente.

## 🏗️ Arquitetura

```
com.authservice
├── config       # Security (filter chain, CORS, BCrypt), DynamoDB, Swagger, criação de tabelas
├── controller   # AutenticacaoController (/api/auth), UsuarioController (/api/usuarios)
├── service      # Regras de negócio + inicializadores de perfis e do ADMIN inicial
├── repository   # Acesso ao DynamoDB (Enhanced Client)
├── model        # Itens DynamoDB (@DynamoDbBean)
├── security     # JwtTokenProvider, JwtAuthenticationFilter, UsuarioAutenticado
├── dto / enums / exception / validator
└── handler      # Tratamento global de erros (formato JSON único)
```

### Tabelas DynamoDB

| Tabela | Chave | Índices (GSI) |
|---|---|---|
| `perfis` | `id` | `tipo-index` |
| `usuarios` | `id` | `email-index` |
| `refresh_tokens` | `id` | `token-index`, `usuario-index` |
| `auditoria_autenticacao` | `id` | `usuario-index` (`usuario_id` + `data_hora`) |

Na inicialização, a aplicação executa, nesta ordem:
1. **Cria as tabelas** que não existirem (desative com `AWS_DYNAMODB_CRIAR_TABELAS=false`);
2. **Cria os perfis** ADMIN e USER;
3. **Cria o ADMIN inicial** (`ADMIN_EMAIL` / `ADMIN_SENHA`), caso ainda não exista.

## 🚀 Começando

### Pré-requisitos

- Java 17+
- Docker (para o LocalStack)
- Maven não é necessário: use o wrapper `mvnw`

### Executando localmente

```bash
# 1. Subir o DynamoDB local (LocalStack)
docker compose up -d

# 2. Rodar a aplicação
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html (use o botão **Authorize** com o access token)
- ADMIN inicial padrão: `admin@example.com` / `AdminSenha123`

### Configuração (variáveis de ambiente)

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | valor de desenvolvimento | **Obrigatório em produção.** Mínimo de 32 bytes |
| `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | `3600000` / `604800000` | Validade em ms (1 h / 7 dias) |
| `AWS_DYNAMODB_ENDPOINT` | `http://localhost:4566` | Endpoint do DynamoDB |
| `AWS_REGION` | `us-east-1` | Região |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | `test` / `test` | Credenciais |
| `AWS_DYNAMODB_CRIAR_TABELAS` | `true` | Cria as tabelas na inicialização |
| `ADMIN_EMAIL` / `ADMIN_SENHA` / `ADMIN_NOME` | `admin@example.com` / `AdminSenha123` / `Administrador` | ADMIN inicial (`ADMIN_EMAIL` vazio desativa) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:8080` | Origens permitidas |

## 📡 API Endpoints

### Públicos

```http
POST /api/auth/registrar
{ "nome": "João Silva", "email": "joao@example.com", "senha": "Senha123" }
→ 201 { "id": "...", "nome": "João Silva", "email": "joao@example.com", "ativo": true, "perfil": { "tipo": "USER", ... } }

POST /api/auth/login
{ "email": "joao@example.com", "senha": "Senha123" }
→ 200 { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 3600 }

POST /api/auth/refresh
{ "refreshToken": "..." }
→ 200 { "accessToken": "<novo>", "refreshToken": "...", ... }
```

### Autenticados (`Authorization: Bearer <accessToken>`)

```http
POST /api/auth/logout            { "refreshToken": "..." }   → revoga o refresh token
GET  /api/usuarios/me                                         → meu perfil
GET  /api/usuarios/{id}                                       → USER: só o próprio | ADMIN: qualquer
GET  /api/usuarios/email/{email}                              → USER: só o próprio | ADMIN: qualquer
POST /api/usuarios               (ADMIN)
     { "nome": "Maria", "email": "maria@example.com", "senha": "Senha123", "perfil": "ADMIN", "ativo": true }
```

### Formato de erro

```json
{ "timestamp": "2026-10-03T10:00:00", "status": 400, "mensagem": "Dados inválidos",
  "erros": { "email": "Email deve ser válido" } }
```

| Status | Quando |
|---|---|
| 400 | Dados inválidos / senha fraca |
| 401 | Credenciais inválidas, token ausente/inválido/expirado, refresh token inválido ou revogado |
| 403 | Usuário inativo ou sem permissão |
| 404 | Usuário não encontrado |
| 409 | Email já existe |

## 📊 Auditoria

Toda autenticação é registrada em `auditoria_autenticacao` com: tipo (`LOGIN_SUCESSO`, `LOGIN_FALHA`, `LOGOUT`, `TOKEN_REFRESH`, `TOKEN_REFRESH_FALHA`), IP (respeitando `X-Forwarded-For`), User-Agent, data/hora e descrição do erro.

## 🧪 Testes

```bash
./mvnw test      # roda os testes e gera o relatório de cobertura em target/site/jacoco/index.html
./mvnw verify    # idem, e falha se a cobertura de linhas ou branches ficar abaixo de 90%
```

Os testes não precisam do DynamoDB, pois os repositórios são testados com o Enhanced Client simulado. Eles cobrem:
- a validação de senha;
- o JWT (expiração, assinatura, access × refresh);
- o fluxo de autenticação;
- os repositórios e os inicializadores;
- o tratamento de erros;
- as regras de acesso dos endpoints com a configuração de segurança real.

O código gerado pelo Lombok fica fora da cobertura (`lombok.config`).

## 📚 Documentação complementar

Guias detalhados (perfis, Swagger, Postman, instalação) estão em [`docs/`](docs/INDICE.md). A coleção de apoio do Postman está em [`postman/`](postman/) e há exemplos de payloads em [`payloads/`](payloads/).

## 📄 Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

## 👤 Autor

**efvieira**: [github.com/efvieira](https://github.com/efvieira)
