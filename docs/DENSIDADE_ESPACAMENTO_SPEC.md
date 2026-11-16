# Densidade da interface e escala de espaçamentos

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/DensitySpacingHarnessTest.java`

## Problema

- A opção "Densidade: Compacto / Padrão / Confortável" só mudava a altura das linhas das tabelas.
  O `fontScale` do `UiDensity` nunca era usado e a altura dos campos era a constante
  `FORM_CONTROL_HEIGHT = 38`, que o compilador copiava para os 213 sítios onde aparece.
- As margens eram escritas à mão: 656 `createEmptyBorder`/`EmptyBorder` com 154 combinações, e
  10, 12, 14, 15 e 16 px conviviam no mesmo tipo de cartão.

## Regras

| ID    | Regra |
|-------|-------|
| DE-01 | Confortável aumenta letra, altura dos campos e espaços; Compacto reduz; Padrão é a base (corpo 12, campo 38, cartão 16). |
| DE-02 | Em qualquer densidade a hierarquia mantém-se (legenda < corpo < … < destaque; XXS < XS < … < XXL). |
| DE-03 | `FORM_CONTROL_HEIGHT`, os papéis do `Typography` e os passos do `Spacing` não são constantes de compilação. |
| DE-04 | Mudar a densidade reconstrói a janela principal (como a troca de tema); a densidade guardada é carregada antes do primeiro ecrã. |
| DE-05 | As margens usam `Spacing.*`; só `0` fica literal. Excepções: impressos dimensionados para papel e o Painel Executivo. |

## Escala de espaçamentos (Padrão)

| Passo | px | Uso | Absorveu |
|-------|----|-----|----------|
| `XXS` | 2 | ícone ↔ contador, rótulos empilhados | 1–3 |
| `XS` | 4 | folga dentro de um controlo | 4–6 |
| `SM` | 8 | itens relacionados, controlos compactos | 7–10 |
| `MD` | 12 | linhas e secções de formulário | 11–13 |
| `LG` | 16 | interior de cartões e diálogos | 14–18 |
| `XL` | 24 | entre secções da página | 19–26 |
| `XXL` | 32 | estados vazios, blocos de destaque | 27+ |

## Factores por densidade

| Densidade | Letra e espaços | Campo | Linha de tabela |
|-----------|-----------------|-------|-----------------|
| Compacto | × 0,90 | 32 | 28 |
| Padrão | × 1,00 | 38 | 36 |
| Confortável | × 1,15 | 44 | 44 |
