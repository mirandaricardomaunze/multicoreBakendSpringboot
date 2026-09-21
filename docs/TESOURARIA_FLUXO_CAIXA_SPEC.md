# Especificação Canónica: Tesouraria & Projeção de Fluxo de Caixa Previsional (Cash Flow Forecast)

- **Identificador do Documento:** `SPEC-TFC-001`
- **Versão:** 1.0.0
- **Módulos Afetados:** `contracts`, `backend`, `desktop`
- **Data:** 2026-09-17

---

## 1. Contexto e Justificação

A gestão proativa da tesouraria é crucial para a saúde financeira de qualquer empresa em Moçambique. Enquanto o histórico de movimentos contabiliza transações já consumadas, a administração necessita de antever com precisão o saldo projetado de caixa a curto e médio prazo para:
1. Identificar com antecedência períodos de rutura de liquidez (*Cash Gap / Défice de Caixa*).
2. Antecipar cobranças a clientes com faturas a vencer ou já vencidas.
3. Planear pagamentos a fornecedores e evitar encargos por atraso ou corte de fornecimento.
4. Apresentar relatórios executivos de liquidez a auditores, conselhos de administração e instituições bancárias.

---

## 2. Princípios e Regras de Negócio

### Regra TFC-01: Posição Inicial de Caixa ($C_0$)
A posição de tesouraria imediata é a soma dos saldos em moeda corrente (Meticais) de todas as contas ativas da empresa (`companyId`), classificadas por tipo:
$$C_0 = \sum \text{Saldo}(\text{Caixas}) + \sum \text{Saldo}(\text{Bancos})$$

### Regra TFC-02: Entradas Previsionais ($I$)
As entradas correspondem a faturas a clientes emitidas e aprovadas (`status == APPROVED`), com saldo pendente (`outstandingAmount() > 0`), alocadas pela sua data de vencimento efetiva (`effectiveDueDate()`):
- **Vencido (Overdue):** $\text{effectiveDueDate} < \text{hoje}$
- **Hoje (Today):** $\text{effectiveDueDate} = \text{hoje}$
- **1 a 7 dias:** $\text{hoje} < \text{effectiveDueDate} \le \text{hoje} + 7$
- **8 a 15 dias:** $\text{hoje} + 7 < \text{effectiveDueDate} \le \text{hoje} + 15$
- **16 a 30 dias:** $\text{hoje} + 15 < \text{effectiveDueDate} \le \text{hoje} + 30$
- **31 a 60 dias:** $\text{hoje} + 30 < \text{effectiveDueDate} \le \text{hoje} + 60$
- **Mais de 60 dias:** $\text{effectiveDueDate} > \text{hoje} + 60$

### Regra TFC-03: Saídas Previsionais ($O$)
As saídas correspondem a compras a fornecedores ativas (`status == COMPLETED`), com saldo pendente (`getOutstanding() > 0`). A data de vencimento considerada é a data estimada de liquidação acordada com o fornecedor (30 dias a partir da compra quando não especificada expressamente):
- Mapeamento nos mesmos intervalos temporais canónicos.

### Regra TFC-04: Saldo Líquido e Projeção Progressiva ($C_t$)
Para cada balde temporal $t$:
$$\Delta C_t = I_t - O_t$$
$$C_t = C_{t-1} + \Delta C_t$$

### Regra TFC-05: Deteção de Rutura de Caixa (Cash Gap Alert)
Se em qualquer ponto da curva de projeção $C_t < 0$, o sistema emite um alerta de risco de liquidez:
- **Estado:** `CRITICAL` se o saldo projetado for negativo em $\le 15$ dias; `WARNING` se ocorrer entre 16 e 60 dias; `HEALTHY` se $C_t \ge 0$ em todos os horizontes.
- **Défice Máximo Projetado:** Menor valor de $C_t$.
- **Horizonte do Primeiro Défice:** Primeiro balde em que o saldo se torna deficitário.
- **Recomendação Executiva:** Sugestão algorítmica de ação para regularização de tesouraria.

---

## 3. Contratos REST e DTOs

### Endpoints
- `GET /api/finance/forecast` - Devolve a estrutura consolidada de fluxo de caixa previsional para a empresa ativa.
- `GET /api/finance/forecast/pdf` - Gera o relatório executivo oficial em formato PDF A4.

---

## 4. Estrutura do Documento PDF Canónico
O documento é emitido em orientação Paisagem (Landscape) ou Retrato A4 com:
1. Cabeçalho canónico com dados da empresa e NUIT (`CompanyHeaderRenderer`).
2. Cartões de Resumo de Liquidez (Saldo Disponível Atual, Total a Receber, Total a Pagar, Saldo Projetado a 30/60 dias).
3. Matriz temporal por colunas (Vencido, Hoje, 7d, 15d, 30d, 60d, >60d).
4. Listagem discriminada dos 10 maiores recebimentos e pagamentos que impactam a tesouraria.
5. Termo de Responsabilidade e Parecer Financeiro.
