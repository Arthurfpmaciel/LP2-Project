# Agent Manager API

API RESTful em Java + Spring Boot para uma estrutura inicial gerenciamento de agentes de IA.

## Estrutura

```text
projeto2/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/agentmanager/
│       │       ├── AgentManagerApplication.java
│       │       ├── controller/
│       │       ├── dto/
│       │       ├── exception/
│       │       ├── model/
│       │       ├── repository/
│       │       └── service/
│       └── resources/
│           └── application.properties
├── data/
├── target/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── .gitignore
└── .dockerignore
```

- `src/main/java/com/agentmanager`: código-fonte principal da aplicação.
- `AgentManagerApplication.java`: classe de inicialização do Spring Boot.
- `controller`: endpoints REST responsáveis por receber as requisições HTTP.
- `service`: regras de negócio e organização das operações da aplicação.
- `repository`: interfaces de acesso ao banco de dados usando Spring Data JPA.
- `model`: entidades persistidas no banco, como usuários, agentes, chaves de API e conversas.
- `dto`: objetos usados para entrada e saída de dados da API.
- `exception`: tratamento centralizado e padronização de erros.
- `src/main/resources/application.properties`: configurações da aplicação, banco SQLite, JPA e porta do servidor.
- `data`: pasta local onde o banco SQLite é criado durante a execução.
- `target`: pasta gerada pelo Maven ao compilar o projeto.
- `pom.xml`: configuração do Maven, dependências e versão do Java.
- `Dockerfile` e `docker-compose.yml`: arquivos para execução da aplicação com Docker.

## Execução com Docker

```bash
docker compose up --build
```

- A API sobe em `http://localhost:8080`.
- Para acessar no navegador use `http://localhost:8080/swagger-ui/index.html`
- O banco SQLite persiste `./data/agent-manager.db`. Para melhor visualização, instalar a extensão do VSCode SQLite Viewer.

## Status

Atualmente somente foi estabelecida a arquitetura e regras de negócio básicas, principalmentes as que dizem respeito ao banco de dados.

## Recursos

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `GET /api/api-keys?userId=1`
- `GET /api/api-keys/{id}`
- `POST /api/api-keys`
- `DELETE /api/api-keys/{id}`
- `GET /api/agents?level=FREE`
- `GET /api/agents/{id}`
- `POST /api/agents`
- `PUT /api/agents/{id}`
- `DELETE /api/agents/{id}`
- `GET /api/conversations?apiKeyId=1`
- `GET /api/conversations?agentId=1`
- `GET /api/conversations/{id}`
- `POST /api/conversations`
- `DELETE /api/conversations/{id}`

## Exemplos

Criar usuario:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Usuario","email":"user@example.com","password":"123456","planType":"PLUS"}'
```

Criar chave de API:

```bash
curl -X POST http://localhost:8080/api/api-keys \
  -H "Content-Type: application/json" \
  -d '{"userId":1}'
```

Criar agente:

```bash
curl -X POST http://localhost:8080/api/agents \
  -H "Content-Type: application/json" \
  -d '{"name":"Agente teste","description":"Agente inicial para testes","level":"FREE"}'
```

Registrar conversa:

```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -d '{"apiKeyId":1,"agentId":1,"input":"Ola","output":"Ola!","inputTokens":3,"outputTokens":4,"latencyMs":120}'
```
