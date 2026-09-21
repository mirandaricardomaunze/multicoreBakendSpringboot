# Especificação Canónica: Padronização dos KPI Cards na UI Desktop
**Código:** SPEC-KPI-001  
**Módulo Principal:** `desktop` (`mz.multicore.erp.gui.components.KpiCard`)  
**Painéis Alvo:** `CreditRiskPanel.java`, `StockWastePanel.java`, `CustomerStatementPanel.java`, `SupplierStatementPanel.java`  
**Data:** 2026-09-17  
**Estado:** APROVADO / EM IMPLEMENTAÇÃO  

---

## 1. Objectivo
Erradicar a duplicação de layouts e cartões construídos de forma ad-hoc nos ecrãs de gestão operacional e financeira do Multicore ERP, adotando universalmente o factory method canónico `KpiCard.createMetricCard(...)`.

Esta unificação garante:
1. **Consistência Visual Absoluta:** O mesmo raio de curvatura (`ModernPanel`), proporção de ícone (`GridBagLayout`), hierarquia tipográfica e espaçamentos padronizados.
2. **Suporte Nativo a Temas (Light / Dark):** Cores de fundo e contraste delegadas no `UIHelper` (`UIHelper.BG_CARD`, `UIHelper.TEXT_LIGHT`, `UIHelper.TEXT_MUTED`), sem recurso a literais de cores espalhados.
3. **Redução e Limpeza de Código:** Eliminação de dezenas de linhas de código boilerplate duplicadas em cada painel, garantindo que nenhum painel ultrapassa o limite arquitetural de 1000 linhas.

---

## 2. Assinatura do Contrato de Reutilização
No componente `mz.multicore.erp.gui.components.KpiCard`:
```java
public static ModernPanel createMetricCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor)
public static ModernPanel createMetricCard(String title, String value, String subtitle, String iconName, Color iconColor)
```

---

## 3. Painéis e Métricas Refatoradas

| Painel | Métricas Padronizadas com `KpiCard` |
|---|---|
| `CreditRiskPanel.java` | 1. Total da Carteira / Dívida Total<br>2. Total Vencido em Mora<br>3. Clientes com Risco Crítico<br>4. Limite Global Concedido |
| `StockWastePanel.java` | 1. Quebras Totais Acumuladas<br>2. Custo Financeiro das Perdas (MT)<br>3. Quebras por Validade Expirada<br>4. Quebras por Avaria / Danos |
| `CustomerStatementPanel.java` | 1. Saldo Devedor Actual<br>2. Faturas em Aberto<br>3. Total Liquidado<br>4. Último Movimento Registado |
| `SupplierStatementPanel.java` | 1. Saldo a Pagar a Fornecedores<br>2. Faturas de Compra Pendentes<br>3. Total de Pagamentos Efectuados<br>4. Prazo Médio de Pagamento |

---

## 4. Restrições Não-Negociáveis
- **Guarda de Tamanho:** Nenhum dos painéis refatorados pode exceder 1000 linhas (imposto por `UiPanelDecompositionTest`).
- **Uniformidade de Cores:** Zero cores instanciadas via `new Color(...)` fora do `UIHelper` (imposto por `FinalUiUniformityHarnessTest`).
- **Preservação de Funcionalidades:** Toda a atualização dinâmica via `setText(...)` dos `JLabel` de valores nos cartões continua 100% operacional.
