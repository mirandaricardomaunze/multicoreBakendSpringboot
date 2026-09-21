# Suíte de Experiência Executiva do UI — Matriz de Testes (HARNESS)

## 1. Escopo de Validação
Esta matriz define os critérios de teste para garantir a robustez, acessibilidade e correto comportamento dos componentes visuais executivos desenvolvidos.

## 2. Casos de Teste de Conformidade

| Código | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **EUI-01** | `SimpleBarChart` renderiza barras com gradiente suave e anti-aliasing | `chart.getPreferredSize()` respeitado, sem lançar excepções e `Graphics2D` configurado com `RenderingHints`. |
| **EUI-02** | `SimpleBarChart` tooltip interativo de valores | Mover coordenadas do rato sobre a barra retorna o tooltip com o rótulo e valor formatado em MT. |
| **EUI-03** | `SimplePieChart` suporte a modo Donut e detecção de fatias | Configurar modo Donut e dados válidos gera fatias com percentagens internas calculadas. |
| **EUI-04** | `TrendBadge` estilização de crescimento positivo | Variação percentual positiva (ex.: `+8.5%`) produz badge com texto verde `APPROVED_GREEN` e seta `▲`. |
| **EUI-05** | `TrendBadge` estilização de queda negativa | Variação percentual negativa (ex.: `-3.2%`) produz badge com texto vermelho `REJECTED_RED` e seta `▼`. |
| **EUI-06** | `KpiCard` integração do `TrendBadge` | `createMetricCard` com indicador de tendência acomoda o badge verticalmente ao lado do valor sem quebra de layout. |
| **EUI-07** | `TopNavBar` botão e atalho de pesquisa global | Componente `TopNavBar` expõe o gatilho de pesquisa rápida e atalho `Ctrl+K`. |
| **EUI-08** | `ModernFormDialog` backdrop dimmer | Instalação do overlay translúcido no `glassPane` da janela pai e limpeza absoluta ao fechar. |
