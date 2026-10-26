# Plano de Testes de Harness: Decomposição de LoginDialog

- **Document ID**: HARNESS-LOGIN-DIALOG-001
- **Fase**: 75
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.LoginDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **LGD-01** | Limites físicos estritos de linhas | `LoginDialog.java` $\le 200$ linhas; `LoginCredentialsCard.java` $\le 295$ linhas; `LoginBackgroundPanel.java` $\le 85$ linhas. |
| **LGD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **LGD-03** | Reutilização canónica de componentes | `LoginDialog.java` instancia e utiliza `LoginBackgroundPanel` e `LoginCredentialsCard`. |
| **LGD-04** | Preservação de contratos de navegação | `UiOrganizationNavigationHarnessTest` continua 100% aprovado. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=LoginDialogDecompositionHarnessTest,UiOrganizationNavigationHarnessTest,DesktopThinContextTest"
```
