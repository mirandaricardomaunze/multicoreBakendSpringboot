# SPEC-POR-001: Assistente Canónico de Recepção e Conferência de Mercadorias

## 1. Princípio e Enquadramento Operacional

Ao descarregar mercadorias de uma Encomenda a Fornecedor, a quantidade que entra no armazém nem sempre coincide com o que foi encomendado:
- Mercadoria em bom estado: entra imediatamente em stock.
- Mercadoria danificada/partida na descarga: **não entra em stock**, mas fica registada para nota de devolução / reclamação.
- Falta definitiva: artigos que o fornecedor não entregou e não irá entregar (fecho curto da encomenda).

O assistente de conferência física substitui confirmações cegas por um painel de conferência estruturado.

## 2. Estrutura e Interface do Assistente

O assistente (`PurchaseOrderReceivingDialog`) apresenta:
1. **Cabeçalho com Contexto da Encomenda:** Número da encomenda, fornecedor, armazém de destino e data prevista.
2. **Grelha de Conferência:**
   - `Produto`: Descrição e SKU.
   - `Encomendado`: Quantidade contratada (formatada a 2 casas decimais).
   - `Já Recebido`: Quantidade já descarregada em entregas anteriores (a 2 casas decimais).
   - `Pendente`: Saldo em falta (a 2 casas decimais).
   - `A Receber (Boas)`: Quantidade a dar entrada em stock (editável, 2 casas).
   - `Danificado`: Quantidade rejeitada ou avariada (editável, 2 casas).
   - `Falta Definitiva`: Quantidade que não virá (editável, 2 casas).
   - `Notas / Motivo`: Descrição de divergências para o fornecedor.
3. **Validações Canónicas:**
   - Todas as quantidades devem ser >= 0 e formatadas com 2 casas decimais.
   - O total conferido por linha (`boas + danificadas + falta`) não pode exceder o saldo pendente.
   - Deve ser conferido pelo menos um artigo para submeter a recepção.

## 3. Integração com Backend

- O payload submetido utiliza o DTO canónico `ReceivePurchaseOrderRequest` (`ReceiveLine` com `quantity`, `damagedQuantity`, `missingQuantity`, `notes`).
- O stock do armazém de destino é creditado exclusivamente com as quantidades em boas condições.

## 4. Harness e Verificação

- `PurchaseOrderReceivingHarnessTest` verifica:
  1. Instanciação e estrutura de colunas do diálogo de conferência.
  2. Validação de divergências (`boas + danificadas + falta <= pendente`).
  3. Formatação numérica a 2 casas decimais.
