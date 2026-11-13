# Mozantech Test – Gestor de Encomendas

API REST para criar e gerir encomendas. Os itens são encomendados e as encomendas são satisfeitas automaticamente assim que o stock o permite. A API é protegida com **Keycloak** (OAuth2/OpenID Connect): todos os pedidos, exceto os de autenticação, exigem um token JWT.

**Stack:** Java 8, Spring Boot 2.7, Spring Data JPA (Hibernate), Spring Security (OAuth2 Resource Server), Keycloak, PostgreSQL, Maven, Docker, Logback (log em ficheiro), Mailpit (servidor SMTP de teste).

## Como executar

### Opção 1: tudo com Docker (recomendado)

Requisitos: Docker e Docker Compose. Não é preciso ter Java nem Maven instalados.

```bash
docker compose up --build
```

Isto arranca:

| Serviço    | Descrição                                          | Endereço                |
|------------|----------------------------------------------------|-------------------------|
| `app`      | A API (compilada com Maven, corre em Java 8)       | http://localhost:8080   |
| `postgres` | Base de dados PostgreSQL 17                        | localhost:5432          |
| `keycloak` | Servidor de identidade (realm `mozantech`)         | http://localhost:8081   |
| `mailpit`  | Servidor SMTP de teste + interface web             | http://localhost:8025   |

O Keycloak importa o realm `mozantech` automaticamente no arranque, a partir da pasta `keycloak/` (montada como volume).
O primeiro arranque demora alguns minutos (descarga de dependências e imagens).

Para parar: `docker compose down` (use `docker compose down -v` para apagar também os dados da base de dados e do Keycloak).

### Opção 2: aplicação local + infraestrutura em Docker

Requisitos: JDK 8 ou superior (o projeto compila com `release 8`, por isso o JDK 17 também serve), Maven e Docker.

```bash
docker compose up -d postgres keycloak mailpit
mvn spring-boot:run
```

A configuração está em `src/main/resources/application.properties`.

## Autenticação

A API não tem sessões: cada pedido leva um **token JWT** emitido pelo Keycloak no cabeçalho
`Authorization: Bearer <token>`. A API valida a assinatura, a validade e o emissor do token com as chaves públicas do Keycloak.

| Endpoint                      | Descrição                                                       | Token |
|-------------------------------|-----------------------------------------------------------------|-------|
| `POST /api/auth/register`     | Cria o utilizador no Keycloak e na base de dados da aplicação   | não   |
| `POST /api/auth/login`        | Devolve o token de acesso e o token de renovação                | não   |
| `POST /api/auth/refresh`      | Troca o token de renovação por um novo token de acesso          | não   |
| Todos os restantes endpoints  | Itens, utilizadores, encomendas e movimentos de stock           | **sim** |

Sem token (ou com um token inválido ou expirado) a API responde `401 Unauthorized`.
O token de acesso expira ao fim de poucos minutos (por defeito 5, configurável no Keycloak); quando isso acontecer,
use `/api/auth/refresh` ou volte a fazer login.

A criação direta de utilizadores (`POST /users`) deixou de existir: os utilizadores são criados só através de
`/api/auth/register`, para ficarem sempre ligados ao Keycloak (campo `keycloakId`).

### Consola do Keycloak (opcional)

http://localhost:8081 → *Administration Console* → utilizador `admin`, palavra-passe `admin` → realm `mozantech`.
Estas credenciais são só para uso local.

## Documentação interativa

Swagger UI com todos os endpoints e exemplos de pedidos/respostas: http://localhost:8080/swagger-ui.html
Para chamar endpoints protegidos a partir do Swagger, faça login (ver abaixo), clique em **Authorize** e cole o `accessToken`.

## Emails

Quando uma encomenda fica completa, é enviado um email ao utilizador que a criou. Em desenvolvimento os emails são
capturados pelo Mailpit e podem ser vistos em **http://localhost:8025** (não é enviado nenhum email real).

## Logs

A aplicação escreve em `logs/orders.log` (na pasta do projeto; em Docker a pasta `logs/` é montada no anfitrião):

- `ORDER CREATED` / `ORDER UPDATED` / `ORDER DELETED` / `ORDER COMPLETED` – encomendas
- `STOCK MOVEMENT CREATED` / `UPDATED` / `DELETED` e `STOCK MOVEMENT x -> ORDER y` – movimentos de stock
- `EMAIL SENT` – emails enviados
- `ERROR ...` – erros (falhas no envio de email, violações de integridade, erros inesperados)

## Entidades

- **User**: `name`, `email` (único), `keycloakId` (único, identificador do utilizador no Keycloak), `createdAt`
- **Item**: `name` (único)
- **StockMovement**: `creationDate`, `item`, `quantity` (+ ligação opcional à encomenda que o consome e, quando
  resulta da divisão de outro movimento, `origin`, o movimento de onde foi separado)
- **Order**: `creationDate`, `item`, `quantity`, `user` (quem criou a encomenda)

## Endpoints

