# Professional Feedback Cleanup Specification

## 1. Contexto e Diagnóstico
O teste arquitetural `ProfessionalFeedbackHarnessTest` assegura a erradicação progressiva de caixas de diálogo nativas do Swing (`JOptionPane`), exigindo:
1. Que o inventário total de chamadas a `JOptionPane` em todo o módulo Desktop seja menor ou igual a 62 (`total <= 62`).
2. Que apenas ficheiros expressamente autorizados na allowlist possam conter chamadas residuais legadas.

Quatro ficheiros recentes introduziram chamadas diretas a `JOptionPane`, violando o teto e a allowlist:
- `OrderPickingDialog.java`: 2 confirmações de separação (`showConfirmDialog`).
- `InvoiceInstallmentsDialog.java`: 1 prompt de valor a amortizar (`showInputDialog`).
- `tv/BusinessUnitGoalManager.java`: 1 aviso de sucesso (`showMessageDialog`).
- `commercial/StandaloneDeliveryGuideDialog.java`: 1 aviso de erro (`showMessageDialog`).

## 2. Invariantes e Padrão Corporativo
- Avisos de sucesso e erro devem utilizar estritamente o `ToastManager` (`ToastManager.success(...)`, `ToastManager.error(...)`).
- Solicitação de montantes monetários deve utilizar `UIHelper.promptAmount(...)`, que utiliza `ModernFormDialog` com validação de dados em tempo real.
- Confirmações operacionais devem ser encapsuladas centralmente em `UIHelper.confirm(Component parent, String message, String title)`, mantendo `UIHelper.java` como o único ponto de abstração permitido.

## 3. Harness de Validação
- `ProfessionalFeedbackHarnessTest`
