# Plano de homologação

Use banco isolado e dados fictícios. Execute `http/tia-karol-api.http` e guarde as evidências com versão da imagem, commit, ambiente e responsável.

## Critérios de aceite

1. Liveness e readiness retornam `UP`; toda resposta tem `X-Request-ID`.
2. Login válido funciona; senha inválida retorna `401`; excesso de tentativas retorna `429` sem revelar se o usuário existe.
3. Matriz de perfis passa integralmente: atendente não acessa financeiro; aplicador não cria/cancela/reagenda; somente administrador consulta histórico/estorna pagamentos e altera estoque.
4. Paciente brasileiro, estrangeiro e recém-nascido obedecem às regras de identidade, responsáveis e alergias; CPF ativo duplicado é recusado.
5. Lote repetido só soma estoque quando todos os dados conferem; vencido/inativo não agenda nem aplica; ajuste exige motivo.
6. Agendamento reserva uma dose; aplicação baixa físico e reservado; cancelamento devolve reserva; falta entra em pendências e exige decisão administrativa.
7. Carrinho aceita múltiplos meios apenas quando a soma fecha; estorno exige motivo e preserva histórico.
8. Relatórios conciliam estoque, aplicações e financeiro com os registros de teste.
9. OpenAPI gerado é idêntico ao arquivo versionado.
10. Backup foi validado por listagem e restauração de ensaio; rollback de imagem foi simulado.

Falha em segurança, perda/duplicação de estoque, divergência financeira, migração ou restauração bloqueia a publicação. Registre defeito, severidade, evidência, responsável e reteste.
