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

Crie um arquivo `.env` na raiz. O provedor LLM é selecionado por `LLM_PROVIDER` (`groq` por padrão; `minimax` também suportado):

**Groq (default):**
```env
LLM_PROVIDER=groq
GROQ_API_KEY=sua_chave_aqui
GROQ_MODEL=qwen/qwen3-32b
GROQ_TEMPERATURE=0.7
GROQ_MAX_TOKENS=1024
```

**MiniMax:**
```env
LLM_PROVIDER=minimax
MINIMAX_API_KEY=sua_chave_aqui
MINIMAX_MODEL=MiniMax-M3
MINIMAX_TEMPERATURE=0.7
MINIMAX_MAX_TOKENS=1024
```

```bash
docker compose up --build
```

- A API sobe em `http://localhost:8080`.
- Para acessar no navegador use `http://localhost:8080/swagger-ui/index.html`
- O banco SQLite persiste `./data/agent-manager.db`. Para melhor visualização, instalar a extensão do VSCode SQLite Viewer.

## Status

API com CRUD de usuários, agentes, API keys e conversas; autenticação por login; integração com Groq via `POST /api/conversations/llm`; e controle de tokens por plano (`FREE` 10k, `PRO` 50k, `MASTER` 100k).