# Especificação Arquitetural: Decomposição de MobilePaymentModal

- **Document ID**: SPEC-MOBILE-PAYMENT-MODAL-DECOMPOSITION-001
- **Fase**: 77
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Alvo**: `desktop/src/main/java/mz/multicore/erp/gui/MobilePaymentModal.java` (468 linhas)

---

## 1. Contexto e Justificação

O `MobilePaymentModal.java` é o diálogo executivo para pagamento móvel Push USSD (M-Pesa e e-Mola) no POS e renovação de subscrições da empresa. Ele envia requisições de débito ao telemóvel do cliente, monitoriza a autorização de PIN em tempo real com contagem regressiva e emite feedback sonoro e visual.

### Dívidas Técnicas Identificadas
1. **Monolito Funcional (468 linhas)**: Combina apresentação de dados do operador/telefone, validação de prefixos moçambicanos (Vodacom 84/85, Movitel 86/87), painel visual de contagem regressiva e polling assíncrono com a API.
2. **Resíduo de JOptionPane (Linha 330)**: Presença de `JOptionPane.showMessageDialog` em falhas de comunicação, impedindo o avanço da catraca de feedback profissional do ERP.
3. **Mojibake em strings**: Caracteres corrompidos em mensagens de erro ("Comunicao").

---

## 2. Desenho Arquitetural da Decomposição

A decomposição decompõe o diálogo em componentes altamente coesos e especializados:

```
+-------------------------------------------------------------------------------+
| MobilePaymentModal (Orquestrador JDialog <= 125 linhas)                       |
| - Janela modal e content pane                                                 |
| - Header visual corporativo                                                   |
| - Integração entre painéis e coordenador                                      |
| - approvedReference de retorno                                                |
+-------------------------------------------------------------------------------+
     |                                |                                   |
     v                                v                                   v
+-----------------------+  +----------------------+  +--------------------------+
| MobilePayment         |  | MobilePayment        |  | MobilePaymentWorkflow    |
| InitiationPanel       |  | WaitingPanel         |  | Coordinator              |
| (<= 145 linhas)       |  | (<= 130 linhas)      |  | (<= 135 linhas)          |
| - Card de montante    |  | - Painel visual USSD |  | - Workers assíncronos    |
| - Combo operador      |  | - Contagem e barra   |  | - Timers (countdown+poll)|
| - Input e validação   |  | - Ações cancelar     |  | - Áudio e callbacks      |
| - Botão USSD          |  | - Simular aprovação  |  | - Cancelamento seguro    |
+-----------------------+  +----------------------+  +--------------------------+
```

---

## 3. Limites Físicos Estritos

| Classe | Teto de Linhas | Responsabilidade |
|---|---|---|
| `MobilePaymentModal.java` | $\le 125$ | Orquestrador do modal e ciclo de vida do diálogo. |
| `MobilePaymentInitiationPanel.java` | $\le 145$ | Seleção de operador, telefone e validação de prefixos. |
| `MobilePaymentWaitingPanel.java` | $\le 130$ | Contagem regressiva de espera USSD e ações visuais. |
| `MobilePaymentWorkflowCoordinator.java` | $\le 135$ | Coordenador de timers, polling e workers assíncronos. |

---

## 4. Requisitos de Qualidade e Regras Não Negociáveis

1. **Zero `JOptionPane`**: Eliminar a chamada residual e avançar a catraca no `ProfessionalFeedbackHarnessTest` (remover `MobilePaymentModal.java` da allowlist e reduzir o teto de 58 para 57).
2. **Preservação de Métodos e Contratos**:
   - `public static String show(Component parent, MobilePaymentProvider initialProvider, BigDecimal totalAmount, POSApiClient client)` mantido intacto.
3. **Feedback Profissional e Idioma**: Textos em português de Moçambique com codificação UTF-8 rigorosa (sem mojibake).
4. **Áudio POS**: Manter acionamento de `PosAudioFeedbackEngine.getInstance().playAsync(...)` em sucessos e erros.
