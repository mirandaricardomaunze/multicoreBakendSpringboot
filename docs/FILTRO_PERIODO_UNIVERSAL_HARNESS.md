# Harness de Conformidade — Filtro Universal por Período em Tabelas Transacionais

**Código:** `FILTRO_PERIODO_UNIVERSAL_HARNESS`  
**Documento de Referência:** `docs/FILTRO_PERIODO_UNIVERSAL_SPEC.md`  
**Suite de Testes:** `UniversalPeriodFilterHarnessTest.java`  

---

## 1. Matriz de Critérios de Conformidade

| ID | Cenário / Regra | Comportamento Esperado | Cobertura Automatizada |
|----|-----------------|------------------------|------------------------|
| **UFP-01** | Opção `Hoje` | Retorna verdadeiro se e só se a data for igual à data de referência `today`. | `testTodaySelection` |
| **UFP-02** | Opção `Ontem` | Retorna verdadeiro para `today - 1 dia`; falso para `today` e datas mais antigas. | `testYesterdaySelection` |
| **UFP-03** | Opção `Esta semana` | Inclui da 2ª feira da semana corrente até `today`; exclui domingo anterior e datas futuras. | `testThisWeekSelection` |
| **UFP-04** | Opção `Últimos 7 dias` | Inclui `today - 6 dias` até `today`; exclui `today - 7 dias`. | `testLastSevenDays` |
| **UFP-05** | Opção `Este mês` | Restringe ao mês e ano correntes (1.º dia do mês passa; último dia do mês anterior não passa). | `testThisMonth` |
| **UFP-06** | Opção `Este ano` | Restringe a 1 de Janeiro do ano corrente até `today`; 31 de Dezembro do ano anterior é excluído. | `testThisYear` |
| **UFP-07** | Opção `Todo o período` | Devolve verdadeiro para qualquer data válida ou até data nula. | `testAllPeriod` |
| **UFP-08** | Integração em Faturas (`CommercialInvoicesView`) | A tabela de faturas possui a coluna `Data` instalada e ligada a `PeriodFilter`. | `testInvoicesTableHasDateColumnAndFilter` |

---

## 2. Execução Automatizada

```powershell
mvn test -pl desktop "-Dtest=UniversalPeriodFilterHarnessTest,PeriodFilterVocabularyTest"
```
