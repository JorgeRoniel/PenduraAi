# Pendura Aí

Aplicação para organizar clientes e dívidas de pequenos comércios. A interface oficial é Angular; `Projeto-web/` contém o React legado.

## Desenvolvimento local

É possível continuar usando as imagens separadas com Docker Compose. Copie `.env.example` para `.env`, ajuste as senhas locais e execute `docker compose up -d`. A interface fica em `http://localhost:4200` e a API em `http://localhost:8080`. O Angular executado com `npm start` em `frontend-angular/` também usa essa API local.

## Publicação no Render e Neon

O `Dockerfile` da raiz compila Angular e Spring Boot com Java 21 e entrega interface e API no mesmo endereço. O React legado e arquivos locais não entram na imagem. Não é preciso configurar `API_URL`: no build de produção, o Angular usa `/api` no próprio site.

1. Crie um projeto gratuito e vazio no Neon com PostgreSQL 17, escolhendo uma região próxima da região desejada no Render. Copie a conexão **direta** e converta para JDBC: `jdbc:postgresql://HOST/DB?sslmode=require`. Guarde usuário e senha em campos separados. O Flyway criará as tabelas na primeira inicialização.
2. Disponibilize esta versão no repositório `JorgeRoniel/Trabalho03-Web` do GitHub. No Render, conecte esse repositório e crie um único **Web Service** Docker, com contexto na raiz, instância Free e health check `/health`.
3. Configure no painel do Render as variáveis abaixo. Insira as credenciais apenas nos painéis, sem incluí-las no repositório ou no chat. Defina `APP_CORS_ALLOWED_ORIGINS` com a URL HTTPS exata fornecida pelo Render, sem barra final.
4. Publique. Confira no log do Render a inicialização e as migrações Flyway. Abra `/health`, `/login` e `/register` no endereço publicado; teste também recarregar essas páginas.

| Variável | Valor no Render |
| --- | --- |
| `DB_URL` | URL JDBC da conexão direta do Neon com `sslmode=require` |
| `DB_USERNAME` | Usuário do banco Neon |
| `DB_PASSWORD` | Senha do banco Neon |
| `JWT_SECRET` | Segredo aleatório exclusivo e longo |
| `AUTH_COOKIE_SECURE` | `true` |
| `APP_CORS_ALLOWED_ORIGINS` | URL HTTPS pública do site |
| `PORT` | Definida automaticamente pelo Render; padrão local `8080` |

O endpoint `GET /health` retorna `{"status":"ok"}` e indica que o serviço iniciou. Ele não verifica a disponibilidade do banco.

Para atualizar o site, envie os novos commits ao repositório e acompanhe o deploy e os logs no painel do Web Service. No mesmo painel, consulte erros e consumo de memória; no painel Neon, acompanhe o uso do banco gratuito. Após um período sem acesso, a instância gratuita pode suspender e a próxima visita demorar mais. Evite dados reais neste portfólio.

## Validação local

```bash
cd apiPenduraAi && ./mvnw test
cd ../frontend-angular && npm test -- --watch=false --browsers=ChromeHeadless
cd .. && docker build -t pendura-ai:local .
```

Os testes de integração da API usam PostgreSQL com Testcontainers e precisam de um Docker Engine acessível. Após publicar, confira cadastro, login, renovação de sessão, logout e operações com dívidas. Em HTTPS, os cookies de autenticação devem apresentar `Secure` e `HttpOnly`; requisições de escrita continuam exigindo o token CSRF. Reinicie o serviço e confira que os dados continuam no Neon.
