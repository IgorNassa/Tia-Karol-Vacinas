# LGPD, dados sensíveis e retenção

Este documento é um controle técnico inicial e precisa de validação do responsável jurídico/encarregado da clínica antes da produção.

## Controles implementados

- Schema `app` privado, sem exposição pela Data API do Supabase.
- TLS obrigatório no perfil de produção, CORS por origem exata e autenticação com tokens opacos armazenados somente como hash.
- Menor privilégio: `ADMIN` controla financeiro, relatórios, usuários e escrita de estoque; `ATTENDANT` não vê financeiro; `APPLICATOR` apenas consulta agenda e registra aplicação; `FINANCIAL` permanece sem acesso até a função ser aprovada.
- Auditoria das operações de negócio, correlação de requisições e erros sem stack trace para o cliente. Os snapshots de auditoria mascaram identificação, contato, endereço, alergias, reações, observações, senhas e tokens; preservam o ID, ação, responsável, horário e campos operacionais.
- Logs não devem conter corpo HTTP, token, senha, CPF, documento, telefone, endereço, alergia, reação ou observação clínica.

## Matriz proposta de retenção

| Dado | Retenção técnica proposta | Encerramento |
|---|---:|---|
| Logs técnicos sem dado pessoal | 90 dias | exclusão automática |
| Logs de auditoria | 5 anos | anonimização/exclusão após validação legal |
| Tokens e sessões expiradas | 30 dias após expiração | exclusão automática |
| Backups operacionais | 35 dias | exclusão criptográfica conforme política do provedor |
| Relatórios de migração | até aceite + 90 dias | exclusão segura |
| Cadastro e histórico clínico | prazo legal aplicável | anonimização ou eliminação somente com autorização jurídica |
| Financeiro/fiscal | prazo fiscal aplicável | eliminação somente após validação contábil/jurídica |

## Regra dos pacientes inativos

A regra de negócio “inativo há três meses pode ser apagado” não deve executar exclusão automática enquanto existirem agendamentos, aplicações, pagamentos, auditoria ou obrigação legal de conservação. Após três meses, o sistema pode apenas colocar o cadastro em uma fila de revisão. A decisão deve preferir anonimização quando for necessário manter histórico clínico/financeiro.

Se os registros forem enquadrados como prontuário médico, há orientação do CFM de que a eliminação só pode alcançar registros com último lançamento há pelo menos 20 anos, cumpridos os demais requisitos. A clínica precisa confirmar com seu responsável jurídico e conselho profissional qual norma alcança sua operação.

Antes de habilitar limpeza definitiva, documente finalidade e base legal de cada tratamento, canal para direitos do titular, responsável pelo atendimento, processo de incidente, prazo aprovado por categoria e evidência da autorização. Faça teste de restauração e registre a exclusão em trilha sem copiar o dado eliminado.

## Resposta a incidente

1. Contenha o acesso, preserve evidências e rotacione credenciais/tokens afetados.
2. Identifique dados, titulares, período e sistemas envolvidos.
3. Acione o encarregado e avalie comunicação à ANPD e aos titulares nos prazos aplicáveis.
4. Corrija, valide, documente causa raiz e revise controles.

## Referências oficiais

- [Lei Geral de Proteção de Dados Pessoais — Lei nº 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm)
- [ANPD — Guia de segurança da informação para agentes de pequeno porte](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/processo-guia-orientativo-sobre-seguranca-da-informacao-para-agentes-de-tratamento-de-pequeno-porte.pdf)
- [ANPD — Resolução CD/ANPD nº 2/2022](https://www.gov.br/anpd/pt-br/acesso-a-informacao/institucional/atos-normativos/regulamentacoes_anpd/resolucao-cd-anpd-no-2-de-27-de-janeiro-de-2022)
- [CFM — Parecer nº 19/2026 sobre digitalização e descarte de prontuários](https://portal.cfm.org.br/wp-content/uploads/2026/06/19_2026.pdf)
