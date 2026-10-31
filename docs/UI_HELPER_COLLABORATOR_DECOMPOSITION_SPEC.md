# Consolidação dos colaboradores de UIHelper

## Diagnóstico

Os colaboradores extraídos na Fase 197 ainda concentravam styling de abas, tabelas,
controlos, formulários, prompts e traduções num único ficheiro por área.

## Decisão

- `UiTabbedAndTableStyler` permanece como fachada fina e delega para
  `UiTabbedPaneStyler`, `UiTableCellStyler`, `UiTableInteractionSupport` e
  `UiTableFooterSupport`.
- `UiFormControlAndFormatSupport` permanece como fachada fina.
- `UiFormComponentStyler` conserva o styling visual e a grelha de formulário.
- `UiTextInputSupport` conserva prompts e filtros de introdução.

As assinaturas de `UIHelper`, dimensões, cores, listeners, atalhos e ordem de
instalação permanecem inalterados. Não existe alteração de layout ou comportamento.
