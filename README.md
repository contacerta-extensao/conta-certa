# ContaCerta
Atividade de extensão do curso de análise e desenvolvimento de sistemas do IFSC, desenvolvimento de uma aplicação educacional focada na matemática financeira.

## Executar com Docker Compose

O Compose constrói e inicia o frontend, o backend, o PostgreSQL e o Mailpit. Copie as variáveis de exemplo e suba os serviços:

```bash
cd backend
cp .env.example .env
docker compose up --build
```

Depois da inicialização, acesse:

- frontend: <http://localhost:4200>;
- API: <http://localhost:8080/api/v1>;
- Mailpit: <http://localhost:8025>.

As chaves RSA usadas nos tokens JWT são criadas automaticamente no volume `jwt_keys` na primeira inicialização. Para criar o administrador inicial, preencha as três variáveis `INITIAL_ADMIN_*` no arquivo `.env` antes de subir os serviços.
