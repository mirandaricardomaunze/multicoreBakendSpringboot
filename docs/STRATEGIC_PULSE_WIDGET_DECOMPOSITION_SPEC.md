# Especificação de Decomposição: StrategicPulseWidget (Fase 200)

## Objectivo
Decompor o widget executivo de pulso estratégico `StrategicPulseWidget.java` (403 linhas) numa fachada leve que delega a construção visual dos cartões de pilares e cabeçalho a `StrategicPulseCardFactory` e a formatação/aplicação de indicadores a `StrategicPulseDataPresenter`.

## Colaboradores Extraídos
1. `StrategicPulseCardFactory.java`: Construção do cabeçalho e dos 4 cartões de pilares (`buildHeader`, `createPillarCard`).
2. `StrategicPulseDataPresenter.java`: Formatação monetária/percentual e aplicação de cores semânticas e textos dos 4 pilares (`applyForensic`, `applyLiquidity`, `applyCreditRisk`, `applyGoals`).

## Invariantes Preservadas
- Construtor, `loadData()`, `applyData(...)` e todos os getters públicos dos labels e botões de navegação permanecem inalterados.
