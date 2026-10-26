# Especificação Técnica de Decomposição: KpiCard (Fase 92)

## 1. Contexto e Diagnóstico

A classe de componente `KpiCard.java` define o padrão canónico de cartões de indicadores de desempenho do ERP Multicore, garantindo a altura uniforme universal de 96px (`KpiCard.STANDARD_CARD_HEIGHT = 96`), renderização em grelha semântica e paletas de cores coordenadas sólidas sem gradientes espúrios.
Antes da Fase 92, acumulava **579 linhas**, misturando num só componente:
1. Resolução espectral de cores e paletas temáticas (`KpiPalette`, `resolvePalette`, `bluePalette`, `emeraldPalette`, etc.).
2. Componente de micro-pill de tendência reativa (`TrendBadge`) com cálculo de sinal e pintura com anti-aliasing.
3. Tratamento de eventos de rato/teclado para interações de drilldown (`makeInteractive`, listeners de hover e atalhos Enter/Espaço).
4. Fábricas de criação de cartões métricos e grelhas de layout (`createGrid`, `create`, `createCard`, `createMetricCard`).

## 2. Objectivos da Refacção (Fase 92)

- Reduzir `KpiCard.java` para $\le 250$ linhas (redução de mais de 55%).
- Extrair `KpiCardPalette.java` ($\le 180$ linhas) para definição e resolução espectral de paletas coordenadas (`KpiPalette`).
- Extrair `TrendBadge.java` ($\le 115$ linhas) para componente independente de pílula de variação percentual.
- Extrair `KpiCardInteractiveHelper.java` ($\le 50$ linhas) para gestão de foco, hover e cliques de drilldown.
- Preservar 100% de compatibilidade retrocompatível: `KpiCard.TrendBadge`, `KpiCard.KpiPalette`, métodos estáticos de paleta e fábricas de cartões.
- Garantir zero chamadas a `JOptionPane`.

## 3. Arquitectura dos Componentes Decompostos

```
mz.multicore.erp.gui.components
 ├── KpiCard.java                   (Fábrica canónica e orquestrador de cartões <= 250 linhas)
 ├── KpiCardPalette.java            (Definição e mapeamento espectral HSB <= 180 linhas)
 ├── TrendBadge.java                (Componente visual de tendência percentual <= 115 linhas)
 └── KpiCardInteractiveHelper.java  (Tratamento de interacções, hover e drilldown <= 50 linhas)
```

## 4. Garantia de Conformidade

- **Limites Físicos:** Todos os ficheiros respeitam estritamente seus tetos.
- **Zero Regressões:** Testes unitários de uniformidade e harnesses de KPI passam a 100%.
