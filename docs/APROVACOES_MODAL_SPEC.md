# Spec — Decisão de aprovações em modal profissional

> Substitui o inspector **inline** da área de Aprovações ([ApprovalsPanel](../src/main/java/com/phcpro/gui/ApprovalsPanel.java))
> por um modal de decisão ([ModernFormDialog](../src/main/java/com/phcpro/gui/components/ModernFormDialog.java)).
> **Só apresentação/UX** — a lógica de aprovar/rejeitar e o `ApprovalService` não mudam.

**Última actualização:** 2026-06-30

## Problema

A fila de aprovações tinha um painel lateral "Inspetor de Detalhes" montado à mão (labels + área de
descrição) com botões Aprovar/Rejeitar embutidos:

- Layout em `GridLayout(1,2)` desperdiçava metade do ecrã num inspector sempre presente (vazio até
  haver selecção); a tabela de pendentes ficava espremida.
- Cores **hardcoded** na área de descrição; aspecto inconsistente com os modais do resto do ERP.
- Aprovar/Rejeitar agarrados ao painel; a rejeição usava `JOptionPane` solto.

## Decisões

- **Tabela de pendentes a largura total** com barra de acções (botão **Abrir / Decidir**, `fas-gavel`,
  activo só com linha seleccionada). Histórico mantém-se em baixo.
- **Modal de decisão** (`ModernFormDialog`, ícone `fas-clipboard-check`, subtítulo "Tipo #id") aberto
  por **duplo-clique** ou pelo botão. Mostra os campos do pedido **só-leitura** (Tipo, ID, Solicitante,
  Valor, Perfil Requerido) + **Descrição/Justificação** com *wrap*.
- **Rodapé com 3 acções:** **Rejeitar** (danger, pede motivo obrigatório), **Fechar** e **Aprovar**
  (confirma via `setOnSave`; em erro de autorização o modal mantém-se aberto com a mensagem).
- **Novas capacidades reutilizáveis no `ModernFormDialog`:** `addActionButton(...)` (botão extra no
  rodapé) e `close()`.
- **Duplo-clique dedicado:** a tabela de pendentes marca `noRowInspector` para o `styleTable` **não**
  abrir o inspector genérico — abre o modal de decisão.
- Campos pelos **estilizadores centrais** (`styleTextField`/`styleTextArea`) — coerentes com o tema.

## Não-objetivos

- Não alterar regras de autorização, auditoria ou estados (`ApprovalService` intacto).
- Não mexer no histórico/auditoria além de continuar a listá-lo.
- Não introduzir nova biblioteca.
