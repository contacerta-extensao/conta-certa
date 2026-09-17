# Conta Certa — Frontend

Angular 22 + PrimeNG 21. Implementa a [spec de integração](../docs/frontend-integration-spec.md)
segundo o plano em [`docs/frontend/`](../docs/frontend/).

## Pré-requisitos

- Node.js ^22.22.3, ^24.15.0 ou ≥ 26 (testado em 26.1)
- npm ≥ 10
- backend e PostgreSQL em execução

## Executar

Suba o backend na porta 8080 e execute:

```bash
npm install
npm start          # http://localhost:4200
```

O frontend sempre usa a API real. Durante o desenvolvimento, o `proxy.conf.json`
encaminha `/api/v1` para `http://localhost:8080`.

O backend não cria dados de demonstração de alunos, salas ou lições. Use dados
existentes no PostgreSQL ou cadastre-os pelos fluxos de administração e conta.

## Comandos

| Comando | O que faz |
|---|---|
| `npm start` | Servidor de desenvolvimento contra a API real |
| `npm run build` | Build de produção em `dist/` |
| `npm test` | Testes em modo watch |
| `npm run test:ci` | Testes uma vez |
| `npm run lint` | ESLint para TypeScript e templates |
| `npm run check:rules` | Varreduras arquiteturais |
| `npm run verify` | Lint, regras, testes e build |
| `npm run format` | Prettier |

## Por que o `.npmrc` existe

`node-options=--no-experimental-webstorage` evita conflito entre o
`localStorage` nativo do Node 26 e o ambiente jsdom do Vitest. Rode os testes
pelos scripts do npm para que essa opção seja aplicada.

`legacy-peer-deps=true` mantém o PrimeNG 21 sobre o Angular 22. O PrimeNG 22
exige uma licença; sem ela, o pacote injeta um aviso visual também em produção.

## Organização

```text
src/app/
├── core/       # HTTP, sessão, erros e modelos do contrato
├── shared/     # componentes, pipes e shells reutilizáveis
└── features/   # public, account, student, teacher, admin e errors
```

Regras de dependência verificadas pelo ESLint:

- `core` não importa de `shared` nem de `features`;
- `shared` não importa de `features`;
- `features/x` não importa de `features/y`.

## Varreduras arquiteturais

`npm run check:rules` impede código que recalcula nota, XP ou estrelas no
frontend, usa o relógio local em tentativas, injeta HTML diretamente ou monta
URL privada da API em templates.

Exceções justificadas usam `cc-allow: <id-da-regra>` na linha correspondente.
O pipeline de Markdown e vídeo documenta suas permissões no próprio código.

## Estado da implementação

| Parte | Documento | Situação |
|---|---|---|
| 1 — Núcleo | [`01-nucleo.md`](../docs/frontend/01-nucleo.md) | completa, com testes |
| 2 — Design system | [`02-design-system.md`](../docs/frontend/02-design-system.md) | completa, com testes |
| 3 — Jornada pública | [`03-publico.md`](../docs/frontend/03-publico.md) | completa |
| 4 — Aluno | [`04-aluno.md`](../docs/frontend/04-aluno.md) | completa |
| 5 — Professor | [`05-professor.md`](../docs/frontend/05-professor.md) | completa |
| 6 — Administrador | [`06-admin.md`](../docs/frontend/06-admin.md) | completa |
| 7 — Qualidade e entrega | [`07-qualidade-entrega.md`](../docs/frontend/07-qualidade-entrega.md) | lint, regras, testes e build |

As jornadas pública, de aluno, professor e administrador consomem
exclusivamente os contratos HTTP do backend.
