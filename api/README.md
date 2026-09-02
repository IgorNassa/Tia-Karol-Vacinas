# Tia Karol Vacinas API

Módulo independente do Swing legado. A API será migrada por domínio, começando por pacientes, lotes, estoque e aplicações.

## Requisitos

- Java 17+
- Docker apenas se optar pelo PostgreSQL local em vez do Supabase

## Executar localmente

```powershell
Copy-Item .env.example .env
# Preencha DATABASE_PASSWORD e, se desejar criar o primeiro administrador,
# BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD.
.\mvnw.cmd spring-boot:run
```

## Supabase

O ambiente de desenvolvimento usa o projeto `tia-karol-api-dev`, na região de São Paulo (`sa-east-1`). A aplicação carrega `.env` automaticamente quando iniciada a partir da pasta `api`. Informe somente os segredos localmente; esse arquivo nunca deve ser commitado.

- A API Java conecta diretamente ao PostgreSQL pelo **Session pooler**, porta `5432`, com `sslmode=require`.
- As tabelas da aplicação ficam no schema privado `app`; a Data API do Supabase permanece desativada.
- A chave `service_role`/secret key não deve ser colocada no frontend, no repositório ou em arquivos de exemplo.
- `legacy_migration_records` preserva a correspondência entre registros antigos e novos, permitindo reexecução idempotente da futura migração.

- Health check: `GET http://localhost:8080/api/v1/health`
- Liveness: `GET http://localhost:8080/actuator/health/liveness`
- Readiness: `GET http://localhost:8080/actuator/health/readiness`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Produção e homologação

- Imagem: `docker build -t tia-karol-api:1.0.0 .`; o container executa como usuário sem privilégios.
- Em `prod`, banco sem SSL, CORS curinga/localhost e senha provisória impedem a inicialização.
- Procedimentos de backup, restauração e rollback: [`docs/OPERATIONS.md`](docs/OPERATIONS.md).
- Coleta e recebimento do backup legado: [`docs/LEGACY_BACKUP_INTAKE.md`](docs/LEGACY_BACKUP_INTAKE.md).
- Segurança e retenção LGPD: [`docs/LGPD_AND_SECURITY.md`](docs/LGPD_AND_SECURITY.md).
- Homologação e coleção: [`docs/HOMOLOGATION.md`](docs/HOMOLOGATION.md) e [`http/tia-karol-api.http`](http/tia-karol-api.http).
- Publicação: [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).
- Swing sem credenciais fixas: [`docs/SWING_CONFIGURATION.md`](docs/SWING_CONFIGURATION.md).
- Contrato congelado: `openapi/tia-karol-api-v1.json`; compare-o com `scripts/verify-openapi-contract.ps1` enquanto a API estiver rodando.

## Regras já protegidas pelo schema

- CPF ativo único.
- Registro de vacina por lote único.
- Lotes vencidos permanecem no histórico, mas serão bloqueados para agendamento e aplicação pela camada de negócio.
- Estoque físico e reservado nunca ficam negativos; reserva não excede quantidade física.
- Ajuste de estoque exige motivo.
- Estados oficiais de agenda são validados.
- Cada agendamento reserva exatamente uma dose usando trava transacional no lote.
- Aplicação reduz, na mesma transação, o estoque físico e o reservado.
- Cancelamento exige motivo e devolve a reserva sem aumentar artificialmente o estoque físico.
- Faltas ficam em uma fila de pendências até o administrador devolver ou manter a dose reservada.

Reserva, baixa, devolução e auditoria do agendamento são executadas na mesma transação. A inativação do paciente já exibe evidências do histórico e exige confirmação dupla quando necessário.

## Agendamentos

- `POST /api/v1/appointments`: agenda e reserva uma dose.
- `PATCH /api/v1/appointments/{id}/confirmation`: confirma o atendimento.
- `PATCH /api/v1/appointments/{id}/application`: registra a aplicação e baixa a dose.
- `PATCH /api/v1/appointments/{id}/cancellation`: cancela com motivo e libera a reserva.
- `PATCH /api/v1/appointments/{id}/no-show`: registra falta mantendo a dose pendente.
- `GET /api/v1/appointments/pending`: lista decisões de estoque pendentes.
- `PATCH /api/v1/appointments/{id}/no-show-resolution`: decisão administrativa de devolver ou manter a reserva.
- `GET /api/v1/appointments?patientId=&status=&fromDate=&toDate=`: consulta a agenda com filtros e paginação.
- `PATCH /api/v1/appointments/{id}`: altera observações e valores; campos clínicos e paciente são administrativos.
- `PATCH /api/v1/appointments/{id}/reschedule`: reagenda e transfere a reserva entre lotes de forma atômica; somente administrador.

As operações concorrentes de agenda, paciente e estoque usam travas transacionais. Assim, duas requisições simultâneas não podem consumir ou devolver a mesma reserva duas vezes.

## Pacientes

