# Guia de Instalação - Auth Service

## Pré-requisitos

Antes de começar, certifique-se de que tem instalado:

- **Java Development Kit (JDK) 17 ou superior**
  - Download: https://www.oracle.com/java/technologies/downloads/
  - Verifique: `java -version`

- **Maven 3.8.1 ou superior**
  - Download: https://maven.apache.org/download.cgi
  - Instruções de instalação: https://maven.apache.org/install.html
  - Verifique: `mvn -version`

- **Git** (opcional, para clonar o repositório)
  - Download: https://git-scm.com/

## Passo a Passo

### 1. Clone ou Abra o Projeto

Se estiver usando Git:
```bash
git clone <repository-url>
cd auth-service
```

Ou abra a pasta do projeto no seu IDE.

### 2. Configure as Variáveis de Ambiente (Opcional)

Para mudar a chave secreta do JWT, edite `src/main/resources/application.yaml`:

```yaml
jwt:
  secret: sua-chave-secreta-muito-segura-com-minimo-256-bits
  expiration: 3600000  # 1 hora em ms
  refresh-expiration: 604800000  # 7 dias em ms
```

### 3. Compile o Projeto

```bash
mvn clean compile
```

### 4. Execute os Testes (Opcional)

```bash
mvn test
```

### 5. Construa o Projeto

```bash
mvn clean package
```

Será gerado um arquivo `.jar` em `target/auth-service-1.0-SNAPSHOT.jar`

### 6. Execute a Aplicação

#### Opção A: Com Maven
```bash
mvn spring-boot:run
```

#### Opção B: Com Java
```bash
java -jar target/auth-service-1.0-SNAPSHOT.jar
```

### 7. Verifique se está Rodando

A aplicação estará disponível em: `http://localhost:8080`

Para acessar o console do banco de dados H2:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- User: `sa`
- Password: (deixe em branco)

## Usar um Banco de Dados Externo

Para usar MySQL em vez de H2, configure o `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_service
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: seu_usuario
    password: sua_senha
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update
```

E adicione ao `pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

## Troubleshooting

### Erro: "Maven not found"
- Instale Maven conforme o link acima
- Adicione o diretório `bin` do Maven ao PATH do sistema

### Erro: "Java not found"
- Instale o JDK conforme o link acima
- Adicione o diretório `bin` do JDK ao PATH do sistema

### Porta 8080 já está em uso
- Mude a porta em `application.yaml`:
```yaml
server:
  port: 8081
```

### Banco de dados não inicializa
- Limpe a compilação: `mvn clean`
- Delete a pasta `target`: `rm -rf target` (Linux/Mac) ou `rmdir /s target` (Windows)

## Estrutura do Projeto

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/authservice/
│   │   │   ├── controller/        # Endpoints REST
│   │   │   ├── service/           # Lógica de negócio
│   │   │   ├── model/             # Entidades JPA
│   │   │   ├── repository/        # Acesso a dados
│   │   │   ├── security/          # JWT e segurança
│   │   │   ├── validator/         # Validações
│   │   │   ├── dto/               # Transfer objects
│   │   │   ├── exception/         # Exceções custom
│   │   │   ├── config/            # Configurações
│   │   │   ├── handler/           # Tratamento de erros
│   │   │   └── AuthServiceApplication.java
│   │   └── resources/
│   │       └── application.yaml   # Configurações
│   └── test/                      # Testes
├── pom.xml                        # Dependências Maven
└── README.md                      # Documentação

```

## Exemplos de Requisições

### 1. Registrar um novo usuário (Recebe perfil USER automaticamente)
```bash
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao@example.com",
    "senha": "Senha123"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "senha": "Senha123"
  }'
```

### 3. Criar um usuário ADMIN (Apenas ADMIN pode fazer isso)
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

### 4. Criar um usuário USER (Apenas ADMIN pode fazer isso)
```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 1" \
  -d '{
    "nome": "Maria Silva",
    "email": "maria@example.com",
    "senha": "Senha456",
    "ativo": true,
    "perfil": "USER"
  }'
```

### 5. Obter usuário por ID (Requer X-Usuario-Id)
```bash
# USER só pode buscar seu próprio ID
curl http://localhost:8080/api/usuarios/1 \
  -H "X-Usuario-Id: 1"

# ADMIN pode buscar qualquer ID
curl http://localhost:8080/api/usuarios/2 \
  -H "X-Usuario-Id: 1"
```

### 6. Obter usuário por email (Requer X-Usuario-Id)
```bash
curl http://localhost:8080/api/usuarios/email/joao@example.com \
  -H "X-Usuario-Id: 1"
```

### 7. Renovar token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "seu_refresh_token_aqui"
  }'
```

### 8. Fazer logout
```bash
curl -X POST http://localhost:8080/api/auth/logout?usuarioId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "seu_refresh_token_aqui"
  }'
```

## Perfis e Permissões

### ADMIN
- ✅ Criar novos usuários
- ✅ Consultar qualquer perfil
- ✅ Gerenciar usuários
- ✅ Acesso completo

### USER
- ✅ Autenticar (login)
- ✅ Consultar seu próprio perfil
- ❌ Criar usuários
- ❌ Consultar perfis de outros usuários

## Tratamento de Erros

### Erro 403 - Permissão Negada
```json
{
  "timestamp": "2026-08-16T10:30:00",
  "status": 403,
  "mensagem": "Acesso negado. Apenas administradores podem realizar esta ação."
}
```

### Erro 409 - Email já existe
```json
{
  "timestamp": "2026-08-16T10:30:00",
  "status": 409,
  "mensagem": "Email já existe: joao@example.com"
}
```

### Erro 400 - Senha inválida
```json
{
  "timestamp": "2026-08-16T10:30:00",
  "status": 400,
  "mensagem": "Senha deve ter no mínimo 8 caracteres"
}
```

## Próximos Passos

1. Teste os endpoints usando Postman, Thunder Client ou cURL
2. Configure o banco de dados externo (MySQL, PostgreSQL)
3. Implemente filtros de segurança com Spring Security
4. Adicione mais testes unitários e de integração
5. Configure CI/CD pipeline
6. Deploy em produção

## Suporte

Para dúvidas sobre a instalação, consulte:
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Maven Docs](https://maven.apache.org/guides/index.html)
- [Java Docs](https://docs.oracle.com/en/java/javase/17/)

