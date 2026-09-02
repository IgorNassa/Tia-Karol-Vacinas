# Operação do backend

## Ambientes

- `dev`: PostgreSQL local, documentação pública e CORS apenas para `http://localhost:3000`.
- `prod`: exige PostgreSQL com SSL, senha não provisória e origens CORS HTTPS exatas. O Swagger UI fica desligado e o contrato exige usuário `ADMIN`.
- Segredos ficam no gerenciador do ambiente. Nunca em imagem Docker, Git, log ou frontend.

Construção da imagem:

```bash
docker build -t tia-karol-api:1.0.0 .
docker run --rm -p 8080:8080 --env-file .env tia-karol-api:1.0.0
```

O processo roda com usuário sem privilégios, encerra requisições de forma graciosa e limita o heap a 75% da memória do container.

## Sinais operacionais

- Liveness: `GET /actuator/health/liveness`. Reinicie a instância se falhar repetidamente.
- Readiness: `GET /actuator/health/readiness`. Retire a instância do balanceador enquanto falhar.
- Logs: saída padrão estruturada em uma linha, com `requestId`; envie para um coletor com acesso restrito.
- Erros: o cliente recebe `code`, `message`, `path` e `requestId`, nunca stack trace.

O limitador de login/refresh é uma proteção por instância. Em múltiplas réplicas, configure também rate limit no proxy/WAF, usando o IP confiável repassado pelo balanceador.

## Backup Supabase

Antes de publicação, migração de dados ou alteração destrutiva:

1. Confirme no painel do Supabase que o backup gerenciado mais recente terminou e anote data/hora e identificador.
2. Gere também um dump lógico em máquina segura, com credencial temporária e conexão SSL:

```bash
pg_dump "$DATABASE_URL" --format=custom --no-owner --no-acl --schema=app --file=tia-karol-AAAA-MM-DD.backup
pg_restore --list tia-karol-AAAA-MM-DD.backup
```

3. Criptografe o arquivo, guarde fora da máquina da clínica e registre responsável, checksum e prazo de retenção. Não envie backup por e-mail ou mensageiro.
4. Faça teste trimestral de restauração em projeto/banco isolado. Backup não testado não conta como recuperável.

## Restauração

Restauração é uma operação de incidente e requer aprovação registrada:

1. Bloqueie escritas e registre o horário do incidente.
2. Crie um banco vazio de homologação; nunca teste primeiro em produção.
3. Restaure com `pg_restore --clean --if-exists --no-owner --no-acl --dbname="$TARGET_DATABASE_URL" arquivo.backup`.
4. Execute a API com `ddl-auto=validate`, confira Flyway, readiness e os fluxos da coleção de homologação.
5. Compare contagens e amostras sem expor dados pessoais. Só então decida o corte de produção.

## Rollback

- Aplicação: mantenha a imagem anterior imutável e reverta o tráfego para ela.
- Banco: migrações Flyway publicadas são progressivas. Toda mudança deve manter compatibilidade com a versão anterior durante uma janela de rollback.
- Se uma migração incompatível corromper dados, não edite a tabela de histórico do Flyway. Interrompa escritas e restaure o backup/PITR seguindo o procedimento acima.
- Depois do rollback, valide login, readiness, paciente, estoque, agendamento, aplicação e pagamento; registre causa e ação corretiva.

Referências: [SSL no Supabase](https://supabase.com/docs/guides/platform/ssl-enforcement), [conexão `psql` com verificação de certificado](https://supabase.com/docs/guides/database/psql) e [restauração para ambiente self-hosted](https://supabase.com/docs/guides/self-hosting/restore-from-platform).
