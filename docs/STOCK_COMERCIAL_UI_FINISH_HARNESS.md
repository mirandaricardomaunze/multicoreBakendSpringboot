# HARNESS — Fecho profissional da UI de Stock e Comercial

Complementa [STOCK_COMERCIAL_UI_FINISH_SPEC.md](STOCK_COMERCIAL_UI_FINISH_SPEC.md).

## A — Harness automático

| ID | Cenário | Esperado |
|---|---|---|
| SCUI-01 | editor recebe `Ctrl+S` | acção Guardar executada uma vez |
| SCUI-02 | editor recebe `Esc` sem alterações | volta à lista uma vez |
| SCUI-03 | fonte do modal | bindings `Ctrl+S` e `Esc` presentes |
| SCUI-04 | filtros de Stock cobertos | altura `FORM_CONTROL_HEIGHT`, sem 35 local |
| SCUI-05 | barra de faturas | menu auxiliar; Liquidar e Anular explícitos |
| SCUI-06 | barra de encomendas | menu auxiliar; Faturar, Converter e Cancelar explícitos |
| SCUI-07 | notas/guias | Aprovar/Rejeitar/Cancelar não escondidos |
| SCUI-08 | Stock global | operações auxiliares agrupadas; bloqueio explícito |
| SCUI-09 | categorias/armazéns | Novo + Editar + Mais acções |
| SCUI-10 | menus | limite máximo testado por `ActionMenuButtonTest` |
| SCUI-11 | tabelas | harness canónico de renderers permanece verde |
| SCUI-12 | decomposição | painéis prioritários continuam ≤ 1.000 linhas |

## B — Validação visual/operacional em Windows

| ID | Cenário | Evidência esperada |
|---|---|---|
| SCUI-20 | 1024×700, 1366×768 e Full HD | barras não cortam nem sobrepõem controlos |
| SCUI-21 | escala 100%, 125% e 150% | campos e menus mantêm altura/alinhamento |
| SCUI-22 | temas claro/escuro | contraste, foco, hover e estados legíveis |
| SCUI-23 | teclado | Tab, Ctrl+S, Esc e navegação dos menus funcionam |
| SCUI-24 | acções agrupadas | resultado igual aos antigos botões em dados reais |

## Gate

Executar `StockCommercialUiHarnessTest`, `ActionMenuButtonTest`, `DocumentEditorHostTest`,
`CanonicalControlsTest`, `UiPanelDecompositionTest`, `POSKeyboardShortcutTest`, `mvn clean compile`
e a suite completa. SCUI-20..24 exigem aplicação real com sessão e dados.

## Evidência de execução — 2026-08-15

- Harness focado completo: verde.
- `mvn clean compile`: verde.
- `mvn test`: verde, **492 testes, 0 falhas, 0 erros e 0 ignorados**.
- Painéis prioritários: `ComercialPanel` 990 linhas e `StockPanel` 891 linhas.
- Auditoria estática: filtros cobertos sem altura 35; menus dentro do limite; acções críticas
  permanecem explícitas; `git diff --check` limpo.
- Validação real em Windows a 1382×736, tema claro: Comercial, RH e Stock sem cortes ou
  sobreposições; filtros alinhados; tabelas legíveis; barras e paginação acessíveis.
- SCUI-20 fica parcialmente coberto pela resolução acima. SCUI-21, tema escuro de SCUI-22 e a
  execução operacional completa SCUI-23/24 permanecem dependentes de uma sessão manual controlada
  a 100%, 125% e 150%; a automação disponível não manteve cliques fiáveis sob DPI elevado.
