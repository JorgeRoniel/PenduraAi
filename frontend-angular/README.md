# Pendura Aí — frontend Angular

Frontend oficial do Pendura Aí. O React em `Projeto-web/` foi mantido apenas como legado temporário. A publicação no Render usa o `Dockerfile` da raiz para servir Angular e API no mesmo endereço.

## Requisitos

- Node.js 20 ou superior
- npm
- API Spring Boot e banco disponíveis para testar os fluxos autenticados

## Desenvolvimento local

Na raiz do repositório, inicie a infraestrutura:

```bash
docker compose up
```

Em outro terminal:

```bash
cd frontend-angular
npm ci
npm start
```

A aplicação ficará em `http://localhost:4200` e o ambiente de desenvolvimento usa `http://localhost:8080`, configurado em `src/environments/environment.ts`.

## Build de produção

Execute `npm run build -- --configuration production`. O bundle de produção usa a API em `/api` no mesmo endereço. Para a imagem completa de publicação, execute `docker build -t pendura-ai:local .` na raiz do projeto.

## Validação

```bash
npm run build -- --configuration production
npm test -- --watch=false --browsers=ChromeHeadless
```

Os testes usam a API local configurada em `src/environments/environment.ts`.

## Funcionalidades

- Login e cadastro com Reactive Forms.
- Sessão por cookies `HttpOnly`, com renovação de sessão.
- Proteção de rotas públicas e privadas.
- Pesquisa paginada, cadastro, atualização e quitação de dívidas.
- Feedbacks de carregamento, sucesso, erro e lista vazia.
- Layout responsivo e modais acessíveis.

Os endpoints continuam centralizados em `src/app/core/api/api-endpoints.ts`, preservando os contratos da API Spring Boot.
