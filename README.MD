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

2º
- Clonar o ficheiro (se usar github) e correr o comando docker compose up --build (facil se utilizar o intelliJ porque o terminal ja se encontra na localização do projecto)
- Caso tenha baixado em ficheiro ZIP, navegar até onde se localiza o ficheiro \\~mozantech-test e depois executar o comando docker compose up --build


### **duvidas**
No exercicio não possuia necessidade de 
bloquear ORDER pelo nr de itens em stock, logo, não criei nenhuma contraint


### Se_nao_tiver_postman
pode usar estes comandos para correr testes e consultas, ou navegar para http://localhost:8080/swagger-ui/index.html#/ para ter um overview dos controllers da API  

curl -X POST localhost:8080/users -H 'Content-Type: application/json' -d '{"name":"Ana","email":"ana@test.com"}'
curl -X POST localhost:8080/items -H 'Content-Type: application/json' -d '{"name":"Keyboard"}'
curl -X POST localhost:8080/orders -H 'Content-Type: application/json' -d '{"itemId":1,"userId":1,"quantity":5}'     
curl -X POST localhost:8080/stock-movements -H 'Content-Type: application/json' -d '{"itemId":1,"quantity":3}'       
curl -X POST localhost:8080/stock-movements -H 'Content-Type: application/json' -d '{"itemId":1,"quantity":10}'      
curl localhost:8080/orders/1/stock-movements    
curl localhost:8080/stock-movements/2       