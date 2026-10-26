# Harness de Testes — Decomposição Modular de ConfigSubscriptionPanel (Fase 65)

## 1. Suíte de Verificação

Arquivo de teste criado: `desktop/src/test/java/mz/multicore/erp/gui/ConfigSubscriptionPanelDecompositionHarnessTest.java`.

## 2. Casos de Teste Rigorosos

1. **`CSP-01: Limites físicos estritos de linhas de código`:**
   - `ConfigSubscriptionPanel.java` $\le 75$ linhas.
   - `ConfigSubscriptionCardRenderer.java` $\le 95$ linhas.
2. **`CSP-02: Zero diálogos legados JOptionPane`:**
   - Nenhum uso de `JOptionPane.show*` em `ConfigSubscriptionPanel.java` e `ConfigSubscriptionCardRenderer.java`.
3. **`CSP-03: Renderização do cartão de subscrição e estados visuais`:**
   - Validação da renderização dos campos de assinatura activa, estado vazio e estado de erro via `ConfigSubscriptionCardRenderer`.
4. **`CSP-04: Estrutura do painel e botões de cabeçalho`:**
   - Presença dos botões "Renovar / Activar Plano" e "Actualizar".
