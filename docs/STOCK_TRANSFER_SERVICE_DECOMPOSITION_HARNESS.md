# Plano de Testes & Harness - StockTransferService (Fase 95)

## 1. Objectivos de Teste

Validar a integridade estrutural, desacoplamento e limites físicos da decomposição de `StockTransferService`.

## 2. Casos de Teste (STSD)

- **STSD-01: Limites físicos estritos de linhas de código:**
  - `StockTransferService.java` $\le 425$ linhas.
  - `StockTransferMovementExecutor.java` $\le 125$ linhas.
  - `StockTransferValidator.java` $\le 105$ linhas.
  - `StockTransferWorkflowCoordinator.java` $\le 105$ linhas.
- **STSD-02: Zero diálogos legados JOptionPane / Println residuais:**
  - Código limpo e headless no backend.
- **STSD-03: Delegação canónica para colaboradores especializados:**
  - `StockTransferService` deve delegar movimentação, validação e workflow para os colaboradores extraídos.
- **STSD-04: Contratos e concorrência preservados:**
  - Preservação da chamada a `findForReservation` e métodos canónicos (`create`, `update`, `approve`, `reject`, `cancel`, `linkToOrder`, `loadForPrint`).
