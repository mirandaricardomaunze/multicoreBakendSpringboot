# Especificação Canónica — Filtro Universal por Período em Tabelas Transacionais

**Código:** `FILTRO_PERIODO_UNIVERSAL_SPEC`  
**Módulo:** `desktop` (componentes e vistas transacionais)  
**Versão:** 1.0  
**Data:** 2026-09-15  

---

## 1. Contexto e Enquadramento

Em sistemas de gestão corporativa (ERP), os utilizadores operacionais (caixas, operadores de faturação, responsáveis de armazém e compradores) consultam diariamente dezenas ou centenas de documentos emitidos.

Anteriormente, o vocabulário em `TableFilter` disponibilizava apenas cinco opções básicas (`Todo o período`, `Hoje`, `Últimos 7 dias`, `Últimos 30 dias`, `Este mês`). Faltavam períodos essenciais de fecho de turno e auditoria:
1. **`Ontem`**: Para conciliação matinal do dia anterior.
2. **`Esta semana`**: Para acompanhamento do ciclo semanal (segunda-feira da semana corrente até o dia atual).
3. **`Este ano`**: Para apuramento acumulado do exercício fiscal corrente.

Adicionalmente, tabelas de alto volume como **Faturas Emitidas** (`CommercialInvoicesView.java`) não exibiam a coluna de data e não permitiam filtragem por período, obrigando os operadores a percorrer páginas para localizar vendas passadas.

---

## 2. Princípios de Arquitetura e Ergonomia

1. **Direção Temporal Retrospetiva:**
   O filtro transacional olha exclusivamente para o **passado** (emissão, criação, registo). Datas futuras ou pós-datadas são sumariamente excluídas dos intervalos passados.
2. **Independência de Fuso Horário e Parse Resiliente:**
   A extração da data da célula lê os primeiros 10 caracteres (`dd/MM/yyyy`), tolerando sufixos com horas (`dd/MM/yyyy HH:mm`) ou separadores.
3. **Não Bloqueio e Filtro Instantâneo:**
   A filtragem é executada no cliente através de `RowFilter` sobre o `TableRowSorter`, atualizando a tabela instantaneamente à medida que o utilizador escolhe uma opção no combo.
4. **Respeito aos Limites de Linhas de Código:**
   Os painéis prioritários continuam estritamente abaixo de **1000 linhas**, em conformidade com `UiPanelDecompositionTest`.

---

## 3. Vocabulário Padronizado do Filtro

O combo padrão (`TableFilter.periodCombo()`) disponibiliza as seguintes opções na ordem indicada:

1. `Todo o período` — Sem qualquer restrição de data.
2. `Hoje` — Data igual a `today`.
3. `Ontem` — Data igual a `today - 1 dia`.
4. `Esta semana` — Da segunda-feira da semana corrente até `today`.
5. `Últimos 7 dias` — De `today - 6 dias` até `today`.
6. `Este mês` — Mês e ano iguais ao mês e ano de `today`.
7. `Últimos 30 dias` — De `today - 29 dias` até `today`.
8. `Este ano` — Ano civil igual ao de `today` até `today`.

---

## 4. Tabelas Integradas

| Módulo / Vista | Tabela | Coluna de Data | Formato Exibido | Filtro Instalado |
|---|---|---|---|---|
| **Comercial / Faturas** (`CommercialInvoicesView`) | `invoicesTable` | Coluna 3 (`Data`) | `dd/MM/yyyy HH:mm` | `PeriodFilter(invPeriodo, 3)` |
| **Compras / Faturas de Compra** (`ComprasPanel`) | `purchasesTable` | Coluna 5 (`Data`) | `dd/MM/yyyy` | `PeriodFilter(histPeriodo, 5)` |
| **Inventário / Quebras** (`StockWastePanel`) | `wasteTable` | Coluna 1 (`Data`) | `dd/MM/yyyy HH:mm` | Integrado via `periodFilterCombo` |

---

## 5. Regras de Negócio e Casos de Fronteira

- **UFP-BIZ-01 (Sem data ou data ilegível):**
  Quando uma opção diferente de `"Todo o período"` estiver selecionada, células sem data válida ou com valor nulo são excluídas da exibição.
- **UFP-BIZ-02 (Semana Civil):**
  A opção `"Esta semana"` inicia impreterivelmente na segunda-feira imediatamente anterior (ou no próprio dia se hoje for segunda-feira). Documentos de domingo anterior **não** pertencem a `"Esta semana"`.
- **UFP-BIZ-03 (Ano Civil):**
  A opção `"Este ano"` restringe ao ano corrente, excluindo documentos com ano anterior mesmo que emitidos há menos de 365 dias.
