# SPEC — Eliminação de Botões Escuros e Padronização Cromática em Todo o Sistema

**Criado em:** 2026-10-04  
**Camada:** Cliente desktop Swing (`mz.multicore.erp.gui`, `mz.multicore.erp.gui.components`)  
**Backend:** Inalterado (regras de apresentação no desktop).

---

## 1. Objectivo e Diagnóstico

### 1.1 Diagnóstico do Problema
Historicamente, o `UIHelper` definia as cores de botões secundários e neutros com tonalidades cinzentas escuras e ardósia pesada:
- `SECONDARY = new Color(75, 85, 99)` (Gray-600 / `#4B5563`)
- `BUTTON_NEUTRAL = new Color(51, 65, 85)` (Slate-700 / `#334155`)

Como resultado:
1. Qualquer chamada a `UIHelper.createSecondaryButton(...)` ou `UIHelper.createRefreshButton(...)` gerava botões escuros/cinzentos opacos.
2. Botões de cancelamento ou fecho em modais utilizavam estilos neutros escuros em vez do botão de perigo/saída padronizado.
3. No POS e nas listagens de Stock, Compras, Finanças e RH, botões de paginação, selecção e acções secundárias ficavam escurecidos e desarmónicos com o restante design moderno e vibrante do ERP.

### 1.2 Princípios de Desenho e Solução
1. **Zero Botões Escuros/Cinzentos:** Nenhum botão operacional ou secundário na aplicação deve apresentar fundo cinzento escuro ou ardósia preta (`Gray-600` ou `Slate-700`).
2. **Cores Semânticas e Vivas:**
   - **Botões Secundários (`createSecondaryButton`):** Assumem tonalidade azul petróleo oceânico luminoso (`Sky-700` — `#0369A1`), garantindo que todos os botões secundários herdados fiquem claros, visíveis e atraentes.
   - **Botões Neutros (`BUTTON_NEUTRAL`):** Assumem tonalidade índigo vibrante (`Indigo-500` — `#6366F1`), com excelente contraste e legibilidade.
   - **Botões de Recarga ("Actualizar"):** Utilizam `SECONDARY` (`Sky-700`) com ícone branco `fas-sync-alt`, com luminosidade instantânea no topo de todas as tabelas.
   - **Botões de Cancelar / Fechar:** Botões de anulação, descarte ou cancelamento de formulários devem usar rigorosamente o botão vermelho canónico (`createDangerButton` — `REJECTED_RED` `#EF4444`).
   - **Texto e Ícones:** Todo o texto e ícones sobre botões coloridos usam `Color.WHITE`, em conformidade estrita com o padrão WCAG 2.1 AA.

---

## 2. Especificação Canónica no `UIHelper`

```java
// Em UIHelper.java
public static final Color BUTTON_NEUTRAL = new Color(99, 102, 241);       // Indigo-500 (#6366F1)
public static final Color BUTTON_NEUTRAL_HOVER = new Color(79, 70, 229); // Indigo-600 (#4F46E5)
public static final Color SECONDARY = new Color(3, 105, 161);            // Sky-700 (#0369A1)
public static final Color SECONDARY_HOVER = new Color(7, 89, 133);       // Sky-800 (#075985)

public static ModernButton createSecondaryButton(String text) {
    return new ModernButton(text, SECONDARY, SECONDARY_HOVER);
}

public static ModernButton createRefreshButton(Runnable refreshAction) {
    Objects.requireNonNull(refreshAction, "A acção de actualização é obrigatória.");
    ModernButton button = new ModernButton("Actualizar", SECONDARY, SECONDARY_HOVER);
    button.setIcon(icon("fas-sync-alt", 14, Color.WHITE));
    button.setForeground(Color.WHITE);
    button.setToolTipText("Carregar os dados mais recentes");
    button.addActionListener(event -> refreshAction.run());
    return button;
}
```

---

## 3. Matriz de Componentes Revitalizados

| Componente / Módulo | Botão Original | Cor Nova Canónica | Papel Semântico |
|---------------------|----------------|-------------------|-----------------|
| `UIHelper.SECONDARY` | `#4B5563` (Dark Gray) | `ACCENT_SKY` (`#0EA5E9`) | Acções secundárias luminosas |
| `UIHelper.BUTTON_NEUTRAL` | `#334155` (Dark Slate) | `Indigo-500` (`#6366F1`) | Controlos alternativos e chips |
| `UIHelper.createRefreshButton` | Fundo cinzento escuro | `ACCENT_SKY` c/ ícone branco | Actualização de dados em tabelas |
| Modais de Diálogo (Cancel/Fechar) | Fundo cinzento escuro | `createDangerButton` (`#EF4444`) | Saída segura sem persistência |
| Catálogo POS (Paginação) | Slate-700 escuro | `ACCENT_BLUE` (`#3B82F6`) | Navegação de páginas de artigos |
| POS Cédulas Rápidas | Fundo cinzento escuro | `APPROVED_GREEN` (`#10B981`) | Notas em Meticais no balcão |
| POS Cotação & Fidelidade | Slate-700 escuro | `ACCENT_CYAN` & `ACCENT` | Importação e Fidelização |
