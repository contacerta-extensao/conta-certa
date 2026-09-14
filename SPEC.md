# Conta Certa — Especificação atual

> Plataforma web de educação financeira gamificada para alunos, professores e administradores.
>
> Este documento substitui a especificação do protótipo React exportado do Figma. O contrato detalhado da aplicação atual está dividido entre o backend Spring Boot e o frontend Angular.

## 1. Fontes de verdade

- [Contrato do backend](docs/backend-spec.md): domínio, regras de negócio, persistência, autorização e endpoints.
- [Contrato de integração do frontend](docs/frontend-integration-spec.md): payloads, estados, permissões e comportamento esperado das telas.
- [Plano do frontend](docs/frontend/00-visao-geral.md): organização Angular, design system, jornadas e critérios de qualidade.
- [README do frontend](frontend/README.md): execução, mock da API, comandos e estado das partes implementadas.

Quando este documento divergir dos contratos acima, os contratos atuais têm precedência.

## 2. Arquitetura atual

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Frontend | Angular 22, TypeScript, PrimeNG 21 | Telas, navegação, formulários, acessibilidade e apresentação dos dados da API |
| Backend | Spring Boot, Java 21, Maven | Autenticação, autorização, regras de negócio, persistência e relatórios |
| Banco | PostgreSQL + Flyway | Dados de usuários, instituições, salas, conteúdos, tentativas, progresso e mídias |
| Arquivos | Armazenamento interno do backend | Upload e download autorizado de materiais e imagens |
| Testes | JUnit 5, Spring Boot Test, Testcontainers, Vitest | Testes unitários, integração, contrato e fluxos do frontend |

A API usa o prefixo `/api/v1`, JSON em `camelCase`, UUIDs como identificadores e instantes em UTC. O frontend apresenta datas no fuso `America/Sao_Paulo`.

O domínio não depende de `localStorage`. O frontend pode usar um mock plugável para desenvolvimento, mas a fonte de verdade em produção é a API com PostgreSQL.

## 3. Perfis e funcionalidades

### Público e conta

- Login com e-mail e senha, rotação de refresh token e logout por sessão.
- Cadastro público de aluno com instituição, confirmação de e-mail e política de senha.
- Recuperação e redefinição de senha.
- Convite administrativo para professores e definição inicial de senha.
- Consulta e edição do próprio perfil e troca de senha.

O cadastro público é exclusivo para alunos. Professores entram pelo fluxo de convite do administrador.

### Aluno

- Entrada em várias salas da própria instituição por código.
- Dashboard da sala com progresso, XP, nível, estrelas, conquistas, dica financeira e ranking.
- Trilha de lições com pré-requisitos, datas, prazo e limite de tentativas.
- Tentativas idempotentes, respostas por tipo de questão, expiração no servidor e resultado oficial.
- Histórico de tentativas e progresso separado por sala.
- Videoaulas, materiais, download autorizado e registro idempotente de visualizações.
- Ranking anonimizado para colegas e conquistas calculadas por sala.

### Professor

- Dashboard com métricas das próprias salas.
- CRUD, arquivamento, duplicação, código de ingresso e gestão de alunos das salas.
- CRUD de lições próprias, publicação, arquivamento, duplicação e imagens no Markdown.
- CRUD e ordenação de questões de escolha única, múltipla, verdadeiro/falso e numéricas.
- Atribuição de lições às salas, configuração de disponibilidade, prazo, tentativas e embaralhamento.
- Concessão de tentativas extras.
- CRUD de vídeos e materiais, upload de PDF/PPT/PPTX e vínculos com salas e lições.
- Consulta de visualizações por aluno.
- Relatórios agregados, ranking, histórico detalhado, filtros, paginação e exportação CSV.

### Administrador

- Dashboard administrativo.
- CRUD e ciclo de vida de instituições.
- Criação, convite, ativação, desativação e redefinição de senha de professores.
- CRUD e ciclo de vida de dicas financeiras.

## 4. Regras deliberadas do produto atual

- Não há lições padrão nem biblioteca compartilhada no backend. Professores trabalham com lições próprias, reutilizadas nas salas por atribuições.
- Não há catálogo de questões padrão desabilitáveis. Todas as questões persistidas pertencem a uma lição do professor.
- O professor não se cadastra publicamente; o administrador cria a conta pendente e envia o convite.
- O progresso é separado por sala, permitindo que o mesmo aluno tenha resultados diferentes em salas diferentes.
- Arquivos privados são acessados pelo endpoint autorizado de download; o cliente não monta URLs diretas para o armazenamento.
- O backend gera CSV para relatórios. A versão de impressão/PDF é montada pelo frontend.
- A autorização é aplicada no servidor por usuário, instituição, autoria, matrícula e estado da sala.

## 5. O que a especificação antiga não representa mais

A versão anterior deste arquivo descrevia:

- React 18, Vite, Tailwind e componentes shadcn/ui;
- navegação por estado sem URL;
- persistência integral em `localStorage`;
- cadastro de professor pela tela pública;
- seis lições fixas com quatro questões embutidas;
- uma única sala vinculada ao aluno.

Esses pontos pertencem ao protótipo original e foram substituídos pela arquitetura e pelos contratos atuais. A semente de demonstração do mock do frontend pode conter conteúdo de exemplo, mas esse conteúdo não é uma obrigação do banco de produção.

## 6. Próximas implementações

Os requisitos das jornadas pública, aluno, professor e administrador já estão cobertos pelos contratos atuais. Uma nova implementação deve começar por uma demanda de produto registrada em uma especificação ou issue.

Adicionar um catálogo inicial de lições e questões padrão é possível, mas constitui uma mudança de produto. Ela exigiria, no mínimo:

1. alterar o contrato do backend para incluir conteúdo compartilhado;
2. definir propriedade, versionamento, publicação e escopo institucional;
3. criar migrations e seed idempotente;
4. adaptar atribuições, listagens, questões e autorização;
5. atualizar o contrato e as telas do frontend;
6. cobrir o fluxo com testes de integração.

## 7. Verificação

Backend:

```bash
cd backend
./mvnw verify
```

Frontend:

```bash
cd frontend
npm run verify
```

O frontend possui mock plugável para desenvolvimento. Para usar o backend real, configure `useMockApi: false` no ambiente do frontend e forneça as variáveis de banco, JWT e SMTP descritas na configuração do backend.
