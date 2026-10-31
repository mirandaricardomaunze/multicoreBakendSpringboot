# Harness de Decomposição do CollectionRadarService

`CollectionRadarServiceDecompositionHarnessTest` assegura que a análise de faixas (`CollectionRadarAnalyzer`)
e a geração de lembretes multicanal (`CollectionReminderTemplateBuilder`) permanecem fora da fachada
`CollectionRadarService`, respeitando os limites físicos de linhas.
