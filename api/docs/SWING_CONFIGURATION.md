# Configuração segura do Swing legado

As credenciais fixas foram removidas. Antes de iniciar o Swing, defina as três variáveis da `.env.swing.example` no sistema operacional. O arquivo é apenas modelo e não é carregado automaticamente.

PowerShell, somente para a sessão atual:

```powershell
$env:LEGACY_DATABASE_URL = "jdbc:postgresql://SERVIDOR:5432/vacin_control?sslmode=require"
$env:LEGACY_DATABASE_USERNAME = "usuario"
$env:LEGACY_DATABASE_PASSWORD = Read-Host "Senha do banco" -MaskInput
java -jar sistema-legado.jar
```

Como alternativa, a JVM aceita `-Dlegacy.database.url`, `-Dlegacy.database.username` e `-Dlegacy.database.password`. Não salve a senha em atalho, script versionado ou argumento visível em histórico. Para a instalação definitiva, prefira um gerenciador de segredos/credenciais do Windows e uma conta PostgreSQL exclusiva com o menor privilégio necessário.

A senha que já apareceu no histórico do repositório deve ser considerada comprometida. Depois de configurar estas
variáveis na máquina da clínica, altere a senha do usuário PostgreSQL legado, atualize a variável local e confirme a
conexão antes de encerrar a janela de manutenção. Não reescreva o histórico do Git sem um plano coordenado; a rotação
da credencial é o controle imediato indispensável.
