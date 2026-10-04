# Matriz de Testes (Harness): Fecho de Caixa Cego com Contagem de Notas/Moedas

**Código:** `HARNESS-POS-BLIND-CLOSE-001`  
**Referência:** `SPEC-POS-BLIND-CLOSE-001`  
**Data:** 2026-09-27  

---

## 1. Casos de Teste de Backend

| ID | Cenário | Entrada | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **BC-BE-01** | Fecho cego com saldo exato e discriminação de notas | `closingBalanceReal = 1500`, notas: 1x1000 + 1x500 | `status = CLOSED`, `difference = 0.00`, salva `cashBreakdownJson` |
| **BC-BE-02** | Fecho cego com quebra (falta de caixa) e justificação | `closingBalanceReal = 1400` (esperado 1500), `notes = "Troco incorreto"` | Requer permissão gerencial; salva diferença de -100.00 MT e `closingNotes` |
| **BC-BE-03** | Fecho cego com sobra de caixa e justificação | `closingBalanceReal = 1550` (esperado 1500), `notes = "Sobra de moeda"` | Requer permissão gerencial; salva diferença de +50.00 MT e `closingNotes` |
| **BC-BE-04** | Preservação de retrocompatibilidade de `CloseSessionRequest` | Chamada sem notas nem discriminação | Executa fecho padrão sem exceções |
| **BC-BE-05** | Emissão de Relatório Z com tabela de contagem física | Sessão fechada com `cashBreakdownJson` e `notes` | PDF gerado inclui seção de contagem física e observações de fecho |

---

## 2. Casos de Teste de Desktop (UI Swing)

| ID | Cenário | Entrada na Interface | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **BC-UI-01** | Contagem por botões de incremento e inputs | Clicar `+1` na nota de 1000 MT e digitar `2` na nota de 500 MT | Total atualiza dinamicamente para `2000.00 MT` |
| **BC-UI-02** | Modo alternativo de introdução direta | Alternar para "Valor Global Direto" e digitar `1850.50` | Sincroniza total de fecho para `1850.50 MT` |
| **BC-UI-03** | Validação antes de submeter | Deixar montante vazio ou negativo | Bloqueia submissão e foca campo com Toast de erro |
| **BC-UI-04** | Exibição de reconciliação pós-fecho | Fecho submetido com sucesso | Revela card com Esperado, Contado, Diferença e ativa botão de impressão do Relatório Z |
| **BC-UI-05** | Instanciação e conformidade arquitetural | `PosBlindCloseDialog` aberto no EDT | Não lança exceções e respeita ergonomia e tema |
