# SPEC-FISCAL-PANEL-DECOMPOSITION-001: Decomposição Modular do FiscalPanel (Fase 11)

## 1. Contexto & Diagnóstico
O `FiscalPanel.java` é actualmente o maior painel monolítico do sistema Multicore ERP, com **798 linhas de código**. Além disso, contém **2 chamadas legadas a `JOptionPane`** (`JOptionPane.showMessageDialog` na validação de SAF-T e `JOptionPane.showConfirmDialog` na eliminação de retenções na fonte), que violam as regras canónicas de experiência de utilizador e bloqueiam a catraca de qualidade no `ProfessionalFeedbackHarnessTest`.

O painel agrega 5 responsabilidades fiscais e tributárias fundamentais de Moçambique:
1. **Apuramento do IVA**: Cálculo do IVA Liquidado (vendas), Deduzido (compras), reporte de crédito anterior e saldo líquido fiscal, além de emissão do Modelo A e geração/validação de SAF-T MZ.
2. **Tabela de Taxas Fiscais**: Registo, actualização e activação/desactivação de taxas tributárias (IVA padrão 16%, reduzido, isento, etc.).
3. **Retenções na Fonte**: Registo, controlo de entrega ao Estado e anulação de retenções em pagamentos a prestadores de serviços, rendas e não-residentes.
4. **IRPS & INSS Salarial**: Mapa fiscal e resumo mensal de retenções fiscais da folha de salários (IRPS e Segurança Social).
5. **Declarações Fiscais**: Ponto central de emissão de guias e declarações oficiais para a Autoridade Tributária (AT).

## 2. Objectivos Arquitecturais
- **Limite Físico de Linhas:**
  - `FiscalPanel.java` $\le 160$ linhas (redução $\ge 80\%$).
  - `FiscalIvaTab.java` $\le 260$ linhas.
  - `FiscalTaxRatesTab.java` $\le 180$ linhas.
  - `FiscalWithholdingsTab.java` $\le 200$ linhas.
  - `FiscalPayrollTab.java` $\le 140$ linhas.
  - `FiscalDeclarationsTab.java` $\le 100$ linhas.
- **Zero Diálogos Legados:** 100% das chamadas a `JOptionPane` eliminadas, substituídas por `ToastManager`, `ModernMessageDialog` e `ModernFormDialog`.
- **Preservação Integral de Contratos e APIs:**
  - Preservar construtor público `FiscalPanel(FiscalApiClient fiscalApiClient)`.
  - Preservar métodos públicos `onPanelSelected()` e `refreshAll()`.
  - Compatibilidade total com `DesktopThinContextTest` e testes de injeção.
- **Auditoria de Retenção de Catraca:** Remoção de `FiscalPanel.java` da lista de exceções do `ProfessionalFeedbackHarnessTest`.

## 3. Matriz de Componentes e Responsabilidades

| Componente | Responsabilidade Principal | Limite Linhas |
|---|---|---|
| `FiscalPanel.java` | Orquestrador principal, inicialização de abas e ciclo de vida | $\le 160$ |
| `FiscalIvaTab.java` | Apuramento mensal de IVA, KPIs, tabelas de vendas/compras, Modelo A e SAF-T | $\le 260$ |
| `FiscalTaxRatesTab.java` | Gestão de taxas fiscais, tabela estilizada, filtro e diálogo de taxa | $\le 180$ |
| `FiscalWithholdingsTab.java` | Registo de retenções, entrega ao Estado, eliminação segura com confirmação | $\le 200$ |
| `FiscalPayrollTab.java` | Mapa fiscal de salários, resumo de IRPS/INSS e impressão de mapa oficial | $\le 140$ |
| `FiscalDeclarationsTab.java` | Hub de declarações oficiais e guias para a Autoridade Tributária | $\le 100$ |

## 4. Estratégia de Migração dos Diálogos Legados
1. **Validação SAF-T (`validateSaft`):**
   - Substituir `JOptionPane.showMessageDialog` por diálogo modal moderno via `ModernFormDialog` com visualização estruturada de erros e styling corporativo.
2. **Eliminação de Retenção (`deleteWithholding`):**
   - Substituir `JOptionPane.showConfirmDialog` por `ModernMessageDialog.confirm(...)`.
