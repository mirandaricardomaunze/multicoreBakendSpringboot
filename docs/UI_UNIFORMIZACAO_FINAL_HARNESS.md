# HARNESS — Uniformização final do design Swing

Complementa [UI_UNIFORMIZACAO_FINAL_SPEC.md](UI_UNIFORMIZACAO_FINAL_SPEC.md).

## A — Automático

| ID | Cenário | Esperado |
|---|---|---|
| FU-01 | renderer de `MANAGER` | mostra “Gestor” |
| FU-02 | select de perfis | apresenta etiqueta humana e preserva código seleccionado |
| FU-03 | RH/Config/Aprovações/Plataforma | aplicam renderer humano |
| FU-04 | formulário legado | célula é `FormField` e preserva input |
| FU-05 | label com `*` | `FormField.required=true` |
| FU-06 | Plataforma | três barras densas usam `ActionMenuButton` |
| FU-07 | Fiscal IVA | usa menu “Documentos” |
| FU-08 | painéis de negócio | zero `new Color(...)` |
| FU-09 | idioma | zero “Atualizar” e “Cadastrar” na UI |
| FU-10 | mensagem de permissão | zero `MANAGER/ADMIN` visível |
| FU-11 | componentes anteriores | harness HCUI/SCUI permanece verde |
| FU-12 | arquitectura UI | painéis prioritários ≤ 1.000 linhas |

## B — Manual

| ID | Cenário | Evidência esperada |
|---|---|---|
| FU-20 | RH e utilizadores | nenhuma role inglesa visível |
| FU-21 | Plataforma/Fiscal em 1366×768 | barras sem corte ou sobreposição |
| FU-22 | formulários representativos | labels, campos, obrigatório e foco alinhados |
| FU-23 | temas/escalas Windows | contraste e layout correctos a 100/125/150% |

## Gate

Executar `FinalUiUniformityHarnessTest`, harnesses UI anteriores, `mvn clean compile` e suite
completa. FU-20..23 exigem observação manual com sessão real.

## Evidência de execução — 2026-08-15

- `FinalUiUniformityHarnessTest` e todos os harnesses UI anteriores: verdes.
- `mvn clean compile`: verde.
- `mvn test`: verde, **496 testes, 0 falhas, 0 erros e 0 ignorados**.
- Auditoria case-insensitive dos painéis: zero `new Color(...)`, “Atualizar”, “Cadastrar” e
  `MANAGER/ADMIN`.
- `PlataformaPanel`: 995 linhas; `FiscalPanel`: 770 linhas.
- FU-20..23 permanecem como certificação manual em sessão real e escalas Windows controladas.
