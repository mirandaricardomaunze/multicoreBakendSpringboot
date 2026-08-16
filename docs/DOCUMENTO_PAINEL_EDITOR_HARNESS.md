# HARNESS — Documento em painel completo (piloto: Encomenda)

Complementa [DOCUMENTO_PAINEL_EDITOR_SPEC.md](DOCUMENTO_PAINEL_EDITOR_SPEC.md). `DE-01..` automáticos
(`DocumentEditorHostTest`, JUnit puro); `DE-50..` manuais (UI).

## Automáticos — `DocumentEditorHostTest`

| ID    | Cenário | Esperado |
|-------|---------|----------|
| DE-01 | `shouldConfirmDiscard(true)` | `true` (há rascunho → confirmar antes de sair) |
| DE-02 | `shouldConfirmDiscard(false)` | `false` (nada por gravar → sai directo) |
| DE-03 | Construção com conteúdo | O host contém o conteúdo passado (não o descarta) |

## Manuais (UI)

| ID    | Cenário | Evidência |
|-------|---------|-----------|
| DE-50 | Clicar **Nova Encomenda** | A aba troca da lista para o **editor a ecrã inteiro** (não abre modal) |
| DE-51 | Adicionar linhas e **Guardar** | Cria a encomenda, mostra a mensagem, recarrega a lista e volta à lista |
| DE-52 | Com linhas no rascunho, clicar **Voltar à lista** | Pede confirmação para descartar; só sai se confirmar |
| DE-53 | Sem linhas, clicar **Voltar** | Volta à lista sem perguntar |
| DE-54 | Erro de validação ao **Guardar** (sem armazém/linhas) | Mostra o erro e **mantém** o editor aberto |
| DE-55 | Empresa activa | Barra de topo + `StatusBar` visíveis durante a edição (contexto não se perde) |
| DE-56 | Aba Faturação → **Nova Fatura** | Troca para o editor a ecrã inteiro (não abre modal) |
| DE-57 | Adicionar linhas e **Guardar** (fatura) | Cria a fatura, mostra mensagem (ou bloqueio de desconto >10%), recarrega e volta |
| DE-58 | **Voltar à lista** com rascunho (fatura) | Pede confirmação para descartar |
| DE-59 | Editor com formulário alto (fatura/encomenda) | **Scroll vertical** revela os botões de baixo; barra Voltar/Guardar fica fixa |

## Definition of done

- `mvn -o clean compile` passa.
- `mvn -o test` passa (DE-01..03).
- Sem emojis (só `UIHelper.icon`).
- O modal antigo (`openOrderFormDialog`) é removido; a criação passa pelo painel.
