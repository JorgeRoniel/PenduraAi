# Pendura Aí — frontend Angular

Frontend oficial do Pendura Aí. O React em `Projeto-web/` foi mantido apenas como legado temporário; a distribuição oficial usa a imagem Angular/Nginx publicada como `jorgeroniel07/pendura-ai-client:0.0.5`.

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

## Build Docker de produção

`API_URL` é obrigatório e fica incorporado ao bundle Angular. Use uma URL acessível pelo navegador:

```bash
docker build \
  --build-arg API_URL=https://api.exemplo.com \
  -t jorgeroniel07/pendura-ai-client:0.0.5 .
```

A imagem serve os arquivos pela porta interna `80`. O Nginx usa `try_files` com fallback para `index.html`, permitindo abrir diretamente `/`, `/login` e `/register`.

## Validação

```bash
npm run build -- --configuration production
npm test -- --watch=false --browsers=ChromeHeadless
```

O build local de produção mantém o marcador de URL até que uma imagem seja construída com `API_URL`; a imagem publicada sempre deve ser gerada com esse argumento.

## Funcionalidades

- Login e cadastro com Reactive Forms.
- Sessão JWT persistida no `localStorage`.
- Proteção de rotas públicas e privadas.
- Pesquisa paginada, cadastro, atualização e quitação de dívidas.
- Feedbacks de carregamento, sucesso, erro e lista vazia.
- Layout responsivo e modais acessíveis.

Os endpoints continuam centralizados em `src/app/core/api/api-endpoints.ts`, preservando os contratos da API Spring Boot.
