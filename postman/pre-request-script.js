// Pre-request Script para Postman
// Este script faz login automaticamente, obtém o JWT token
// e o armazena em uma variável de coleção para uso no Authorization header

// Credenciais de login (ajuste conforme necessário)
const loginEmail = "admin@example.com";   // ADMIN inicial (ADMIN_EMAIL)
const loginSenha = "AdminSenha123";       // ADMIN_SENHA

// Payload do login
const loginPayload = {
  email: loginEmail,
  senha: loginSenha
};

// Configuração da requisição de login
const loginRequest = {
  url: "http://localhost:8080/api/auth/login",
  method: "POST",
  header: {
    "Content-Type": "application/json"
  },
  body: {
    mode: "raw",
    raw: JSON.stringify(loginPayload)
  }
};

// Executar requisição de login
pm.sendRequest(loginRequest, function(err, response) {
  if (err) {
    console.error("Erro ao fazer login:", err);
    pm.test("Login falhou", function() {
      pm.expect.fail("Não foi possível fazer login: " + err.message);
    });
  } else {
    try {
      const responseBody = response.json();
      const accessToken = responseBody.accessToken;

      if (accessToken) {
        // Salvar token em variável de coleção (persiste durante a sessão do Postman)
        pm.collectionVariables.set("jwt_token", accessToken);

        // Opcional: também salvar em variável de ambiente se preferir
        // pm.environment.set("jwt_token", accessToken);

        console.log("✓ Token obtido com sucesso e salvo em: {{jwt_token}}");
        console.log("✓ Email:", loginEmail);
        console.log("✓ Token expira em:", responseBody.expiresIn, "segundos");
      } else {
        console.error("Token não encontrado na resposta");
      }
    } catch (e) {
      console.error("Erro ao processar resposta:", e);
    }
  }
});

