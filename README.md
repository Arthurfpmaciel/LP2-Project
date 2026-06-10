# Agent Manager API

API RESTful em Java + Spring Boot para o gerenciamento de agentes de IA.

## Estrutura

```text
root/
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

## Próximos Passos

* user login para que usuários façam requisições 
* implementar agentes