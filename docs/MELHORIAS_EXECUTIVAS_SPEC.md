# Especificação Técnica — Pacote de Excelência Executiva e Operacional

Este documento define a especificação técnica canónica dos 6 módulos executivos do Multicore ERP:
1. **Centro de Alertas Inteligentes & Ações Proativas (`SmartAlertsCenter` / `SmartAlertsDialog`)**
2. **Motor de Impressão Direta & Silenciosa no POS (`PosDirectPrintEngine`)**
3. **Widget de Rentabilidade, Margens e DRE no Dashboard (`ProfitAnalyticsWidget`)**
4. **Calculadora e Pagamentos Multimoeda no Balcão (`MultiCurrencyEngine` / `CurrencyExchangeDialog`)**
5. **Programa de Fidelização de Clientes & Resgate de Pontos (`LoyaltyEngine` / `LoyaltyBadge`)**
6. **Centro de Cópias de Segurança e Integridade (`DatabaseBackupDialog`)**

---

## 1. Centro de Alertas Inteligentes & Ações Proativas (`SmartAlertsDialog`)

### 1.1 Objetivo
Permitir que o gestor e os operadores identifiquem imediatamente situações críticas da operação agrupadas por urgência (Crítico, Atenção, Operacional) e executem ações imediatas com 1 clique (como criar encomenda a fornecedor para produtos em rutura ou consultar dados de cobrança de clientes com faturas em atraso).

### 1.2 Classificação de Criticidade
- **🔴 CRÍTICO:**
  - Stock com quantidade inferior ao stock de segurança / mínimo (`qty < minStock`).
  - Lotes de produtos já vencidos (`expirationDate < hoje`).
  - Faturas emitidas vencidas há mais de 30 dias.
- **🟡 ATENÇÃO:**
  - Lotes com validade a vencer nos próximos 30 dias.
  - Pedidos de aprovação documental pendentes.
  - Exames de saúde ocupacional ou documentos de colaboradores a expirar.
- **🟢 INFORMATIVO:**
  - Lembretes de obrigações de processamento ou avisos de subscrição.

### 1.3 Design e Interação
- Diálogo moderno centralizado com suporte a temas Claro/Escuro.
- Filtro rápido por botões segmentados: `Todos`, `Críticos (🔴)`, `Atenção (🟡)`.
- Cada cartão exibe: Ícone, Título, Detalhe, Badge de Urgência e Botão de Ação Direta (`ModernButton`) que aciona a navegação ou o diálogo correspondente.

---

## 2. Motor de Impressão Direta no POS (`PosDirectPrintEngine`)

### 2.1 Objetivo
Permitir aos operadores de balcão de alta cadência imprimir o talão de venda em impressoras térmicas (80mm ou 58mm) instantaneamente no fecho da venda, sem necessidade de confirmação visual no modal de pré-visualização.

### 2.2 Requisitos
- **Alternador de Modo:** Checkbox/Switch `[✓ Impressão Rápida Direta]` no POS.
- **Execução:** Ao concluir o checkout com sucesso, se a opção estiver ativa, invoca `PdfPrinter.printSilent(pdfBytes, "Talao-POS-...")` no formato térmico.
- **Feedback:** Notificação em toast não-bloqueante (`ToastManager.showToast`) informando o envio para a impressora.

---

## 3. Widget de Rentabilidade, Margens e DRE no Dashboard (`ProfitAnalyticsWidget`)

### 3.1 Objetivo
Apresentar ao gestor a rentabilidade real do negócio no período selecionado, detalhando a receita líquida, o custo das mercadorias vendidas (CMVMC), o lucro bruto absoluto e a margem percentual.

### 3.2 Fórmulas
- $\text{Receita Total} = \sum \text{Faturas Aprovadas} + \sum \text{Vendas POS}$
- $\text{CMVMC Estimado} = \sum (\text{Quantidade Vendida} \times \text{Preço de Custo / Compra})$
- $\text{Lucro Bruto} = \text{Receita Total} - \text{CMVMC}$
- $\text{Margem Bruta (\%)} = \frac{\text{Lucro Bruto}}{\text{Receita Total}} \times 100$
- $\text{Ticket Médio} = \frac{\text{Receita Total}}{\text{Total de Transações}}$

### 3.3 Visualização
- Cartão executivo com 3 colunas de indicadores-chave (Receita, CMVMC, Lucro Bruto & Margem).
- Mini-gráfico de barras horizontais comparando margens dos canais de venda (POS vs Faturação direta).

---

## 4. Pagamentos Multimoeda & Câmbio no POS (`MultiCurrencyEngine`)

### 4.1 Objetivo
Permitir que clientes paguem em Moeda Estrangeira (USD, ZAR, EUR) no checkout do POS com cálculo instantâneo da conversão oficial para Meticais (`MZN`) e determinação exata do troco em Meticais.

### 4.2 Moedas e Taxas Canónicas
- **MZN (Metical Moçambicano):** Moeda Base (1.00).
- **USD (Dólar Americano):** Taxa de referência 63.83 MZN.
- **ZAR (Rand Sul-Africano):** Taxa de referência 3.65 MZN.
- **EUR (Euro):** Taxa de referência 69.50 MZN.

### 4.3 Algoritmo de Conversão
- $\text{Total em Moeda Estrangeira} = \frac{\text{Total MZN}}{\text{Taxa Câmbio}}$
- $\text{Valor Recebido em MZN} = \text{Valor Recebido ME} \times \text{Taxa Câmbio}$
- $\text{Troco a Devolver em MZN} = \text{Valor Recebido em MZN} - \text{Total MZN}$

---

## 5. Programa de Fidelização de Clientes (`LoyaltyEngine`)

### 5.1 Objetivo
Incentivar a fidelização de clientes no retalho moçambicano através da acumulação de pontos em compras e desconto direto no checkout do POS.

### 5.2 Regras de Negócio
- **Geração:** 1 Ponto por cada 100,00 MT em compras finalizadas.
- **Valor do Ponto:** 1 Ponto = 1,00 MT de desconto na próxima compra.
- **Resgate:** O operador pode abater os pontos disponíveis até ao valor total da compra.
- **Apresentação:** Indicador visual de estrelas/pontos com badge cromático ao selecionar o cliente no POS.

---

## 6. Centro de Cópias de Segurança e Integridade (`DatabaseBackupDialog`)

### 6.1 Objetivo
Proporcionar uma interface executiva e intuitiva para o Administrador gerar backups lógicos da base de dados com 1 clique, consultar ficheiros existentes e validar a integridade estrutural (tamanho e hash).

### 6.2 Funcionalidades
- **Geração Imediata:** Acionamento assíncrono com barra de progresso indeterminada.
- **Lista de Cópias:** Listagem dos ficheiros `.dump` / `.sql` gerados no servidor.
- **Verificação de Integridade:** Validação de cabeçalho, tamanho e SHA-256 do arquivo.
