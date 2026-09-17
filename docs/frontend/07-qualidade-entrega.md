# Parte 7 — Qualidade e entrega

Depende de todas as partes anteriores. O frontend consome exclusivamente a API
real; testes isolados de HTTP usam `HttpTestingController` para validar os
contratos sem incluir um backend em memória no artefato da aplicação.

## 1. Estratégia de testes

### 1.1 Critérios obrigatórios

| # | Critério da spec | Proteção |
|---|---|---|
| 1 | Nenhum gabarito antes do fim | modelos e componentes da tentativa |
| 2 | Cronômetro baseado no servidor | `ServerClock` e testes da tentativa |
| 3 | Troca de sala não mistura dados | stores reiniciados pelo `roomId` |
| 4 | Refresh concorrente único | interceptors de autenticação |
| 5 | `fieldErrors` junto aos campos | formulários e tratamento de erro |
| 6 | `version` e conflito sem sobrescrita | serviços e telas editáveis |
| 7 | Arquivos privados por endpoint autorizado | componente de download seguro |
| 8 | Markdown sanitizado com KaTeX | pipeline de Markdown |
| 9 | Estados distinguíveis | componentes e páginas |
| 10 | Frontend não recalcula resultado | varreduras da seção 2 |

### 1.2 Testes unitários e de componente

Serviços de dados, stores, pipes, `ServerClock`, `applyFieldErrors` e o pipeline
de sanitização são testados isoladamente. `HttpTestingController` verifica URL,
query, headers e corpo, incluindo `Idempotency-Key` e `version`.

Componentes cobrem carregamento, vazio, erro, `403`, `404`, `409`, os quatro
tipos de questão, cronômetro e editor de questões.

### 1.3 Verificação ponta a ponta

As jornadas pública, de aluno, professor e administrador devem ser verificadas
com o backend e o PostgreSQL reais. A automação E2E pode ser adicionada sem
alterar os serviços ou introduzir um backend em memória na aplicação.

## 2. Varreduras automáticas

As verificações executadas por `npm run check:rules` impedem:

- cálculo de nota, XP, estrelas ou nível no cliente;
- uso de `Date.now()` no cronômetro de tentativas;
- URL privada da API em `href` ou `src` de template;
- `[innerHTML]` fora do pipeline sanitizado;
- conversão de valores monetários para ponto flutuante no envio;
- uso das diretivas estruturais legadas.

O ESLint restringe `HttpClient` ao núcleo de API e persistência de sessão ao
módulo de autenticação. Uma exceção precisa ser marcada com
`cc-allow: <id-da-regra>` junto da linha e justificada na revisão.

## 3. Scripts

```json
{
  "start": "ng serve",
  "build": "ng build",
  "test": "ng test",
  "lint": "ng lint",
  "check:rules": "node scripts/check-rules.mjs",
  "verify": "npm run lint && npm run check:rules && npm run test:ci && npm run build"
}
```

`npm run verify` é o portão de cada alteração.

## 4. Desempenho e acessibilidade

- Áreas funcionais são carregadas sob demanda.
- `marked`, DOMPurify, KaTeX e Chart.js só entram quando necessários.
- O build limita o pacote inicial a 800 kB para aviso e 1 MB para erro.
- Navegação por teclado, foco visível, `aria-live` e redução de movimento são
  requisitos de todas as telas.
- Documento e `LOCALE_ID` usam `pt-BR`.

## 5. Entrega

O build de produção fica em `frontend/dist/`. O host deve encaminhar rotas
profundas para `index.html` e rotear `/api/v1` para o backend.

## 6. Definição de pronto

- `npm run verify` passa;
- os dez critérios da spec têm teste ou varredura;
- as três áreas consomem a API real;
- o README permite executar o projeto com backend e banco locais.
