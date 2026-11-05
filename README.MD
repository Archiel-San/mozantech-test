# Desafio
This is a simple exercise, a simple order manager. You should develop an API where users can create and manage orders. Items can be ordered and orders are automatically fulfilled as soon as the item stock allows it.
Specification
The system should be able to provide the following features:
- create, read, update and delete and list all entities;
- when an order is created, it should try to satisfy it with the current stock.;
- when a stock movement is created, the system should try to attribute it to an order that isn't complete;
- when an order is complete, send a notification by email to the user that created it;
- trace the list of stock movements that were used to complete the order, and vice-versa;
- show current completion of each order;
- Write a log file with: orders completed, stock movements, email sent and errors.
Entities
- Item
> name
- StockMovement
> creationDate
> Item
> quantity
- Order
> creationDate
> Item
> quantity
> User (who created the order)
- User
> name
> email
Requirements:
- The API should be made with java 8 with Spring Boot + Spring JPA or Jave EE + Hibernate, PostgreSQL, GIT, log4j (or other);
- You should provide instructions on how to run the project and how to call the routes;

### Como_correr
1º Baixar o docker app

# Mozantech Test – Gestor de Encomendas

API REST para criar e gerir encomendas. Os itens são encomendados e as encomendas são satisfeitas automaticamente assim que o stock o permite.

**Stack:** Java 8, Spring Boot 2.7, Spring Data JPA (Hibernate), PostgreSQL, Maven, Docker, Logback (log em ficheiro), Mailpit (servidor SMTP de teste).

## Como executar

### Opção 1: tudo com Docker (recomendado)

Requisitos: Docker e Docker Compose.

```bash
docker compose up --build
```

Isto arranca:

| Serviço    | Descrição                                   | Endereço                |
|------------|---------------------------------------------|-------------------------|
| `app`      | A API (compilada com Maven, corre em Java 8)| http://localhost:8080   |
| `postgres` | Base de dados PostgreSQL 17                 | localhost:5432          |
| `mailpit`  | Servidor SMTP de teste + interface web      | http://localhost:8025   |

Para parar: `docker compose down` (use `docker compose down -v` para apagar também os dados da base de dados).

### Opção 2: aplicação local + base de dados em Docker

Requisitos: JDK 8 ou superior (o projeto compila com `release 8`, por isso o JDK 17 também serve), Maven e Docker.

```bash
docker compose up -d postgres mailpit
mvn spring-boot:run
```

A aplicação fica em http://localhost:8080. A configuração está em `src/main/resources/application.properties`
(base de dados `mozantech`, utilizador `postgres`, palavra-passe `password`).

## Documentação interativa

Swagger UI com todos os endpoints e exemplos de pedidos/respostas: http://localhost:8080/swagger-ui.html

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

- **User**: `name`, `email` (único)
- **Item**: `name` (único)
- **StockMovement**: `creationDate`, `item`, `quantity` (+ ligação opcional à encomenda que o consome e, quando
  resulta da divisão de outro movimento, `origin`, o movimento de onde foi separado)
- **Order**: `creationDate`, `item`, `quantity`, `user` (quem criou a encomenda)

## Endpoints

Todos os endpoints aceitam e devolvem JSON.

| Recurso          | Método e rota                         | Descrição                                          |
|------------------|---------------------------------------|----------------------------------------------------|
| Utilizadores     | `POST /users`                         | Criar                                              |
|                  | `GET /users`, `GET /users/{id}`       | Listar / ler                                       |
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
// POST/PUT /users
{ "name": "Ana", "email": "ana@test.com" }

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
| 404    | Recurso não encontrado                                                 |
| 409    | Conflito (email/item duplicado, apagar algo que está em uso, alterar uma encomenda completa ou um movimento já usado) |
| 500    | Erro inesperado (fica registado no log)                                |

## Exemplo completo (Se n tiver postman)

```
# 1. Criar utilizador e item
#curl -X POST localhost:8080/users -H 'Content-Type: application/json' \
     -d '{"name":"Ana","email":"ana@test.com"}'
#curl -X POST localhost:8080/items -H 'Content-Type: application/json' \
     -d '{"name":"Teclado"}'

# 2. Criar uma encomenda de 5 unidades (sem stock: 0% completa)
#curl -X POST localhost:8080/orders -H 'Content-Type: application/json' \
     -d '{"itemId":1,"userId":1,"quantity":5}'

# 3. Entram 3 unidades em stock: a encomenda passa a 60%
#curl -X POST localhost:8080/stock-movements -H 'Content-Type: application/json' \
     -d '{"itemId":1,"quantity":3}'

# 4. Entram 10 unidades: a encomenda fica completa (100%), é enviado o email
#    e ficam 8 unidades livres
#curl -X POST localhost:8080/stock-movements -H 'Content-Type: application/json' \
     -d '{"itemId":1,"quantity":10}'

# 5. Rastreabilidade nos dois sentidos
#curl localhost:8080/orders/1/stock-movements   # movimentos usados pela encomenda
#curl localhost:8080/stock-movements/1          # campo "orderId" = encomenda que o usou
#curl localhost:8080/orders/1                   # conclusão atual
```

Depois do passo 4, o email aparece em http://localhost:8025 e o ficheiro `logs/orders.log` regista os movimentos,
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
- **Concorrência:** as operações que mexem em stock bloqueiam a linha do item (`PESSIMISTIC_WRITE`) dentro de uma
  transação, para dois pedidos em simultâneo não gastarem o mesmo stock.
- **Emails:** são enviados só depois de a transação ser confirmada (`AFTER_COMMIT`). Uma falha no envio fica registada
  no log como erro e não faz falhar o pedido.
- **Apagar uma encomenda** liberta os movimentos que ela usava, que são oferecidos às outras encomendas pendentes.
- **Java 8:** o projeto compila com `--release 8` (o compilador rejeita APIs posteriores ao Java 8) e a imagem Docker
  corre sobre um JRE 8.
- A tabela de encomendas chama-se `orders` porque `order` é palavra reservada em SQL, e a de utilizadores `usuario`.
- **Rastreabilidade de movimentos divididos:** a parte separada de um movimento guarda `origin` (devolvido como
  `originId`), pelo que é possível saber de que movimento original veio cada parcela.

## Testes

#mvn test

Requer Docker a correr: os testes sobem uma base de dados PostgreSQL descartável com Testcontainers e chamam a API
real por HTTP. Cobrem: sem stock, stock parcial, divisão de movimentos, prioridade da encomenda mais antiga,
rastreabilidade nos dois sentidos, apagar encomenda (liberta stock), validações, envio (e falha) de email e
**concorrência** (20 encomendas em simultâneo contra 10 unidades de stock; 10 movimentos em simultâneo).

Nota:
Se tiver um serviço de postgres a correr localmente desligue. Pois a instância do docker corre também na porta 5432 e poderá gerar conflinto e as tabelas não serão criadas.
