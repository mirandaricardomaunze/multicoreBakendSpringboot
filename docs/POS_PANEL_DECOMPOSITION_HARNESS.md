# Harness de Testes — Decomposição do Painel de Ponto de Venda (POS)
**Código:** `HARNESS-POS-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/POS_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Comandos de Execução

```powershell
mvn test -pl desktop "-Dtest=PosPanelDecompositionHarnessTest,PosErgonomicsHarnessTest,PosHeaderLayoutHarnessTest,PosProfessionalErgonomicsHarnessTest,PosCustomerRequirementHarnessTest,PosKeyboardShortcutsHarnessTest,POSKeyboardShortcutTest,DesktopThinContextTest"
```

## 2. Validações Automáticas

1. **`DECOMP-01: Limite de Linhas do POSPanel`**
   Garante que `POSPanel.java` possui `<= 450` linhas.
2. **`DECOMP-02: Limite de Linhas dos Componentes Especializados`**
   Garante que:
   - `PosCheckoutCoordinator.java` possui `<= 200` linhas.
   - `PosCartController.java` possui `<= 200` linhas.
   - `PosWorkspaceBuilder.java` possui `<= 300` linhas.
3. **`DECOMP-03: Integridade de Métodos de Atalhos e Teclado`**
   Valida que `clearCart()`, `isHistoryView()`, `editSelectedCartQuantity()`, `removeFromCart()`, `showReturnDialog()` e `runCheckout()` permanecem operacionais e acessíveis.
4. **`DECOMP-04: Cálculo de Imposto Efetivo e Cliente Avulso`**
   Confirma o método estático `effectiveTaxRate()` e a garantia de prompt e validação de nome para cliente avulso.