Todos aceitam e devolvem JSON. Exceto `/api/auth/**`, todos exigem `Authorization: Bearer <token>`.

| Recurso          | Método e rota                         | Descrição                                          |
|------------------|---------------------------------------|----------------------------------------------------|
| Autenticação     | `POST /api/auth/register`             | Registar utilizador                                |
|                  | `POST /api/auth/login`                | Obter tokens                                       |
|                  | `POST /api/auth/refresh`              | Renovar o token de acesso                          |
| Utilizadores     | `GET /users`, `GET /users/{id}`       | Listar / ler                                       |
|                  | `PUT /users/{id}`                     | Atualizar                                          |
|                  | `DELETE /users/{id}`                  | Apagar                                             |
| Itens            | `POST /items`                         | Criar                                              |
|                  | `GET /items`, `GET /items/{id}`       | Listar / ler                                       |
|                  | `PUT /items/{id}`                     | Atualizar                                          |
|                  | `DELETE /items/{id}`                  | Apagar                                             |
| Encomendas       | `POST /orders`                        | Criar (tenta satisfazer com o stock atual)         |
|                  | `GET /orders`, `GET /orders/{id}`     | Listar / ler (inclui a percentagem de conclusão)   |
|                  | `GET /orders/{id}/stock-movements`    | Movimentos de stock usados nesta encomenda         |
|                  | `PUT /orders/{id}`                    | Alterar a quantidade (só se ainda não estiver completa) |
|                  | `DELETE /orders/{id}`                 | Apagar (liberta o stock para outras encomendas)    |
| Movimentos stock | `POST /stock-movements`               | Criar (tenta atribuir a encomendas incompletas)    |
|                  | `GET /stock-movements`, `GET /stock-movements/{id}` | Listar / ler (inclui `orderId`)      |
|                  | `PUT /stock-movements/{id}`           | Alterar a quantidade (só se ainda não foi usado)   |
|                  | `DELETE /stock-movements/{id}`        | Apagar (só se ainda não foi usado)                 |

### Corpo dos pedidos

```json
// POST /api/auth/register
{ "name": "Ana", "email": "ana@test.com", "password": "Test1234!" }

// POST /api/auth/login
{ "email": "ana@test.com", "password": "Test1234!" }

// POST /api/auth/refresh
{ "refreshToken": "<refreshToken devolvido no login>" }

// PUT /users/{id}
{ "name": "Ana Silva", "email": "ana@test.com" }

// POST/PUT /items
{ "name": "Teclado" }

// POST /orders
{ "itemId": 1, "userId": 1, "quantity": 5 }

// PUT /orders/{id}  e  PUT /stock-movements/{id}
{ "quantity": 8 }

// POST /stock-movements
{ "itemId": 1, "quantity": 3 }
```

### Resposta de uma encomenda

```json
{
  "id": 1,
  "creationDate": "2026-11-03T13:10:00",
  "itemId": 1,
  "itemName": "Teclado",
  "userId": 1,
  "quantity": 5,
  "fulfilledQuantity": 3,
  "completionPercent": 60.0,
  "completed": false
}
```

### Códigos de erro

| Código | Quando                                                                 |
|--------|------------------------------------------------------------------------|
| 400    | Dados inválidos (campo em falta, email inválido, quantidade < 1)       |
| 401    | Token em falta, inválido ou expirado                                   |
| 404    | Recurso não encontrado                                                 |
| 409    | Conflito (email/item duplicado, apagar algo que está em uso, alterar uma encomenda completa ou um movimento já usado) |
| 500    | Erro inesperado (fica registado no log)                                |

## Exemplo completo com `curl`

Os comandos usam a variável `TOKEN`; o passo 2 preenche-a com o token devolvido no login.

```bash
# 1. Registar um utilizador (responde 201 sem corpo)
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
     -d '{"name":"Ana","email":"ana@test.com","password":"Test1234!"}'

# 2. Fazer login e guardar o token de acesso na variável TOKEN
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"ana@test.com","password":"Test1234!"}'
# A resposta traz "accessToken" e "refreshToken". Copie o valor do accessToken:
TOKEN='cole-aqui-o-accessToken'

# 3. Sem token a API recusa o pedido (401)
curl -i localhost:8080/items

# 4. Com token funciona: criar um item e ver o id do utilizador registado
curl -X POST localhost:8080/items -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"name":"Teclado"}'
curl localhost:8080/users -H "Authorization: Bearer $TOKEN"

# 5. Criar uma encomenda de 5 unidades (sem stock: 0% completa)
curl -X POST localhost:8080/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"itemId":1,"userId":1,"quantity":5}'

# 6. Entram 3 unidades em stock: a encomenda passa a 60%
curl -X POST localhost:8080/stock-movements -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"itemId":1,"quantity":3}'

# 7. Entram 10 unidades: a encomenda fica completa (100%), é enviado o email
#    e ficam 8 unidades livres
curl -X POST localhost:8080/stock-movements -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"itemId":1,"quantity":10}'

# 8. Rastreabilidade nos dois sentidos
curl localhost:8080/orders/1/stock-movements -H "Authorization: Bearer $TOKEN"   # movimentos usados pela encomenda
curl localhost:8080/stock-movements/1 -H "Authorization: Bearer $TOKEN"          # "orderId" = encomenda que o usou
curl localhost:8080/orders/1 -H "Authorization: Bearer $TOKEN"                   # conclusão atual
```

