# HARNESS — Hierarquia visual de RH e Configurações

Complementa [HR_CONFIG_UI_HIERARCHY_SPEC.md](HR_CONFIG_UI_HIERARCHY_SPEC.md).

## A — Componente automático

| ID | Cenário | Esperado |
|---|---|---|
| HCUI-01 | criar menu sem nome | rejeitado com mensagem clara |
| HCUI-02 | criar menu válido | altura canónica, tooltip e nome acessível |
| HCUI-03 | adicionar duas entradas | ambas preservam texto, ícone e listener |
| HCUI-04 | adicionar sexta entrada | rejeitada para impedir menu excessivo |
| HCUI-05 | activar entrada | listener original executado uma vez |

## B — Auditoria dos painéis

| ID | Área | Esperado |
|---|---|---|
| HCUI-10 | RH / Colaboradores | Novo + Editar + Mais acções |
| HCUI-11 | RH / Recibos | Gerar + Pagar + Processar + Documentos; críticas visíveis |
| HCUI-12 | Configuração / Utilizadores | Novo + Editar + Mais acções |
| HCUI-13 | Configuração / Backups | um menu Criar backup com três modalidades |
| HCUI-14 | acções existentes | mesmos métodos/listeners, sem mudança de negócio |

## C — Validação manual em Windows

| ID | Cenário | Evidência esperada |
|---|---|---|
| HCUI-20 | 1024×700 e 1366×768 | cabeçalhos sem corte ou segunda linha desnecessária |
| HCUI-21 | escala 100%, 125% e 150% | menus e textos legíveis, sem sobreposição |
| HCUI-22 | temas claro/escuro | contraste e hover/foco visíveis |
| HCUI-23 | teclado | Tab chega ao botão; Enter abre; setas seleccionam; Esc fecha |
| HCUI-24 | executar cada entrada | comportamento idêntico ao botão anterior |

## Gate

Executar `ActionMenuButtonTest`, `mvn clean compile`, auditoria do diff e actualizar
`tasks/current.md`. HCUI-20..24 exigem evidência visual num Windows real.

## Evidência de execução — 2026-08-15

- `ActionMenuButtonTest`: HCUI-01..05 verdes.
- Harness focado (`ActionMenuButtonTest`, `TopNavItemTest`, `CanonicalControlsTest` e
  `UiPanelDecompositionTest`): verde.
- `mvn clean compile`: verde.
- `mvn test`: verde, 487 testes, 0 falhas, 0 erros e 0 ignorados.
- Auditoria estática confirma os quatro menus e preserva visíveis Aprovar, Rejeitar, Eliminar,
  Marcar Pago e Processar Mês.
- HCUI-20..24 permanecem pendentes de validação visual e operacional em Windows real.
