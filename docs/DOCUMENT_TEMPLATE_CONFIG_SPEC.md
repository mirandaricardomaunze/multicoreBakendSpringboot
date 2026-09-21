# Especificação — Configuração Visual de Modelos de Documentos A4

**Versão:** 1.0  
**Data:** 2026-09-14  
**Domínio dono:** `documents`  
**Harness:** `DocumentTemplateConfigHarnessTest`

---

## 1. Contexto e motivação

O sistema já possui a infraestrutura técnica de `DocumentColumnConfig` / `DocumentColumnsDTO` /
`LineItemsTableRenderer`, que configura **quais colunas** aparecem num documento A4 por empresa.
Contudo, esta configuração é incompleta para uso em cliente final:

- Não permite configurar **colunas por tipo de documento** (Fatura, Encomenda, Cotação, Guia são tratadas de forma idêntica com um único `DocumentType.COMMERCIAL`).
- Não existe conceito de **modelo predefinido** (Compacto / Fiscal Detalhado / Logístico).
- Não existe **ordem configurável** de colunas.
- Não existe **pré-visualização** antes de gravar.
- Colunas como lote, peso e preço por caixa não são ainda configuráveis.

Esta especificação define a evolução completa do subsistema.

---

## 2. Novos tipos de documento

O `DocumentType` passa a ter um tipo por documento comercial:

| Valor enum       | Label UI               |
|------------------|------------------------|
| `INVOICE`        | Fatura                 |
| `ORDER`          | Encomenda              |
| `QUOTATION`      | Cotação                |
| `DELIVERY_GUIDE` | Guia de Remessa        |
| `POS_RECEIPT`    | Recibo POS             |

> **Retrocompatibilidade:** `COMMERCIAL` é mantido como alias e mapeado internamente para `INVOICE`. Registos existentes na base de dados com `COMMERCIAL` continuam a funcionar sem migração destrutiva.

---

## 3. Novas colunas configuráveis

Além das 8 já existentes, são adicionadas:

| Campo DTO            | Coluna visível        | Aplicável a                  |
|----------------------|-----------------------|------------------------------|
| `batch`              | Lote                  | INVOICE, DELIVERY_GUIDE      |
| `weight`             | Peso (kg)             | DELIVERY_GUIDE               |
| `boxPrice`           | Preço/Caixa           | ORDER, QUOTATION             |
| `discountPct`        | Desconto (%)          | INVOICE, ORDER, QUOTATION    |
| `taxAmount`          | IVA Valor (MZN)       | INVOICE                      |

Total: **13 colunas configuráveis**.

---

## 4. Modelos predefinidos

O administrador pode seleccionar um modelo base que pré-preenche o estado dos checkboxes:

| Modelo                  | Colunas activadas por defeito                                                        |
|-------------------------|--------------------------------------------------------------------------------------|
| `COMPACT`               | Referência, Descrição, Quantidade, Subtotal                                          |
| `FISCAL_DETAILED`       | Referência, Descrição, Validade, Quantidade, Preço Unit., Desconto, IVA%, IVA Valor, Subtotal |
| `LOGISTICS`             | Código de Barras, Referência, Descrição, Lote, Validade, Quantidade, Peso, Subtotal  |

Os modelos são **pré-preenchimento apenas** — o utilizador pode depois ajustar individualmente antes de gravar.

---

## 5. Ordem configurável das colunas

A `DocumentColumnsDTO` passa a incluir um campo `List<String> columnOrder` com os nomes canónicos das colunas na ordem desejada.

Regras:
- A lista contém apenas os nomes das colunas activas (as inactivas são ignoradas).
- Se a lista estiver vazia ou nula, aplica-se a ordem canónica padrão.
- `LineItemsTableRenderer` ordena as colunas conforme a lista antes de construir a tabela PDF.

---

## 6. Pré-visualização (Preview)

A tela de configuração inclui um botão **"Pré-visualizar"** que:

1. Constrói um `DocumentColumnsDTO` com o estado actual dos controlos (sem gravar).
2. Chama o endpoint `POST /api/documents/preview?documentType=INVOICE` (novo) com dados de exemplo fictícios embutidos no backend.
3. Recebe o PDF em `byte[]` e abre-o numa janela `PreviewDialog` (JDialog com `JScrollPane` + renderização via `PDFRenderer` ou extracção de imagem da primeira página via PDFBox).

O preview mostra **apenas a tabela de linhas e totais** (não o cabeçalho da empresa) para ser mais rápido.

---

