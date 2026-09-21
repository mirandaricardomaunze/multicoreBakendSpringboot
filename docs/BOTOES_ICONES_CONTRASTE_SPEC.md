# SPEC — Contraste e Sincronização Cromática de Botões e Ícones

**Criado em:** 2026-09-13  
**Camada:** cliente desktop Swing (`mz.multicore.erp.gui.components`)  
**Backend:** contratos e regras de negócio permanecem inalterados.

---

## 1. Objectivo

Garantir que todos os botões da aplicação (`ModernButton`, `ActionMenuButton`, botões de formulário, botões de ação e abas segmentadas) apresentem contraste nítido e legível (em conformidade com as diretrizes WCAG 2.1 AA, ratio $\ge 4.5:1$) tanto para os rótulos de texto como para os respetivos ícones, nos temas Claro e Escuro.

---

## 2. Regras Fundamentais de Desenho e Contraste

1. **Botões de Ação com Fundo Colorido ou Escuro**:
   - Botões com fundo primário (`ACCENT_BLUE`), sucesso (`APPROVED_GREEN`), perigo (`REJECTED_RED`), aviso (`PENDING_YELLOW`), secundário (`SECONDARY`) ou neutro (`BUTTON_NEUTRAL`) utilizam obrigatoriamente **texto branco** e **ícone branco**.
   - Proibição de ícones escuros ou pretos sobre fundos de ação.

2. **Sincronização Dinâmica em `ModernButton`**:
   - `ModernButton` é o componente central para botões com ícone.
   - Qualquer atribuição de ícone (`setIcon`) em que o ícone seja um `FontIcon` deve ajustar automaticamente a cor do ícone (`setIconColor`) para coincidir com a cor de texto calculada (`textColor` / `readableTextOn(background)`).
   - Quando um botão comuta de estado (ex.: ativo $\rightarrow$ inativo) através de `setColors(base, hover)`, a cor do texto e a cor do ícone são simultaneamente recalculadas e sincronizadas.
   - Quando o botão recebe fundo claro (ex.: `Theme.LIGHT.card`), tanto o texto como o ícone assumem a cor de texto escuro legível.

3. **Padrão Canónico de Criação de Ícones (`UIHelper.icon`)**:
   - A sobrecarga canónica `UIHelper.icon(code, size)` devolve `Color.WHITE` por defeito, garantindo que qualquer botão ou controlo que instancie um ícone sem fornecer cor explícita receba um ícone branco de alta visibilidade.
   - Componentes que requerem cores contextuais (ex.: abas, títulos, indicadores de status) chamam explicitamente `UIHelper.icon(code, size, color)`.

---

## 3. Especificação da API

```java
// Em ModernButton.java
@Override
public void setIcon(Icon icon) {
    if (icon instanceof org.kordamp.ikonli.swing.FontIcon fi) {
        fi.setIconColor(this.textColor != null ? this.textColor : Color.WHITE);
    }
    super.setIcon(icon);
}

@Override
public void setForeground(Color fg) {
    super.setForeground(fg);
    this.textColor = fg;
    if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
        fi.setIconColor(fg);
    }
}
```
