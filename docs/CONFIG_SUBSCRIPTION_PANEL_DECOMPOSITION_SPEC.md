# Especificação Técnica — Decomposição Modular de ConfigSubscriptionPanel (Fase 65)

## 1. Contexto e Diagnóstico

O painel `ConfigSubscriptionPanel.java` (129 linhas) integra a aba *"A Minha Assinatura"* em `ConfigPanel.java`. Suas responsabilidades misturavam:
1. **Coordenação de Ciclo de Vida e Ações de Subscrição:** Montagem do cabeçalho com botões "Renovar / Activar Plano" (disparando `SubscriptionRenewalDialog.show(...)`) e "Actualizar" (`loadMySubscription()`).
2. **Formatação e Renderização Visual do Cartão de Assinatura:** Regras de apresentação para os 6 campos canónicos de subscrição (`Empresa`, `Plano`, `Estado`, `Válida até`, `Dias restantes`, `Mensalidade`), lógica de cores condicionais para dias restantes e estados de licença, tratamento de estado vazio (`!sub.hasSubscription()`) e exibição de falhas de carregamento assíncrono.

## 2. Objectivos da Decomposição

1. **Extração de `ConfigSubscriptionCardRenderer.java`:**
   - Componente puro de renderização que manipula a superfície `subscriptionCard` (`ModernPanel(16)`).
   - Renderização dos estados:
     - Dados válidos (`renderSubscription(JPanel card, MySubscriptionDTO sub)`).
     - Sem subscrição activa / estado vazio (`renderEmpty(JPanel card)`).
     - Falha de comunicação assíncrona (`renderError(JPanel card, String message)`).
   - Cálculos e paletas semânticas: `daysText(Long)`, `daysColor(Long)`, `statusColor(String)`.
   - Limite físico: $\le 95$ linhas.

2. **Refatoração e Compactação de `ConfigSubscriptionPanel.java`:**
   - Reduzir de 129 linhas para $\le 75$ linhas (~60 linhas).
   - Foco estrito em ciclo de vida de UI (`buildPanel()`, `refresh()`, `loadMySubscription()`).
   - Zero `JOptionPane` — manter feedback não intrusivo.

3. **Invariantes e Contratos Preservados:**
   - Construtor `ConfigSubscriptionPanel(ConfigPanel owner)`.
   - Métodos públicos/de pacote: `buildPanel()`, `refresh()`, `loadMySubscription()`.
   - Acionamento canónico de `SubscriptionRenewalDialog.show(owner, owner.mySubscriptionApiClient, ...)`.
   - Compatibilidade total com `ConfigPanel.java` e `ConfigPanelDecompositionHarnessTest`.
