# Checklist de publicação

## Antes

- [ ] PR aprovada; CI unitária e integrada verde; nenhuma credencial no diff ou histórico novo.
- [ ] Contrato OpenAPI revisado e versionado; coleção de homologação aprovada.
- [ ] Migrações Flyway revisadas quanto a trava, volume e compatibilidade com rollback.
- [ ] Variáveis `prod`, CORS HTTPS, TLS do banco e menor privilégio conferidos por duas pessoas.
- [ ] Backup/PITR confirmado e restauração de ensaio recente com evidência.
- [ ] Imagem atual e anterior identificadas por digest; janela, responsável e canal de incidente definidos.

## Publicação

- [ ] Aplicar uma instância sem tráfego e aguardar Flyway/readiness.
- [ ] Executar smoke tests sem dados reais; conferir logs por `requestId`.
- [ ] Liberar tráfego gradualmente e observar erros, latência, conexões, CPU e memória.
- [ ] Validar login e fluxos de paciente, estoque, agendamento, aplicação e pagamento.

## Depois

- [ ] Conciliar contagens e totais; confirmar ausência de erro anormal por 30 minutos.
- [ ] Registrar versão, horário, executor, evidências e decisão de encerramento.
- [ ] Revogar credenciais temporárias e apagar artefatos locais com dados sensíveis.
- [ ] Em falha, interromper tráfego, executar rollback de aplicação e seguir `OPERATIONS.md` para banco.