Se o token expirar (resposta `401`), renove-o sem voltar a escrever a palavra-passe:

```bash
curl -s -X POST localhost:8080/api/auth/refresh -H 'Content-Type: application/json' \
     -d '{"refreshToken":"cole-aqui-o-refreshToken"}'
```

Depois do passo 7, o email aparece em http://localhost:8025 e o ficheiro `logs/orders.log` regista os movimentos,
a conclusão da encomenda e o envio do email.

## Regras de negócio e decisões de desenho

- **Criar encomenda:** consome o stock livre do item, do movimento mais antigo para o mais recente. Se não houver
  stock suficiente, a encomenda fica parcialmente completa.
- **Criar movimento de stock:** é atribuído às encomendas incompletas desse item, da mais antiga para a mais recente.
- **Stock disponível** = movimentos do item que ainda não estão ligados a nenhuma encomenda.
- **Conclusão** de uma encomenda = soma das quantidades dos movimentos ligados / quantidade encomendada.
- **Um movimento pertence a, no máximo, uma encomenda.** Para respeitar isto sem criar entidades extra, um movimento
  maior do que aquilo de que a encomenda precisa é **dividido**: a parte usada passa a ser um movimento ligado à
  encomenda e o resto fica livre. Por exemplo, ao registar um movimento de 10 para uma encomenda que só precisa de 5,
  ficam dois movimentos de 5 (um ligado à encomenda, outro livre).
- **Rastreabilidade de movimentos divididos:** a parte separada de um movimento guarda `origin` (devolvido como
  `originId`), pelo que é possível saber de que movimento original veio cada parcela.
- **Concorrência:** as operações que mexem em stock bloqueiam a linha do item (`PESSIMISTIC_WRITE`) dentro de uma
  transação, para dois pedidos em simultâneo não gastarem o mesmo stock.
- **Emails:** são enviados só depois de a transação ser confirmada (`AFTER_COMMIT`). Uma falha no envio fica registada
  no log como erro e não faz falhar o pedido.
- **Apagar uma encomenda** liberta os movimentos que ela usava, que são oferecidos às outras encomendas pendentes.
- **Utilizadores e Keycloak:** a identidade (palavra-passe, tokens) vive no Keycloak; a aplicação guarda só os dados
  de negócio e o `keycloakId` que liga os dois.
- **Java 8:** o projeto compila com `--release 8` (o compilador rejeita APIs posteriores ao Java 8) e a imagem Docker
  corre sobre um JRE 8.
- A tabela de encomendas chama-se `orders` porque `order` é palavra reservada em SQL, e a de utilizadores `usuario`.

## Testes

```bash
mvn test
```

Requer Docker a correr: os testes sobem uma base de dados PostgreSQL descartável com Testcontainers e chamam a API
real por HTTP. Cobrem: sem stock, stock parcial, divisão de movimentos, prioridade da encomenda mais antiga,
rastreabilidade nos dois sentidos, apagar encomenda (liberta stock), validações, envio (e falha) de email e
**concorrência** (20 encomendas em simultâneo contra 10 unidades de stock; 10 movimentos em simultâneo).

## Limitações conhecidas e evolução para produção

- **Notificações:** o email é enviado após o commit e uma falha só fica registada em log. Em produção usaria um
  *transactional outbox* (tabela de notificações escrita na mesma transação + job com retries e *backoff*) ou uma
  fila persistente.
- **Histórico de stock:** movimentos ainda não utilizados podem ser alterados e apagar uma encomenda liberta o seu
  stock. Em produção os movimentos de inventário seriam imutáveis e as atribuições a encomendas viveriam numa
  tabela própria (movimento ↔ encomenda ↔ quantidade), o que evitaria dividir movimentos. Não o fiz porque o
  enunciado define apenas quatro entidades; a divisão é rastreada através de `origin_id`.
- **Bloqueio pessimista:** serializa as operações por artigo. É simples e correto; com muito tráfego no mesmo
  artigo seria necessário avaliar bloqueio otimista com *retries* ou uma fila por artigo. Sem `lock_timeout`, um
  pedido bloqueado espera indefinidamente.
- **Keycloak:** corre em modo de desenvolvimento (`start-dev`), por HTTP, com credenciais de administrador por
  defeito e a base de dados partilhada com a aplicação. Em produção usaria HTTPS, segredos fora do repositório e
  uma base de dados própria para o Keycloak.
- **Schema:** `ddl-auto=update` serve para o exercício; em produção usaria migrações (Flyway/Liquibase).
- **Observabilidade:** métricas (encomendas completas, stock livre) e *correlation id* nos logs.