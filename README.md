# Pendura Aí

Aplicação para organizar clientes e dívidas de pequenos comércios. O frontend oficial é o Angular, servido por Nginx; o frontend React em `Projeto-web/` permanece no repositório apenas como legado temporário.

## Tecnologias

- Angular + TypeScript
- Spring Boot
- PostgreSQL
- Docker e Docker Compose

## Executar a aplicação publicada

Pré-requisitos: Docker Desktop ou Docker Engine e Docker Compose.

Configure um arquivo `.env` na raiz:

```dotenv
POSTGRES_PORT=5432
DB_NAME=pendura_ai
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://postgres:5432/pendura_ai
JWT_SECRET=uma-chave-secreta-local
BACKEND_PORT=8080
FRONTEND_PORT=4200
```

Suba banco, API e frontend Angular:

```bash
docker compose up -d
```

A aplicação ficará em `http://localhost:${FRONTEND_PORT}` e a API em `http://localhost:${BACKEND_PORT}`.
