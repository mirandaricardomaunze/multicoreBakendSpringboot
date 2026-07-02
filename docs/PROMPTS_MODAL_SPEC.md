# Spec — Prompts de dados em modal profissional (motivos + valores)

> Substitui os `JOptionPane.showInputDialog` de **entrada de dados** (motivos de anulação/rejeição e
> valores de caixa) por modais profissionais reutilizáveis. **Só apresentação/UX.**

**Última actualização:** 2026-06-30

## Problema

Várias acções pediam dados numa caixa cinzenta do sistema (`showInputDialog`), sem cabeçalho/ícone
nem validação que mantenha o diálogo aberto — destoava dos `ModernFormDialog` do resto do ERP.

## Decisões

- **Dois helpers reutilizáveis** no [UIHelper](../src/main/java/com/phcpro/gui/components/UIHelper.java)
  (DRY), ambos sobre `ModernFormDialog` (cabeçalho premium, contido na janela, validação no
  `setOnSave` → erro mantém aberto):
  - `promptRequiredText(title, icon, subtitle, label)` → texto obrigatório (área com *wrap*);
    devolve o texto ou `null` se cancelado.
  - `promptAmount(title, icon, subtitle, label, min)` → valor monetário (aceita `,` ou `.`),
    com mínimo opcional; devolve `BigDecimal` ou `null`.
- **Call sites migrados:**
  - **POS:** Abrir Caixa (`fas-lock-open`) e Fechar Caixa (`fas-lock`) → `promptAmount(min=0)`.
  - **Motivos** (`promptRequiredText`): anular **Fatura**/**Recibo** (`fas-ban`), rejeitar **NC**/
    **ND** (`fas-times-circle`) no Comercial; cancelar **Encomenda** no Compras (`fas-ban`);
    rejeitar **Guia** de transferência no Stock (`fas-times-circle`); rejeitar **Férias** no RH
    (`fas-times-circle`).
- **Limpeza (C):** removido o `StockPanel.createWarehouseDialog` **V1** (código morto — o botão usa
  o V2 em `ModernFormDialog`).

## Não-objetivos

- Não alterar serviços/regras nem as confirmações Sim/Não (`showConfirmDialog`).
- Não introduzir nova biblioteca.
