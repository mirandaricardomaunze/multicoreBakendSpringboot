# Especificação Canónica: Central de Auditoria Forense & Controlo de Fraude Interna

- **Identificador do Documento:** `SPEC-AFF-001`
- **Versão:** 1.0.0
- **Módulos Afetados:** `contracts`, `backend`, `desktop`
- **Data:** 2026-09-17

---

## 1. Contexto e Justificação

A prevenção e deteção de fraudes operacionais e desvios de conformidade interna é um requisito imperativo em ambientes empresariais modernos em Moçambique. Desvios comuns que drenam a rentabilidade e colocam a empresa em risco fiscal incluem:
1. **Cancelamento indevido de faturas/recibos** após cobrança em numerário no balcão ou comercial para apropriação indevida de valores.
2. **Concessão abusiva de descontos manuais** acima das alçadas autorizadas.
3. **Registo excessivo ou desproporcional de quebras e perdas de inventário** sem justificativa técnica ou aprovação superior.
4. **Manipulação de limites de crédito** para contornar bloqueios a clientes inadimplentes.
5. **Estornos e cancelamentos financeiros** sem rastreabilidade documental.

A Central de Auditoria Forense agrega estes eventos críticos num motor analítico de 360°, classificando o risco por severidade, quantificando a exposição financeira em Meticais e permitindo a emissão de relatórios de auditoria canónicos.

---

## 2. Princípios e Regras de Negócio

### Regra AFF-01: Classificação de Severidade de Risco
Cada anomalia detetada é classificada num de três níveis:
- **`CRITICAL` (Crítico):**
  - Cancelamento de fatura com valor $> 5.000,00$ MT ou sem motivo expresso.
  - Quebra de stock com custo total $> 10.000,00$ MT ou motivo de furto/roubo sem termo policial.
  - Anulação financeira de recebimento confirmado $> 5.000,00$ MT.
- **`SUSPICIOUS` (Suspeito):**
  - Cancelamento de qualquer documento fiscal com valor $\le 5.000,00$ MT.
  - Desconto comercial ou POS manual superior a 10% do subtotal.
  - Quebra de stock com custo total entre $3.000,00$ e $10.000,00$ MT.
- **`INFO` (Informativo / Log de Auditoria):**
  - Ajustes de inventário de rotina, sessões de caixa abertas e fechadas fora do horário habitual, logs de auditoria administrativa.

### Regra AFF-02: Quantificação da Exposição Financeira
A exposição de risco financeiro ($R_{\text{total}}$) é calculada como:
$$R_{\text{total}} = \sum \text{Impacto}(A_i), \quad \forall A_i \in \{\text{Anomalias com severidade CRITICAL ou SUSPICIOUS}\}$$

### Regra AFF-03: Cálculo do Índice de Conformidade (Compliance Score)
O índice de conformidade global da empresa no período avaliado é apurado com base no volume de transações normais vs anomalias graves:
- Se $R_{\text{total}} = 0 \implies \text{Score} = 100\%$ ("Excelente / Sem Desvios")
- Se $R_{\text{total}} \le 10.000,00 \text{ MT} \implies \text{Score} \ge 85\%$ ("Bom / Baixo Risco")
- Se $10.000 < R_{\text{total}} \le 50.000,00 \text{ MT} \implies \text{Score} \in [60\%, 84\%]$ ("Atenção / Risco Moderado")
- Se $R_{\text{total}} > 50.000,00 \text{ MT} \implies \text{Score} < 60\%$ ("Crítico / Exposição Elevada")

### Regra AFF-04: Respeito ao Multi-Tenant e Isolamento
Todas as análises forenses são estritamente delimitadas pela empresa ativa (`companyId`) obtida via `CurrentUserContext`.

### Regra AFF-05: Emissão do Dossiê de Auditoria Forense em PDF
O relatório canónico de auditoria é gerado em folha A4 com:
1. Cabeçalho canónico com dados fiscais da empresa (`CompanyHeaderRenderer`).
2. Cartões KPI de Risco (Exposição Financeira Total, Ocorrências Críticas, Ocorrências Suspeitas, Índice de Conformidade).
3. Matriz discriminada de anomalias com data/hora, operador, categoria, referência documental, impacto monetário e justificação.
4. Termo de encerramento de auditoria com parecer e espaço para assinatura do Auditor Interno e Direção Executiva.

---

## 3. Contratos REST e DTOs

### Endpoints
- `GET /api/audit/forensic` - Retorna o sumário executivo com anomalias filtradas (`startDate`, `endDate`, `severity`, `category`, `operator`).
- `GET /api/audit/forensic/pdf` - Gera o Dossiê de Auditoria Forense oficial em formato PDF A4.
