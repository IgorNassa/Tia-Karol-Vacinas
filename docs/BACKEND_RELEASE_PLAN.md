# Plano de entrega do backend — 18 a 22 de agosto de 2026

## Objetivo

Entregar até sábado uma release candidate do backend: regras acordadas implementadas, migrations aplicadas no Supabase de desenvolvimento, testes verdes, OpenAPI utilizável e importação legada pronta para ensaio. A promoção para produção depende da validação funcional e da conferência da migração real.

## Calendário

| Data | Entrega principal | Critério de aceite |
|---|---|---|
| 18/08 | Build reproduzível, CI e pagamentos | Maven Wrapper; GitHub Actions; carrinho dividido; estorno; auditoria; build verde |
| 19/08 | Segurança, usuários, pacientes e estoque | login por token; papéis; evidência para inativação; catálogo e inventário consolidados |
| 20/08 | Agenda e financeiro | reagendamento; edição; estorno de aplicação; entradas, saídas, despesas e projeções |
| 21/08 | Relatórios, LGPD e migração | relatórios operacionais; auditoria consultável; importador idempotente com dry-run |
| 22/08 | Estabilização e release candidate | migrations validadas; suíte completa; advisors; OpenAPI; reconciliação e checklist final |

## Portões de qualidade

Uma entrega só avança quando:

1. regra de negócio e autorização estão cobertas por testes;
2. `./mvnw clean verify` está verde;
3. qualquer alteração estrutural possui migration Flyway validada no Supabase;
4. endpoints e exemplos estão documentados;
5. segredos e dados pessoais não aparecem em código ou logs;
6. alterações sensíveis geram auditoria.

## Validações do supervisor

- confirmar decisões funcionais que tenham mais de uma interpretação válida;
- validar os fluxos no Swagger conforme forem publicados;
- fornecer acesso somente quando necessário;
- aprovar o ensaio e o relatório de reconciliação antes da migração definitiva.

## Riscos controlados

- Dados legados inconsistentes: dry-run, relatório de rejeições e importação idempotente.
- Corrida de estoque ou pagamentos: travas pessimistas e testes de concorrência.
- Prazo comprimido: prioridade para completude do backend; refinamentos visuais ficam no frontend.
- Produção: nenhum corte definitivo sem backup e reconciliação assinada.
