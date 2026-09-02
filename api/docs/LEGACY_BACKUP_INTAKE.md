# Recebimento seguro do banco legado

Este roteiro coleta o banco usado pelo Swing sem modificar a instalação da clínica. O primeiro processamento será
feito em ambiente isolado e somente leitura; nada será enviado ao Supabase antes da conferência das contagens,
regras e pendências.

## Informações a registrar

- versão do PostgreSQL exibida no pgAdmin;
- nome do servidor e do banco, sem registrar senha;
- data, hora e fuso do backup;
- versão do sistema Swing em uso;
- se havia usuários trabalhando durante a coleta;
- tamanho do arquivo e SHA-256.

## Backup completo pelo pgAdmin

1. Abra o pgAdmin na máquina da clínica e confirme qual banco a aplicação usa. Não altere tabelas ou configurações.
2. Clique com o botão direito no banco e escolha **Backup**.
3. Selecione o formato **Custom**, um arquivo terminado em `.backup` e compressão padrão.
4. Inclua todos os schemas e as seções `pre-data`, `data` e `post-data`. Não limite tabelas.
5. Ative as opções equivalentes a **no owner** e **no privileges**. Não ative `clean` nem `create database`.
6. Execute, espere a mensagem de sucesso e não feche o pgAdmin durante o processo.
7. Faça um segundo backup **Only schema**, em formato Plain, salvando como `legacy-schema.sql`. Ele acelera o
   mapeamento e não substitui o backup completo.

O dump customizado do PostgreSQL usa um snapshot consistente. Ainda assim, registre se o sistema estava em uso,
pois alterações concluídas depois do início do backup não estarão no arquivo.

## Conferência antes da análise

No PowerShell, dentro da pasta segura que contém os arquivos:

```powershell
Get-FileHash .\tia-karol-legado.backup -Algorithm SHA256
Get-Item .\tia-karol-legado.backup | Select-Object Name, Length, LastWriteTime
```

Se `pg_restore` estiver disponível, confirme que o catálogo pode ser lido:

```powershell
pg_restore --list .\tia-karol-legado.backup
```

Não envie os arquivos por e-mail, WhatsApp, Notion ou GitHub. Guarde-os em pasta local de acesso restrito. Para a
análise, informe apenas o caminho local; a pasta `private-migration/` e arquivos `.backup`/`.dump` já estão ignorados
pelo Git.

## O que faremos ao receber

1. Validar hash, formato, versão e catálogo sem restaurar em produção.
2. Restaurar em PostgreSQL isolado, sem rede pública e com credencial temporária.
3. Executar o analisador `dry-run`, inventariar tabelas, volumes, duplicidades e dados inválidos.
4. Produzir o mapa legado → API e as regras de transformação para sua aprovação.
5. Ensaiar uma importação idempotente, reconciliar totais e gerar relatório sem dados pessoais.
6. Só depois de backup do destino e autorização explícita executar a carga no Supabase.

Bloqueiam a importação: backup ilegível, contagens inconclusivas, documentos duplicados sem decisão, estoque
negativo, aplicações sem paciente/lote e qualquer divergência financeira não explicada.
