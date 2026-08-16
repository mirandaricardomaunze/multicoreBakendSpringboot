# SPEC — Documento em painel completo (não modal)

**Criado em:** 2026-07-27
**Camada:** UI Swing (`com.phcpro.gui`)
**Sem backend, sem migração.**
**Piloto:** criação de **Encomenda**.

> Segue a decisão de UX (2026-07-27): **híbrido** — listagem como ecrã principal, **painel completo**
> para criar/editar documentos com linhas, **modais só para acções curtas**. Aqui aplica-se o padrão
> ao documento **Encomenda** como piloto, para validar antes de alastrar (Fatura, Compras, …).

## 1. Porquê painel e não modal (para documentos com linhas)

Um documento com **tabela de linhas** (produtos, qtd, desconto, lote, totais) fica apertado num
modal. Editá-lo a ecrã inteiro dá espaço, mantém a barra de topo (empresa activa) e a `StatusBar`
sempre visíveis, e é o que os ERPs fazem. Acções curtas (motivo, pagamento, confirmação) **continuam
em modal**.

## 2. Padrão — `DocumentEditorHost` (reutilizável)

Componente que aloja o **conteúdo do documento** com uma **barra de acções** no topo:

- **← Voltar à lista** (com **guarda de alterações**: se houver rascunho por gravar, confirma
  descartar antes de sair).
- Título do documento.
- **Guardar** (delega no `onSave` do painel; erros mantêm o editor aberto).

Contrato: `new DocumentEditorHost(titulo, conteudo, onSave, onBack, dirtySupplier)`.
`dirtySupplier` diz se há alterações por gravar (ex.: `!draftOrderLines.isEmpty()`).

## 3. Navegação — aba alterna lista ⇄ editor

A aba **Encomendas** passa a `CardLayout` com dois cartões:
- `list` — cabeçalho + tabela (como hoje).
- `editor` — `DocumentEditorHost` a envolver o formulário de encomenda (o mesmo `orderFormContent`
  que antes ia no modal).

Fluxo: **Nova Encomenda** → mostra `editor` (rascunho limpo); **Guardar** cria a encomenda
(`issueOrderOrThrow`, inalterado), mostra mensagem, recarrega a lista e volta a `list`; **Voltar** →
guarda de alterações → `list`.

## 4. Reutilização / não-reescrita

- O formulário (`orderFormContent`), a validação e a criação (`issueOrderOrThrow`) **não mudam** —
  só muda o **hospedeiro** (modal → painel). O `openOrderFormDialog` (modal) é **removido**.
- Regras de negócio continuam no backend; a UI só chama HTTP.

## 5. Alastramento

- **Encomenda** (piloto) e **Fatura** — feitos: ambas as abas alternam lista ⇄ editor com o mesmo
  `DocumentEditorHost` (reutiliza `invoiceFormContent`/`orderFormContent` + `submitInvoiceOrThrow`/
  `issueOrderOrThrow`); os modais `openInvoiceFormDialog`/`openOrderFormDialog` foram removidos.
- **A seguir:** Compras (encomenda a fornecedor); e suportar **editar** documento existente no host.

## 6. Ficheiros

| Ficheiro | Papel |
|----------|-------|
| `gui/components/DocumentEditorHost.java` (novo) | barra Voltar/Guardar + guarda de alterações |
| `gui/ComercialPanel.java` | aba Encomendas: CardLayout lista⇄editor; remove o modal |
