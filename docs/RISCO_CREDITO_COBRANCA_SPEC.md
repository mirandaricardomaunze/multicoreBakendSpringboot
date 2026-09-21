# Especificação Canónica — Centro de Risco de Crédito & Cobrança Formal

**Código:** `RISCO_CREDITO_COBRANCA_SPEC`  
**Módulos:** `contracts`, `backend`, `desktop`  
**Versão:** 1.0  
**Data:** 2026-09-16  

---

## 1. Contexto e Enquadramento

A sustentabilidade financeira de uma empresa assenta na capacidade de conceder crédito comercial com segurança, controlar ativamente os prazos de vencimento e reagir imediatamente a situações de incumprimento.

O **Centro de Risco de Crédito & Cobrança Formal** estabelece:
1. **Matriz de Aging em Tempo Real**: Segmentação multidimensional da dívida por faixas cronológicas de antiguidade.
2. **Classificação Objetiva de Risco (`CreditRiskLevel`)**: Semáforo cromático baseado em dias máximos de atraso e percentagem de utilização do limite concedido.
3. **Mecanismo de Travamento de Vendas a Crédito**: Bloqueio preventivo de clientes em risco crítico ou que tenham excedido a tolerância de crédito.
4. **Cobrança Formal com 1-Clique**: Geração de Notificação/Carta de Cobrança em PDF pronta a enviar por correio ou email.
5. **Tramitação de Exceções por Alçada**: Pedido formal de desbloqueio excecional avaliado pelo módulo de aprovações da administração.

---

## 2. Princípios e Regras de Negócio Inegociáveis

1. **RCC-BIZ-01 (Escalões de Antiguidade / Aging Matrix):**
   Para qualquer data de referência $\text{RefDate}$ (por padrão, o dia de hoje), o saldo devedor de cada fatura emitida é alocado a um dos seguintes baldes:
   - **Corrente**: Faturas não vencidas ($\text{DueDate} \ge \text{RefDate}$).
   - **1 a 30 dias**: Atraso entre 1 e 30 dias.
   - **31 a 60 dias**: Atraso entre 31 e 60 dias.
   - **61 a 90 dias**: Atraso entre 61 e 90 dias.
   - **> 90 dias**: Atraso superior a 90 dias (Crítico / Pré-contencioso).

2. **RCC-BIZ-02 (Classificação de Risco do Cliente):**
   O nível de risco de cada cliente é calculado deterministicamente com base na pior condição:
   - `CRITICAL`: Atraso máximo $> 60$ dias OU cliente expressamente marcado como bloqueado OU dívida total excede o limite em $> 50\%$.
   - `HIGH`: Atraso máximo entre $31$ e $60$ dias OU dívida total ultrapassa o limite de crédito contratado ($> 100\%$).
   - `MEDIUM`: Atraso máximo entre $1$ e $30$ dias OU utilização do limite de crédito $\ge 75\%$.
   - `LOW`: Sem faturas vencidas e utilização do crédito $< 75\%$.

3. **RCC-BIZ-03 (Tolerância e Bloqueio Automático):**
   - Clientes com nível `CRITICAL` ou que estejam sinalizados com `blocked = true` ficam impedidos de emitir novas faturas a prazo no POS ou na Faturação Comercial.
   - Qualquer venda a clientes nesta situação exige um pedido de exceção aprovado pela gerência.

4. **RCC-BIZ-04 (Carta de Cobrança Formal):**
   - A carta de cobrança lista todas as faturas vencidas do cliente, indicando número do documento, data de emissão, data de vencimento, dias de mora e saldo pendente.
   - Apresenta o valor total em dívida, os dados de contacto da empresa emitente e as contas bancárias para regularização.
   - A pré-visualização e impressão utilizam o componente canónico `PrintPreviewDialog`.

5. **RCC-BIZ-05 (Tramitação de Exceções de Crédito):**
   - Em caso de necessidade comercial justificada, o operador pode submeter um `CreditExceptionApprovalRequest` informando o valor adicional pretendido e o motivo.
   - A solicitação gera um `ApprovalRequest` na categoria de crédito comercial, auditado com nome do requerente, data e decisão do gestor.

---

## 3. Interface do Utilizador (`CreditRiskPanel`)

O painel está integrado no módulo de Clientes (`ClientesPanel`) e acessível diretamente via paleta de comandos Spotlight (`Ctrl+K`):
- **Barra de KPIs de Topo:**
  - *Total a Receber* (`kpiTotalReceivable`)
  - *Saldo em Mora* (`kpiTotalOverdue`)
  - *Clientes Bloqueados* (`kpiBlockedCount`)
  - *Risco Elevado / Crítico* (`kpiCriticalCount`)
- **Barra de Filtros:**
  - Seletor de Data de Referência (`dateField`)
  - Filtro por Nível de Risco (`riskFilterCombo`)
  - Pesquisa rápida por Cliente, NUIT ou Email (`searchField`)
  - Botão de atualização assíncrona com barra de progresso não bloqueante (`busyBar`)
- **Tabela de 15 Colunas Operacionais:**
  - ID, Cliente, NUIT, Limite, Dívida Total, Disponível, Corrente, 1-30d, 31-60d, 61-90d, >90d, Total em Mora, Atraso, Nível Risco e Situação.
  - Renderer cromático `CreditRiskCellRenderer` aplicando as cores do Design System (`APPROVED_GREEN`, `PENDING_YELLOW`, `REJECTED_RED`).
