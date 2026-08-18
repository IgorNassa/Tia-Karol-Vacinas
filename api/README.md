# Tia Karol Vacinas API

Módulo independente do Swing legado. A API será migrada por domínio, começando por pacientes, lotes, estoque e aplicações.

## Requisitos

- Java 17+
- Maven 3.9+
- Docker apenas se optar pelo PostgreSQL local em vez do Supabase

## Executar localmente

```powershell
Copy-Item .env.example .env
# Preencha DATABASE_PASSWORD e, se desejar criar o primeiro administrador,
# BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD.
mvn spring-boot:run
```

## Supabase

O ambiente de desenvolvimento usa o projeto `tia-karol-api-dev`, na região de São Paulo (`sa-east-1`). A aplicação carrega `.env` automaticamente quando iniciada a partir da pasta `api`. Informe somente os segredos localmente; esse arquivo nunca deve ser commitado.

- A API Java conecta diretamente ao PostgreSQL pelo **Session pooler**, porta `5432`, com `sslmode=require`.
- As tabelas da aplicação ficam no schema privado `app`; a Data API do Supabase permanece desativada.
- A chave `service_role`/secret key não deve ser colocada no frontend, no repositório ou em arquivos de exemplo.
- `legacy_migration_records` preserva a correspondência entre registros antigos e novos, permitindo reexecução idempotente da futura migração.

- Health check: `GET http://localhost:8080/api/v1/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Regras já protegidas pelo schema

- CPF ativo único.
- Registro de vacina por lote único.
- Lotes vencidos permanecem no histórico, mas serão bloqueados para agendamento e aplicação pela camada de negócio.
- Estoque físico e reservado nunca ficam negativos; reserva não excede quantidade física.
- Ajuste de estoque exige motivo.
- Estados oficiais de agenda são validados.

As regras que dependem de transação (reserva, baixa, devolução, inativação e auditoria automática) serão implementadas na camada de aplicação antes de expor os endpoints de negócio.

## Acesso local

Os endpoints de negócio usam HTTP Basic temporariamente durante a fundação da API. O primeiro administrador é criado apenas quando as variáveis `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` forem informadas. A migração para login com token será feita antes do frontend.
