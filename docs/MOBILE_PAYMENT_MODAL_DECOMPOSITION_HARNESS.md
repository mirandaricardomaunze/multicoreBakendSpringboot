# Plano de Testes de Harness: Decomposição de MobilePaymentModal

- **Document ID**: HARNESS-MOBILE-PAYMENT-MODAL-001
- **Fase**: 77
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.pos.MobilePaymentModalDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **MPM-01** | Limites físicos estritos de linhas | `MobilePaymentModal.java` $\le 125$ linhas; `MobilePaymentInitiationPanel.java` $\le 145$ linhas; `MobilePaymentWaitingPanel.java` $\le 130$ linhas; `MobilePaymentWorkflowCoordinator.java` $\le 135$ linhas. |
| **MPM-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas a `JOptionPane.`. |
| **MPM-03** | Preservação do contrato público | Método estático `MobilePaymentModal.show(...)` permanece inalterado e funcional. |
| **MPM-04** | Integração de áudio e feedback | Acionamentos de `PosAudioFeedbackEngine` preservados para eventos `SUCCESS` e `ERROR`. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=MobilePaymentModalDecompositionHarnessTest,MobilePaymentModalHarnessTest,ProfessionalFeedbackHarnessTest,BrightButtonsHarnessTest,DesktopThinContextTest"
```
