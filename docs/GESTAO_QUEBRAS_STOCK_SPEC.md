# Especificação Canónica — Gestão e Aprovação de Quebras de Stock (*Stock Waste & Shrinkage*)

**Código:** `GESTAO_QUEBRAS_STOCK_SPEC`  
**Módulos:** `backend`, `contracts`, `desktop`  
**Versão:** 1.0  
**Data:** 2026-09-15  

---

## 1. Contexto e Enquadramento

A gestão de desperdício, quebras operacionais e avarias em armazém (*shrinkage & waste management*) é um dos pilares de conformidade fiscal e rentabilidade do Multicore ERP.

Anteriormente, ajustes negativos de inventário podiam ser realizados sem rastreabilidade de motivos fiscais, sem discriminação de lotes com risco iminente de validade e sem segregação de funções entre quem constata a perda (operador de armazém) e quem a autoriza com impacto no razão contabilístico (gestor/administrador).

Este módulo estabelece o ciclo de vida auditável:
$$\text{Proposta de Quebra} \longrightarrow \text{Validação de Alçada} \longrightarrow \text{Aprovação/Rejeição} \longrightarrow \text{Abate Físico e Contabilístico}$$

---

## 2. Princípios e Regras de Negócio Inegociáveis

1. **GQS-BIZ-01 (Motivos Legais de Abate):**
   Toda a quebra deve declarar expressamente um dos motivos padronizados em `WasteReason`:
   - `EXPIRED` — Produto fora da data de validade.
   - `DAMAGED` — Avaria, quebra física ou deterioração em loja/armazém.
   - `TRANSPORT` — Sinistro ou perda durante transporte entre armazéns ou receção.
   - `THEFT` — Furto ou desaparecimento confirmado em contagem.
   - `INTERNAL_USE` — Amostras comerciais ou consumo interno da empresa.
   - `OTHER` — Outro motivo justificado com nota explicativa obrigatória.

2. **GQS-BIZ-02 (Custo Histórico Real):**
   O custo unitário de perda (`unitCost`) provém do custo histórico de aquisição do lote (ou do custo médio ponderado do produto na ausência de lote), garantindo que a valorização monetária da perda não seja arbitrada pelo utilizador.

3. **GQS-BIZ-03 (Segregação de Alçadas e Estados):**
   - Um registo nasce como `PENDING` (Pendente de Validação).
   - O botão **Aprovar** só é habilitado para linhas em estado `PENDING`.
   - Após ser aprovada (`APPROVED`), a quebra é final e irreversível; o stock físico é debitado do armazém e é lançado o estorno de CMVMC/Mercadorias.
   - Se rejeitada (`REJECTED`), a perda é anulada e o stock permanece intacto.

4. **GQS-BIZ-04 (Radar de Validades e Prevenção Ativa):**
   O sistema calcula proativamente os lotes em risco de expiração por escalões:
   - *Vencidos* (< 0 dias)
   - *Crítico* (≤ 3 dias)
   - *Alto* (≤ 7 dias)
   - *Médio* (≤ 15 dias)
   - *Atenção* (≤ 30 dias)

5. **GQS-BIZ-05 (Ergonomia e UI Não Bloqueante):**
   - As decisões de aprovação ou rejeição utilizam `ModernFormDialog` com validação síncrona de notas obrigatórias em caso de rejeição, sem lançar `JOptionPane`.
   - Feedback de sucesso é apresentado via `ToastManager`.
   - A tabela suporta filtragem combinada por Estado, Motivo, Período e Pesquisa livre.

---

## 3. Interface do Utilizador (`StockWastePanel`)

O painel está estruturado em 3 abas operacionais dentro de `StockPanel`:
1. **Registo & Validação de Quebras:** Tabela consolidada de histórico, barra de filtros com período universal, botões de ação e modal para lançamento de nova quebra.
2. **Radar de Validades & Prevenção:** Tabela de lotes com risco de vencimento, perda potencial calculada em Meticais e nível de alerta.
3. **Métricas & Relatório Executivo:** Discriminação de custos de quebra por motivo e por categoria de produto, com botão de impressão de relatório em PDF.
