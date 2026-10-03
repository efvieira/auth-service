# 🔗 Exemplo de Integração com Postman

## 📦 Importar OpenAPI no Postman

### Método 1: Via Link Direto
1. Abra Postman
2. Clique em **File** → **Import**
3. Selecione a aba **Link**
4. Cole: `http://localhost:8080/api-docs`
5. Clique em **Import**

### Método 2: Via Arquivo JSON
```bash
# 1. Baixe a especificação
curl http://localhost:8080/api-docs > openapi.json

# 2. No Postman:
# File → Import → Selecione openapi.json
```

## 📋 Coleção Postman Manualmente

### 1. Criar Collection
- Nome: **Auth Service API**

### 2. Adicionar Requisições

#### Registrar Usuário
```
POST http://localhost:8080/api/auth/registrar
Content-Type: application/json

{
  "nome": "João Silva",
  "email": "joao@example.com",
  "senha": "Senha123"
}
```

#### Login
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "joao@example.com",
  "senha": "Senha123"
}
```

**Guardar o Response:**
- Clique em **Tests**
- Adicione:
```javascript
var jsonData = pm.response.json();
pm.environment.set("accessToken", jsonData.accessToken);
pm.environment.set("refreshToken", jsonData.refreshToken);
pm.environment.set("usuarioId", 1);
```

#### Obter Usuário
```
GET http://localhost:8080/api/usuarios/1

Headers:
X-Usuario-Id: {{usuarioId}}
```

#### Criar Usuário (Admin)
```
POST http://localhost:8080/api/usuarios
Content-Type: application/json

Headers:
X-Usuario-Id: {{usuarioId}}

{
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "senha": "Senha456",
  "ativo": true,
  "perfil": "USER"
}
```

## 🌐 Variáveis de Ambiente

### Criar Environment
1. Clique em **Environment** (engrenagem)
2. Clique em **+** para criar novo
3. Nome: **Auth Service Dev**

### Variáveis
```json
{
  "baseUrl": "http://localhost:8080",
  "accessToken": "",
  "refreshToken": "",
  "usuarioId": "1"
}
```

### Usar nas Requisições
```
{{baseUrl}}/api/auth/login
Header: X-Usuario-Id: {{usuarioId}}
```

## 🧪 Scripts de Teste

### Login - Extrair Tokens
```javascript
// Tests
const response = pm.response.json();
pm.environment.set("accessToken", response.accessToken);
pm.environment.set("refreshToken", response.refreshToken);
pm.expect(response).to.have.property("accessToken");
pm.expect(response.tokenType).to.equal("Bearer");
```

### Validar Status
```javascript
pm.test("Status is 200", function () {
    pm.response.to.have.status(200);
});
```

### Validar Structure
```javascript
pm.test("Response has required fields", function () {
    const response = pm.response.json();
    pm.expect(response).to.have.property("id");
    pm.expect(response).to.have.property("email");
});
```

## 📊 Workflow Automático

### Teste Completo
1. **Registrar** → Salvar usuário
2. **Login** → Salvar tokens
3. **Obter** → Consultar perfil
4. **Criar** → Tenta criar (deve falhar se USER)
5. **Refresh** → Renovar token
6. **Logout** → Finalizar

## 🚀 Executar Coleção

### Via Postman
1. Clique com direito na Collection
2. **Run Collection**
3. Observe os testes passando/falhando

### Via Newman (CLI)
```bash
npm install -g newman

newman run openapi.json \
  -e auth-dev-env.json \
  -r cli,json
```

## 💡 Dicas de Teste

✅ **Use variáveis** para IDs e tokens  
✅ **Sempre registre** antes de testar  
✅ **Valide respostas** com testes  
✅ **Teste casos de erro** também  
✅ **Documente** o fluxo esperado  

---

**Pronto para começar!** 🎉

