# Especificação de Fecho Automático e Manual de Mensagens de Feedback (SPEC-FAM-001)

## 1. Visão Geral e Objectivo
Esta especificação define o comportamento padrão para todas as mensagens de feedback visual no ERP Multicore (painéis em linha via `InlineFeedbackPanel` e notificações flutuantes via `ToastManager`).

O objectivo é atender ao requisito ergonómico:
> *"As mensagens de feedback devem aparecer, fechar automaticamente após um tempo adequado e permitir ao utilizador fechar manualmente a qualquer momento."*

---

## 2. Princípios de Desenho e Ergonomia

1. **Visibilidade Semântica Clara:** A mensagem surge com cores semânticas vibrantes do tema (`SUCCESS`, `INFO`, `WARNING`, `ERROR`), ícone vetorial FontAwesome de alta resolução e texto descritivo.
2. **Fecho Automático Temporizado (Auto-Dismiss):**
   - O operador não deve ser obrigado a clicar repetidamente em "X" para fechar avisos de rotina (ex.: "Cliente guardado com sucesso", "Linha adicionada").
   - O temporizador é calibrado com base na densidade de leitura:
     - `SUCCESS`: **5.000 ms (5s)**
     - `INFO`: **5.000 ms (5s)**
     - `WARNING`: **7.000 ms (7s)**
     - `ERROR`: **8.000 ms (8s)** (tempo maior para diagnóstico de falha e leitura do detalhe).
3. **Pausa ao Passar o Rato (`Hover Pause`):**
   - Se o utilizador mover o cursor do rato por cima da mensagem (`mouseEntered`), o temporizador é imediatamente **pausado**.
   - A mensagem **NÃO desaparece** enquanto o operador a estiver a ler ou com o rato por cima a ponderar clicar num botão de ação (como "Tentar novamente").
   - Quando o rato sai da mensagem (`mouseExited`), o temporizador é reiniciado suavemente.
4. **Fecho Manual Imediato:**
   - O utilizador pode fechar a mensagem a qualquer momento através do botão de fechar (`fas-times`), estilizado com cursor pointer, tooltip `"Fechar mensagem"` e atalho acessível.
   - O clique no botão ou na notificação cancela imediatamente o temporizador e esconde o painel, actualizando o layout do ecrã pai (`revalidate()`, `repaint()`).
5. **Opção de Mensagem Persistente:**
   - Em operações críticas em que a mensagem precise de permanecer visível até acção expressa do utilizador, é possível chamar `show(..., int autoCloseDurationMs)` passando `0` ou `panel.setAutoCloseEnabled(false)`.

---

## 3. Componentes Implementados

### 3.1 `InlineFeedbackPanel`
- Componente contextual inserido no cabeçalho ou topo de tabelas em mais de 20 painéis operacionais (`ClientesPanel`, `ComprasPanel`, `ComercialPanel`, `StockPanel`, `POSPanel`, `ModernFormDialog`, etc.).
- Incorpora:
  - Temporizador `autoCloseTimer` de paragem segura no EDT.
  - Listeners de hover recursivos para toda a árvore de componentes filhos.
  - Botão de fecho compacto (28x28) com ícone vetorial FontAwesome `fas-times` e tooltip.
  - Suporte ao botão contextual de acção (ex.: "Tentar novamente").

### 3.2 `ToastManager`
- Notificações não-bloqueantes ancoradas no canto inferior da janela activa.
- Temporizador com slide-up/fade-in e `attachHoverRecursive` para não fechar enquanto o rato estiver sobre a notificação.
- Clique em qualquer ponto ou no botão `fas-times` fecha imediatamente.

---

## 4. Testes Automatizados no Harness
Aprovados em [FeedbackAutoCloseAndManualDismissHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/components/FeedbackAutoCloseAndManualDismissHarnessTest.java):
- `FDB-01`: Fecho automático após expirar o temporizador.
- `FDB-02`: Fecho manual imediato via botão `fas-times`.
- `FDB-03`: Duração calibrada por tipo semântico (`ERROR` > `WARNING` > `SUCCESS`).
- `FDB-04`: Pausa com `mouseEntered` (hover) e persistência durante leitura.
- `FDB-05`: Desativação de fecho automático para alertas críticos permanentes.
- `FDB-06`: Notificações Toast com fecho manual e acessibilidade.
