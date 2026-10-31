# Especificação de Decomposição: AssetService (Fase 154)

## 1. Diagnóstico do Monólito
`AssetService.java` possuía **229 linhas**, misturando:
- Registo, listagem, consulta e mapeamento DTO de activos imobilizados (`createAsset`, `listAssets`, `getAsset`, `toDTO`).
- Processamento mensal em lote de depreciações com emissão de `AssetDepreciatedEvent` e consulta de histórico (`runMonthlyDepreciation`, `getDepreciationHistory`).

## 2. Arquitectura Alvo
1. `AssetService` (`<= 80` linhas): Fachada `@Service` e `@Transactional`, preservando os construtores retrocompatíveis de 4 e 5 argumentos.
2. `AssetLifecycleService` (`<= 125` linhas): Componente `@Component` dedicado ao registo, validação de código/valor residual, consulta e mapeamento DTO de activos.
3. `AssetDepreciationBatchService` (`<= 125` linhas): Componente `@Component` dedicado ao processamento mensal de depreciações, emissão de eventos e histórico.
