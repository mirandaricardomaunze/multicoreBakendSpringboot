# SPEC-AI-OPERATIONAL-DRAFT-001

## Objectivo

Permitir que o Assistente de Gestão prepare rascunhos controlados de cotações, encomendas a
fornecedor, pedidos de cliente, memorandos de cobrança, planos de reposição e propostas comerciais.

## Regras obrigatórias

- O rascunho nunca é persistido, emitido ou aprovado pela IA.
- Apenas dados escritos explicitamente pelo utilizador podem ser extraídos.
- Cliente, fornecedor, produto, conta, imposto, preço e quantidade nunca são inventados.
- Dados em falta são apresentados antes de abrir o módulo responsável.
- `MANAGER` ou `ADMIN`, empresa activa e auditoria são obrigatórios.
- O botão `Abrir no Módulo` apenas navega; os Services existentes continuam responsáveis por
  validação, transacção, permissões e emissão.

## Harness

- `OD-01`: records permanecem em `contracts` e sem dependências de framework.
- `OD-02`: controller chama somente o Service.
- `OD-03`: Service exige função e tenant.
- `OD-04`: allowlist cobre os seis rascunhos previstos.
- `OD-05`: resposta contém campos explícitos e dados em falta.
- `OD-06`: auditoria não guarda o pedido livre.
- `OD-07`: desktop usa chamada assíncrona e botão de navegação.
- `OD-08`: nenhuma rota persiste ou emite documentos.
