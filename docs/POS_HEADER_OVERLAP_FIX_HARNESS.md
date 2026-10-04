# Harness — Teste e Validação da Eliminação de Sobreposição no POS

> **Status:** Aprovado  
> **Data:** 2026-10-04  
> **Suíte Automatizada:** `PosHeaderLayoutHarnessTest.java`

---

## 1. Objectivos de Teste

Garantir que a barra superior do POS (`topBar`) nunca sobreponha botões ou elementos gráficos, independentemente do estado da sessão (aberta ou fechada) e sob dimensões de ecrã padrão de operadores (1366x768 e 1280x720 com sidebar ativa).

---

## 2. Casos de Teste (POS-OVERLAP-01 a POS-OVERLAP-05)

### POS-OVERLAP-01: Ausência de Intersecção de Bounds em Ecrã Operacional
- **Condição:** Janela configurada em 1024px de largura útil, com sessão de caixa ABERTA (`activeSession != null`), ativando simultaneamente `shiftHandoverBtn`, `cashMoveBtn` e `closeSessionBtn`.
- **Validação:**
  - O maior X absoluto de qualquer componente em `segmented` deve ser estritamente menor que o menor X absoluto de qualquer componente em `sessionActions`.
  - Nenhuma caixa delimitadora (bounding box) dos botões deve intersectar outra caixa.

### POS-OVERLAP-02: `GridBagLayout` Anti-Colisão no `topBar`
- **Condição:** Inspeção do layout manager de `topBar`.
- **Validação:**
  - `topBar.getLayout()` deve ser uma instância de `GridBagLayout`.
  - Deve conter um espaçador expansível (`horizontal glue` com `weightx = 1.0`) entre o grupo da esquerda e o grupo da direita.

### POS-OVERLAP-03: Agrupamento Canónico em `ActionMenuButton` (Operações)
- **Condição:** Inspeção de `segmented`.
- **Validação:**
  - `segmented` contém `tabVendaBtn`, `tabHistBtn` e `operationsMenu` (`ActionMenuButton`).
  - `operationsMenu` possui exatamente as ações de Cotação (F7), Fidelidade e Fechos de Caixa (Z), respeitando o limite estrito $\le 5$ opções.

### POS-OVERLAP-04: Preservação de Cores Semânticas Canónicas
- **Condição:** Verificação das cores e tokens dos botões primários.
- **Validação:**
  - `openSessionBtn` utiliza verde de aprovação (`UIHelper.APPROVED_GREEN`).
  - `closeSessionBtn` utiliza vermelho de perigo (`UIHelper.REJECTED_RED`).
  - `cashMoveBtn` utiliza âmbar/laranja de alerta (`UIHelper.PENDING_YELLOW` ou warning).

### POS-OVERLAP-05: Decomposição Estrita de Linhas do `POSPanel.java` ($\le 1000$ linhas)
- **Condição:** Contagem de linhas físicas de `POSPanel.java`.
- **Validação:**
  - `lines <= 1000` garantindo conformidade com a regra de decomposição arquitetural.
