# Spec — Inputs e selects profissionais (estilo único)

> Elevação visual de **todos** os campos de formulário (text fields, password, text area, combos) via
> os estilizadores centrais em [UIHelper](../src/main/java/mz/multicore/erp/gui/components/UIHelper.java).
> **Só apresentação** — uma fonte de verdade, sem tocar nos ~centenas de call sites.

**Última actualização:** 2026-06-29

## Problema

Os campos eram funcionais mas com aspecto amador:

1. **Cores fixas, não sensíveis ao tema.** `styleTextField`/`stylePasswordField`/`styleTextArea`/
   `styleComboBox` pintavam o fundo com `BG_CARD` e a borda com um cinzento **hardcoded**
   (`new Color(75, 85, 99)`). No **tema claro** a borda escura destoava — denuncia improviso.
2. **Sem realce de foco.** Ao focar um campo nada mudava — falta a afordância nº1 de um input
   profissional (a borda acende na cor de acento).
3. **Seta do combo amador.** O botão de seta herdava o bevel 3D do Metal/Ocean, com fundo cinzento
   que não combina com o campo.

## Decisões

- **Cores pelo tema.** Fundo = `FIELD_BG`, borda = `BORDER` (slots de [Theme](../src/main/java/mz/multicore/erp/gui/components/Theme.java)),
  em vez de constantes fixas. Funciona em claro e escuro e acompanha a troca de tema (os
  estilizadores são reaplicados na re-skin).
- **Realce de foco a acento.** Borda normal `BORDER`; ao ganhar foco passa a `ACCENT` (2px),
  ao perder foco volta ao normal. Aplicado a text field, password, text area e combo, via um
  helper partilhado (`installFocusBorder`) — **DRY**. Guardado por *client property* para não
  empilhar listeners em re-skins.
- **Borda arredondada + padding consistente** (cantos suaves, respiro interno uniforme) em todos
  os campos.
- **Combo achatado.** O botão de seta perde o bevel (sem borda, fundo = `FIELD_BG`), para o combo
  parecer uma peça só. O popup já segue o tema (renderer existente).
- **Altura uniforme** mantida (`FORM_CONTROL_HEIGHT`, 38px) — alinhada com os botões.

## Não-objetivos

- Não trocar de Look&Feel (continua Metal/Ocean) nem introduzir FlatLaf.
- Não mexer nos call sites — só nos estilizadores centrais do `UIHelper`.
- Não alterar comportamento/validação dos formulários.
