# Spec — Rascunho e decisão de transferências de stock

## Objectivo

Substituir a criação modal e imediatamente pendente por uma área de trabalho tabular a ecrã inteiro,
com linhas pesquisáveis e um ciclo de vida explícito e auditável.

## Máquina de estados

`DRAFT → PENDING_APPROVAL → APPROVED | REJECTED`

`DRAFT | PENDING_APPROVAL → CANCELLED`

- `DRAFT`: cabeçalho e linhas podem ser alterados; não reserva nem movimenta stock.
- `PENDING_APPROVAL`: conteúdo bloqueado para edição e disponível para decisão.
- `APPROVED`: estado terminal; a aprovação consome FEFO na origem, replica os lotes no destino e grava os movimentos `TRANSFER`.
- `REJECTED` e `CANCELLED`: estados terminais sem movimento de stock.
- Uma guia aprovada, rejeitada ou cancelada é apenas consultável.

## Regras de negócio

1. O número da guia é atribuído uma única vez na criação do rascunho e permanece estável.
2. Origem e destino pertencem à empresa activa e devem ser diferentes.
3. Motorista, matrícula e pelo menos uma linha positiva são obrigatórios para gravar.
4. A disponibilidade é verificada ao gravar e novamente ao submeter; a aprovação continua a executar a validação FEFO autoritativa dentro da transacção.
5. Só `MANAGER`/`ADMIN` aprovam ou rejeitam.
6. A actualização usa versão optimista; uma fotografia desactualizada não sobrescreve alterações.
7. Criação, actualização, submissão, aprovação, rejeição e cancelamento deixam auditoria.
8. Transferências criadas a partir de reposição interna também nascem em `DRAFT` e devem ser revistas e submetidas na área de Stock.

## Contratos HTTP

- `POST /api/inventory/transfers` — cria `DRAFT`.
- `PUT /api/inventory/transfers/{id}` — actualiza apenas `DRAFT`, incluindo a versão.
- `POST /api/inventory/transfers/{id}/submit` — passa `DRAFT` a `PENDING_APPROVAL`.
- `POST /api/inventory/transfers/{id}/approve` — move stock e encerra como `APPROVED`.
- `POST /api/inventory/transfers/{id}/reject` — encerra como `REJECTED` com motivo.
- `POST /api/inventory/transfers/{id}/cancel` — encerra `DRAFT` ou `PENDING_APPROVAL`.

## Interface desktop

- A lista mantém pesquisa e filtros dentro do mesmo `ModernPanel` da tabela.
- `Nova Transferência`, duplo clique e `Editar / Consultar` abrem uma área tabular no próprio separador;
  não existe formulário ou diálogo de edição.
- Os dados gerais seguem o padrão canónico de cabeçalho documental em linha horizontal (`Origem`, `Destino`, `Motorista`, `Matrícula`, `Responsável`, `Viatura`, `Observações`), organizados em campos estilizados em linha (idênticos à Encomenda de Cliente e Cotação), evitando tabelas impróprias no cabeçalho.
- O modelo de edição dos itens segue a grelha documental do PHC: `Produto` (primeira coluna), `Qtd`, `Emb.` e `Cx.` são alterados directamente nas células, sem transportar a linha para campos externos.
- O produto é pesquisado dentro da primeira célula por nome, referência, SKU ou código de barras via `ProductSearchComboBox`.
- Ao alterar quantidade, embalagens ou caixas, as restantes representações e a percentagem da caixa são sincronizadas; referência, código de barras, valor e IVA são derivados do produto.
- Acções da tabela ficam no topo do card (`Adicionar linha`, `Remover item`); totais e contagens ficam no rodapé do card; documentos fora de `DRAFT` abrem em modo de consulta.

## Compatibilidade

- Guias antigas em `PENDING_APPROVAL` continuam pendentes; a migração não reclassifica histórico.
- O novo campo `version` é acrescentado ao fim do DTO, mantendo construtores sobrecarregados para os consumidores anteriores.
