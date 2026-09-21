# Matriz de Testes: Seletor Universal de Calendário (`HARNESS-UDP-001`)

## 1. Visão Geral
Esta matriz de testes define a conformidade automatizada da experiência do utilizador e integridade funcional do seletor universal de calendário (`ModernCalendarPopup` e `DateField`).

---

## 2. Casos de Teste Canónicos (UDP-01 a UDP-06)

| ID | Designação | Componente / Alvo | Comportamento Esperado |
|---|---|---|---|
| **UDP-01** | Trailing Calendar Button | `DateField` | O campo possui botão anexado via `"JTextField.trailingComponent"` com ícone `fas-calendar-alt` e cursor de mão. |
| **UDP-02** | Inicialização do Popup | `ModernCalendarPopup` | Ao abrir, o popup inicializa centrado na data do campo (ou hoje se vazio) sem exceções AWT. |
| **UDP-03** | Seleção de Dia na Grelha | `ModernCalendarPopup` | Ao acionar um dia do mês, o campo alvo tem o seu texto atualizado estritamente no padrão ISO (`yyyy-MM-dd`) e o popup fecha. |
| **UDP-04** | Ação Rápida "Hoje" | `ModernCalendarPopup` | O botão "Hoje" preenche a data atual (`LocalDate.now().toString()`) e fecha o popup. |
| **UDP-05** | Ação Rápida "Limpar" | `ModernCalendarPopup` | O botão "Limpar" define o texto como vazio e fecha o popup. |
| **UDP-06** | Retrocompatibilidade ISO | `DateField.value()` | Mantém recusa estrita de formatos inválidos e parsing exato de strings ISO válidas. |
