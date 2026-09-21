# Matriz de Testes & Harness Canónico: Overflow e Navegação por Setas
**Código:** HARNESS-ASO-001  
**Especificação Associada:** `SPEC-ASO-001`  
**Módulo:** `desktop`  
**Data:** 2026-09-17  
**Estado:** ATIVO  

---

## 1. Matriz de Critérios de Aceitação

| ID | Cenário | Critério de Aceitação | Verificação |
|---|---|---|---|
| **ASO-01** | Largura adaptativa (Tracking Viewport) | Contentor encapsulado no `ArrowScrollPanel` reporta `getScrollableTracksViewportWidth() == true` e a largura interna coincide com a largura da viewport. | Teste unitário em `ArrowScrollPanelTest` |
| **ASO-02** | Omissão da barra nativa | `JScrollPane` configura `VERTICAL_SCROLLBAR_NEVER` e `HORIZONTAL_SCROLLBAR_NEVER`. | `ArrowScrollPanelTest` |
| **ASO-03** | Ação do botão superior [ ▲ ] | Ao clicar no botão de seta superior, a viewport desloca-se para cima (`y` diminui) respeitando o limite mínimo `y = 0`. | `ArrowScrollPanelTest` |
| **ASO-04** | Ação do botão inferior [ ▼ ] | Ao clicar no botão de seta inferior, a viewport desloca-se para baixo (`y` aumenta) respeitando a altura total máxima. | `ArrowScrollPanelTest` |
| **ASO-05** | Desativação no topo | Quando a viewport está em `y = 0`, o botão superior fica desativado (`isEnabled() == false`). | `ArrowScrollPanelTest` |
| **ASO-06** | Desativação no fundo | Quando a viewport atinge o limite inferior, o botão inferior fica desativado (`isEnabled() == false`). | `ArrowScrollPanelTest` |
| **ASO-07** | Decomposição de Linhas | `DashboardPanel.java` $\le 700$ linhas (limite $\le 1000$). | `UiPanelDecompositionTest` |
| **ASO-08** | Uniformidade Visual | Zero literais de cor arbitrários fora de `UIHelper`. | `FinalUiUniformityHarnessTest` |

---

## 2. Comando de Execução Rápida
```bash
mvn test -pl desktop "-Dtest=ArrowScrollPanelTest,UiPanelDecompositionTest,FinalUiUniformityHarnessTest"
```
