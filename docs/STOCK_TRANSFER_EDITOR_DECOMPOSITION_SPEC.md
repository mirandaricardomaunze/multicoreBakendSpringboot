# Especificação Técnica: Decomposição Modular de StockTransferEditorForm

- **Document ID**: SPEC-STOCK-TRANSFER-EDITOR-001
- **Fase**: 69
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Domínio**: Stock & Armazéns / Editor de Transferência de Stock

---

## 1. Contexto e Motivação

O ficheiro `StockTransferEditorForm.java` acumulava **879 linhas de código**, concentrando num único componente:
1. **Formulário de Cabeçalho Canónico**: Campos de armazéns (origem e destino), condutor, matrícula, viatura, responsável, notas e opt-in de mercadoria em trânsito.
2. **Grelha PHC de Artigos Editável**: 10 colunas canónicas com sincronização bidirecional de unidades, embalagens e caixas, leitor de código de barras e atalhos F2/F3.
3. **Cálculo Avançado de Reposição de Stock**: Mais de 100 linhas dedicadas ao cálculo de stock mínimo vs. saldos por armazém, determinação de unidades por caixa e confirmação interactiva de carregamento.

---

## 2. Decisão Arquitetural

1. **Extração de `StockTransferReplenishmentHelper.java`**:
   - Encapsula o cálculo de reposição inteligente entre armazém de origem e armazém de destino.
   - Avalia saldos correntes (`StockDTO`) vs. stock mínimo configurado.
   - Solicita confirmação corporativa ao utilizador via `ModernMessageDialog.confirm(...)`.
   - Limite físico: $\le 130$ linhas.

2. **Refatoração de `StockTransferEditorForm.java`**:
   - Mantém o ciclo de vida do editor, cabeçalho e grelha canónica.
   - Reduz a contagem física de 879 para $\le 550$ linhas (**redução de ~40%**).
   - Preserva estritamente todos os contratos de integração esperados pelos testes de harness (`StockTransferDraftUiHarnessTest`, `StockInTransitUiHarnessTest`).

---

## 3. Critérios de Aceitação

- [x] Criação de `StockTransferReplenishmentHelper.java` com $\le 130$ linhas.
- [x] `StockTransferEditorForm.java` reduzido para $\le 550$ linhas.
- [x] Zero chamadas a `JOptionPane` em todos os ficheiros.
- [x] Testes existentes e novos de harness passam com 100% de sucesso.
