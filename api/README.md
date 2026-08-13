# Tia Karol Vacinas API

Módulo independente do Swing legado. A API será migrada por domínio, começando por pacientes, lotes, estoque e aplicações.

## Requisitos

- Java 17+
- Maven 3.9+
- Docker (para o PostgreSQL local)

## Executar localmente

```powershell
docker compose up -d
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@tiakarol.local"
$env:BOOTSTRAP_ADMIN_PASSWORD = "troque-esta-senha"
mvn spring-boot:run
```

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
