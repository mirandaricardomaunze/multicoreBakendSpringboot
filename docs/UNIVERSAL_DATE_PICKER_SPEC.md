# Especificação Técnica: Seletor Universal de Calendário nos Campos de Data (`SPEC-UDP-001`)

## 1. Visão Geral e Contexto
No sistema desktop Multicore ERP, a introdução de datas em formulários operacionais e analíticos (como admissões em RH, períodos de férias, filtros contabilísticos de balancete, datas de entrega de compras, validade de lotes e pagamentos da plataforma) era realizada predominantemente por digitação manual em formato ISO (`yyyy-MM-dd`).

Esta especificação define o componente visual unificado de calendário interativo embutido no componente canónico [`DateField`](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/DateField.java), acionado por clique em botão trailing no próprio campo ou por atalhos de teclado (`F4`, `Alt+↓`).

---

## 2. Requisitos Arquiteturais e de Negócio

### 2.1 Preservação Estrita dos Contratos ISO (`yyyy-MM-dd`)
- O formato de persistência e intercâmbio de dados com o backend Spring Boot é estritamente ISO-8601 (`yyyy-MM-dd`).
- Qualquer seleção de dia no calendário deve invocar `setDate(LocalDate)` ou `setText(date.toString())`, formatando automaticamente a string como `AAAA-MM-DD`.
- A digitação manual continua plenamente suportada e validada pelo método canónico `value()`.

### 2.2 Componente Trailing FlatLaf
- Em conformidade com o tema FlatLaf 3.7.2, o botão do calendário deve ser anexado como componente embutido no lado direito do campo através da propriedade canónica:
  `putClientProperty("JTextField.trailingComponent", calendarButton)`.
- O botão deve utilizar estilo de barra de ferramentas (`BUTTON_TYPE_TOOLBAR_BUTTON`), sem bordas invasivas, com ícone FontAwesome `fas-calendar-alt` em tamanho adequado (13-14px) e cursor `HAND_CURSOR`.

### 2.3 Pop-up de Calendário (`ModernCalendarPopup`)
O componente de calendário é encapsulado em `JPopupMenu` leve e modal-independente:
1. **Navegação Superior:**
   - Botões `‹` (mês anterior) e `›` (mês seguinte).
   - Dropdown de Mês (`JComboBox<String>`) em língua portuguesa (Janeiro a Dezembro).
   - Seletor de Ano (`JSpinner`) com amplitude padrão de 1920 a 2120.
2. **Grelha Semanal:**
   - 7 colunas alinhadas com os dias da semana em Moçambique: `Seg`, `Ter`, `Qua`, `Qui`, `Sex`, `Sáb`, `Dom` (início na Segunda-feira, `DayOfWeek.MONDAY`).
3. **Matriz Mensal de Dias (6x7):**
   - Dias do mês selecionado visíveis e clicáveis com fonte normal.
   - Dias de transbordo (mês anterior e posterior) com cor de texto atenuada (`UIHelper.TEXT_MUTED`).
   - Destaque sutil para o dia de **Hoje** (`LocalDate.now()`).
   - Destaque em destaque executivo (`UIHelper.ACCENT_BLUE` com texto branco) para a data atualmente preenchida no campo.
   - Efeito de hover suave nos dias para feedback tátil.
4. **Ações Rápidas no Rodapé:**
   - Botão **"Hoje"**: seleciona a data corrente e encerra o popup.
   - Botão **"Limpar"**: esvazia o texto do campo e encerra o popup.
   - Botão **"Fechar"**: fecha o popup mantendo o valor inalterado.

---

## 3. Ergonomia e Acessibilidade por Teclado
- **Atalho Canónico:** Pressionar `F4` ou `Alt + Seta para Baixo` dentro do campo de data abre o calendário.
- **Cancelamento:** Pressionar `ESC` fecha o popup imediatamente.
- **Acessibilidade:** Tooltip claro indicando `"Abrir calendário (F4)"`.