- `GET /api/v1/patients/{id}/inactivation-preview`: exibe contagem e os cinco registros mais recentes do histórico.
- `PATCH /api/v1/patients/{id}/inactivation`: exige confirmação simples sem histórico e confirmação dupla quando houver agendamento ou aplicação.
- CPF é validado e permanece único; estrangeiros usam documento livre e recém-nascidos exigem ao menos um responsável documentado.

## Catálogo e estoque

- `POST /api/v1/vaccines`: cadastra a vacina independentemente dos lotes.
- `GET /api/v1/vaccines`: pesquisa o catálogo por nome com paginação estável.
- `PUT /api/v1/vaccines/{id}` e `PATCH /api/v1/vaccines/{id}/inactivation`: manutenção administrativa.
- `POST /api/v1/vaccine-lots`: cria um lote para `vaccineId`; se o mesmo lote já existir com todos os dados iguais, apenas aumenta o saldo.
- `POST /api/v1/vaccine-lots/{id}/stock-movements`: registra entrada, retorno, ajuste, perda ou vencimento sob trava transacional.

Vacina ou lote inativo e lote vencido não podem ser reservados nem aplicados. O schema preserva os campos antigos do lote durante a transição para facilitar a importação do Swing sem perda de dados.

## Pagamentos

- `GET /api/v1/appointments/{id}/payments`: consulta o carrinho ativo.
- `PUT /api/v1/appointments/{id}/payments`: registra ou substitui pagamentos múltiplos.
- `POST /api/v1/appointments/{id}/payments/void`: estorna o carrinho com motivo; somente administrador.
- `GET /api/v1/appointments/{id}/payments/history`: consulta todo o histórico, inclusive versões estornadas; somente administrador.

A soma de débito, crédito, dinheiro e pendência deve ser exatamente igual ao valor final do atendimento. Substituições e estornos preservam o histórico no banco, além do registro de auditoria. Um atendimento com pagamento ativo precisa ter o carrinho estornado antes de alteração de valor, cancelamento ou registro de falta.

## Financeiro e relatórios

- `POST/GET /api/v1/financial/entries`: registra e consulta entradas e saídas avulsas.
- `POST /api/v1/financial/entries/{id}/void`: estorna um lançamento sem apagar seu histórico.
- `/api/v1/financial/recurring-expenses`: mantém despesas fixas ou variáveis e registra seus pagamentos.
- `GET /api/v1/reports/financial/summary`: consolidado realizado e projetado por período.
- `GET /api/v1/reports/financial/cash-flow`: livro-caixa unificado de atendimentos e lançamentos avulsos.
- `GET /api/v1/reports/financial/projections`: recebíveis de atendimentos e despesas recorrentes futuras.
- `GET /api/v1/reports/stock`: quantidades, capital de aquisição e venda potencial por lote.
- `GET /api/v1/reports/applications`: aplicações por período.
- `GET /api/v1/reports/patients/{id}/applications`: histórico de aplicações por paciente.

O financeiro administrativo e todos os relatórios são exclusivos do administrador. O atendente acessa apenas o carrinho operacional dentro do atendimento. O schema mantém `legacy_method` para preservar formas de pagamento antigas durante a futura importação do Swing, mesmo que não existam no domínio novo.

## Acesso local

O primeiro administrador é criado apenas quando `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` forem informados.

- `POST /api/v1/auth/login`: gera access token de 15 minutos e refresh token de 7 dias.
- `POST /api/v1/auth/refresh`: rotaciona o refresh token e revoga a sessão anterior.
- `POST /api/v1/auth/logout`: revoga imediatamente a sessão atual.
- `/api/v1/users/**`: gestão administrativa de usuários e redefinição de senha.

Os tokens são opacos e aleatórios; somente hashes SHA-256 ficam armazenados no banco. Inativação ou redefinição de senha revoga todas as sessões do usuário. Nesta versão, somente `ADMIN` e `ATTENDANT` podem ser cadastrados.

## Migração do Swing

O primeiro estágio da migração é exclusivamente de leitura e não grava no Supabase. Ele abre uma transação
`REPEATABLE READ`, marca a conexão legada como somente leitura, valida as tabelas conhecidas e produz um relatório
sem nomes, documentos, telefones ou outros dados pessoais.

Variáveis necessárias:

- `LEGACY_DATABASE_URL`: JDBC do PostgreSQL legado.
- `LEGACY_DATABASE_USERNAME` e `LEGACY_DATABASE_PASSWORD`: credenciais temporárias, nunca versionadas.
- `LEGACY_MIGRATION_REPORT`: caminho local do JSON; opcional.
- `LEGACY_MIGRATION_MODE`: deve permanecer como `dry-run` nesta fase.

Execução: `mvn -DskipTests compile exec:java`. O processo retorna código `2` quando encontra erro impeditivo e
mantém avisos para revisão manual. A futura execução definitiva usará `legacy_migration_records` para relacionar
cada ID antigo ao UUID novo e impedir duplicidades; ela só será habilitada após a validação do backup real.