## 7. Contratos REST

### GET /api/documents/columns
```
Parâmetros: companyId (Long), documentType (DocumentType)
Resposta: DocumentColumnsDTO
```

### PUT /api/documents/columns  
```
Parâmetros: companyId (Long), documentType (DocumentType)
Body: DocumentColumnsDTO
Resposta: DocumentColumnsDTO
```

### POST /api/documents/preview *(NOVO)*
```
Parâmetros: documentType (DocumentType)
Body: DocumentColumnsDTO
Resposta: application/pdf (byte[])
Sem persistência — usa dados fictícios internos.
Requer autenticação; não requer ADMIN (qualquer utilizador pode pré-visualizar).
```

---

## 8. Impacto nos PrintServices existentes

Os `InvoicePrintService`, `OrderPrintService`, `QuotationPrintService` e `DeliveryGuidePrintService` passam a chamar `DocumentConfigService.getColumns(companyId, tipoEspecífico)` em vez de `COMMERCIAL`.

---

## 9. Regras de negócio críticas

- **DT-01:** A pré-visualização nunca persiste dados.
- **DT-02:** Pelo menos uma coluna de conteúdo (não apenas cabeçalho) deve ficar visível — `BusinessRuleException` se o save deixar 0 colunas activas.
- **DT-03:** A coluna `taxAmount` (IVA Valor) não altera o cálculo fiscal — é apenas apresentação visual.
- **DT-04:** O modelo predefinido (`COMPACT` etc.) é um atalho de UI; o que é persistido é sempre o `DocumentColumnsDTO` expandido com todos os campos booleanos.
- **DT-05:** Permissão de gravação: `MANAGER` ou `ADMIN`. Leitura: qualquer utilizador autenticado da empresa.
- **DT-06:** A ordem de colunas no PDF respeita `columnOrder` se presente; caso contrário usa a ordem canónica da especificação.
- **DT-07:** Retrocompatibilidade: `COMMERCIAL` como `documentType` é aceite no endpoint e internamente mapeado para `INVOICE`.

---

## 10. Estrutura de ficheiros a criar/alterar

### contracts/
- `[MODIFY] DocumentColumnsDTO` — adicionar campos: `batch`, `weight`, `boxPrice`, `discountPct`, `taxAmount`, `columnOrder`
- `[MODIFY] DocumentType` — adicionar `INVOICE`, `ORDER`, `QUOTATION`, `DELIVERY_GUIDE`; manter `COMMERCIAL`

### backend/
- `[MODIFY] DocumentColumnConfig` — adicionar colunas JPA para os 5 novos campos
- `[MODIFY] DocumentConfigService` — suportar novos campos; novo método `renderPreview(DocumentType, DocumentColumnsDTO)`
- `[MODIFY] DocumentConfigController` — novo endpoint `POST /api/documents/preview`
- `[MODIFY] LineItemsTableRenderer` — suportar novos campos e `columnOrder`
- `[MODIFY] InvoicePrintService` — usar `DocumentType.INVOICE`
- `[MODIFY] OrderPrintService` — usar `DocumentType.ORDER`
- `[MODIFY] QuotationPrintService` — usar `DocumentType.QUOTATION`
- `[MODIFY] DeliveryGuidePrintService` — usar `DocumentType.DELIVERY_GUIDE`
- `[NEW] DocumentPreviewService` — gera PDF de preview com dados fictícios

### desktop/
- `[MODIFY] DocumentConfigApiClient` — novo método `preview(DocumentType, DocumentColumnsDTO)`
- `[MODIFY] ConfigPanel` — refatorar `createDocumentColumnsTab()` para o novo `DocumentTemplateConfigPanel`
- `[NEW] DocumentTemplateConfigPanel` — painel completo com modelo, checkboxes, ordem, preview

### testes/
- `[NEW] DocumentTemplateConfigHarnessTest` — harness DT-01 a DT-07
- `[MODIFY] DocumentConfigServiceTest` — cobrir novos campos e preview

---

## 11. Critérios de aceitação

1. Administrador selecciona "Fatura" → configura colunas → clica "Pré-visualizar" → vê PDF correctamente sem gravar.
2. Clica "Guardar" → a próxima impressão de fatura reflecte as colunas configuradas.
3. Selecciona modelo "Compacto" → checkboxes preenchem automaticamente.
4. Tenta guardar com 0 colunas → mensagem "O documento tem de ter pelo menos uma coluna."
5. `COMMERCIAL` como parâmetro legado continua a funcionar sem erro.
