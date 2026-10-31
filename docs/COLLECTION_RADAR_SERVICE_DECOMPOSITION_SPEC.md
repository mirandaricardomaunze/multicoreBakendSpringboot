# Especificação Técnica: Decomposição do CollectionRadarService (Fase 136)

## Diagnóstico

`CollectionRadarService` (259 linhas) misturava o cálculo de faixas preventivas e de mora sobre facturas
cobradas parcialmente/em aberto com a composição de mensagens multicanal (SMS, WhatsApp e E-mail) em três
horizontes temporais.

## Decisão

- `CollectionRadarService`: fachada tenant-scoped que resolve `companyId` e `referenceDate`, delega aos colaboradores e preserva o construtor legado `(InvoiceRepository)`.
- `CollectionRadarAnalyzer`: consulta facturas cobráveis (`InvoiceStatus.collectableStatuses()`), classifica-as em `CollectionRadarBucket` e produz `CollectionRadarSummaryDTO` e a carteira ordenada `CollectionRadarItemDTO`.
- `CollectionReminderTemplateBuilder`: geração dos templates de cobrança preventiva, vencimento no dia e regularização em atraso para SMS, WhatsApp e E-mail.
