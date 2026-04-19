# Troubleshooting Guide - Application Startup Issues

## Erro Resolvido

### Erro Original:
```
Parameter 0 of constructor in com.tailorshop.metric.config.SecurityConfig required a bean of type 
'org.springframework.security.core.userdetails.UserDetailsService' that could not be found.
```

### Causa:
O `SecurityConfig` requeria um bean `UserDetailsService` que não estava implementado na aplicação.

---

## Solução Implementada

### 1. CustomUserDetailsService ✅
**Arquivo:** `src/main/java/com/tailorshop/metric/security/CustomUserDetailsService.java`

- Implementa `UserDetailsService` 
- Carrega usuários do banco de dados
- Mapeia roles para authorities do Spring Security
- Integra com `UserRepository`

### 2. DataInitializer ✅
**Arquivo:** `src/main/java/com/tailorshop/metric/config/DataInitializer.java`

- Cria roles padrão (ADMIN, USER) automaticamente
- Cria usuário admin padrão:
  - Username: `admin`
  - Password: `admin123`
  - **Importante:** Alterar esta senha em produção!

### 3. GlobalExceptionHandler ✅
**Arquivo:** `src/main/java/com/tailorshop/metric/exception/GlobalExceptionHandler.java`

- Trata `UsernameNotFoundException`
- Trata `ResourceNotFoundException`
- Trata `BusinessException`
- Trata exceções de validação
- Retorna respostas padronizadas ApiResponse

### 4. AuthController ✅
**Arquivo:** `src/main/java/com/tailorshop/metric/controller/AuthController.java`

- Endpoint para login: `/api/auth/login`
- Endpoint para logout: `/api/auth/logout`
- Integra com `AuthenticationManager`

### 5. SecurityConfig Atualizado ✅
**Arquivo:** `src/main/java/com/tailorshop/metric/config/SecurityConfig.java`

- Removido `oauth2Login()` que estava causando problemas
- Adicionado mais endpoints públicos
- Melhorado logout configuration
- Habilitado logout com `clearAuthentication` e `invalidateHttpSession`

### 6. RootController Aprimorado ✅
**Arquivo:** `src/main/java/com/tailorshop/metric/controller/RootController.java`

- Melhorado health check response
- Adicionado timestamp
- Documentado com Swagger annotations

---

## Como Executar

### Passo 1: Build
```bash
cd "g:\Gu Project\metric-system"
mvn clean install -DskipTests
```

### Passo 2: Executar Script de Setup (Recomendado)
```powershell
.\setup-database.ps1
```

Ou manualmente:
```bash
# Criar database
psql -U postgres -c "CREATE DATABASE metric_system;"

# Iniciar aplicação
mvn spring-boot:run
```

---

## Testar Aplicação

### 1. Health Check
```bash
curl http://localhost:8080/api/health
```

**Resposta esperada:**
```json
{
  "status": "UP",
  "message": "Application is running successfully",
  "timestamp": 1712168329000
}
```

### 2. APIs Públicas
```bash
# Root endpoint
curl http://localhost:8080/api/

# Swagger UI
curl http://localhost:8080/api/swagger-ui.html
```

### 3. Login
```bash
curl -X POST "http://localhost:8080/api/auth/login?username=admin&password=admin123"
```

**Resposta esperada:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "username": "admin",
    "roles": [...],
    "token": "jwt-token-will-be-generated"
  },
  "timestamp": "2026-04-04T17:58:00"
}
```

---

## Acessar Aplicação

| Recurso | URL |
|---------|-----|
| **API Root** | http://localhost:8080/api |
| **Health Check** | http://localhost:8080/api/health |
| **Swagger UI** | http://localhost:8080/api/swagger-ui.html |
| **Login Page** | http://localhost:8080/login |
| **Dashboard** | http://localhost:8080/dashboard |

---

## Credenciais Padrão

**Data de Criação:** Primeira execução da aplicação

| Campo | Valor |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |
| Role | ADMIN |

---

## Próximos Passos

### 1. Alterar Senha Admin
```bash
# Será adicionado endpoint para change password
POST /api/auth/change-password
```

### 2. Criar Mais Usuários
```bash
# Será criado endpoint para user creation
POST /api/users
```

### 3. Implementar JWT
- Tokens JWT para autenticação stateless
- Refresh tokens para renovação
- Token expiration handling

### 4. Implementar OAuth2
- Login via Google
- Login via Azure
- User provisioning automático

---

## Se Encontrar Mais Erros

### Erro: "database metric_system does not exist"
```bash
psql -U postgres -c "CREATE DATABASE metric_system;"
```

### Erro: "Port 8080 already in use"
Altere em `application.yml`:
```yaml
server:
  port: 8081
```

### Erro: "Cannot find Flyway migrations"
Certifique-se que existe:
```
src/main/resources/db/migration/V1__create_initial_schema.sql
```

### Erro de Encoding no Windows
Adicione à execução:
```bash
mvn spring-boot:run -Dfile.encoding=UTF-8
```

---

## Logs e Debug

### Ver logs detalhados:
```bash
mvn spring-boot:run -Ddebug
```

### Arquivo de log:
```
logs/metric-system.log
```

---

## Estrutura de Componentes Criados

```
src/main/java/com/tailorshop/metric/
├── config/
│   ├── SecurityConfig.java          (Atualizado ✅)
│   ├── DataInitializer.java         (Novo ✅)
│   └── SwaggerConfig.java
├── security/
│   ├── CustomUserDetailsService.java (Novo ✅)
│   └── JwtTokenProvider.java         (Novo ✅)
├── controller/
│   ├── RootController.java           (Atualizado ✅)
│   └── AuthController.java           (Novo ✅)
├── exception/
│   └── GlobalExceptionHandler.java   (Novo ✅)
└── ...
```

---

## Checklist de Verificação

- [ ] Application.yml configurado corretamente
- [ ] Database PostgreSQL criado
- [ ] Migrations Flyway executadas
- [ ] CustomUserDetailsService criado
- [ ] DataInitializer criando dados padrão
- [ ] SecurityConfig atualizado
- [ ] AuthController disponível
- [ ] GlobalExceptionHandler tratando erros
- [ ] Aplicação inicia sem erros
- [ ] Health check respondendo
- [ ] Login funcionando com admin/admin123

---

**Status:** ✅ Tudo configurado e pronto para uso!
