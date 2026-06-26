# Spec — Formulários em Modal Responsivo

> Padrão de UX para criação de documentos: o formulário vive num **modal responsivo com scroll**,
> deixando a **tabela/lista do separador em ecrã inteiro**. Convenções em [CONVENTIONS.md](../CONVENTIONS.md).

**Última actualização:** 2026-06-26
**Estado:** padrão aplicado a Faturação (FT), Registar Compra (V/FT) e Encomenda a Fornecedor (EC-F).

---

## 1. Problema

Os formulários de criação ocupavam metade do separador (split lado-a-lado ou em cima), espremendo
as tabelas. Pretendia-se: tabelas com espaço livre + formulários em modais profissionais que se
adaptam ao ecrã.

## 2. Componente canónico — `ModernFormDialog`

Modal reutilizável (`gui/components/ModernFormDialog`) com:
- **Scroll automático:** o conteúdo é sempre embrulhado num `JScrollPane` vertical
  (`AS_NEEDED`/horizontal `NEVER`, incremento 16) — campos/linhas nunca empurram o diálogo para fora.
- **Responsivo:** após `pack`, o tamanho é limitado a **92% × 88%** do ecrã e centrado no pai;
  `setSize(w,h)` também respeita esse tecto. Tamanho mínimo garantido.
- **Botões:** Cancelar + **Gravar** (ícone `fas-save` via `UIHelper.icon`, **sem emojis** — regra do projeto).
- **Validação:** `setOnSave(Runnable)` que **lança `RuntimeException`** em erro → o modal mantém-se
  aberto e mostra a mensagem; em sucesso fecha e `showDialog()` devolve `true`.

## 3. Padrão de uso (por separador)

1. O separador mostra **apenas a lista/tabela** (ecrã inteiro) + um cabeçalho com botão **"Novo…"**.
2. O botão abre o `ModernFormDialog` com o conteúdo do formulário (inputs + linhas de rascunho + total).
3. `onSave` faz **validação + criação**, lançando em erro; em sucesso o separador recarrega a lista.

```java
ModernFormDialog dlg = new ModernFormDialog(parent, "Título", formContent);
dlg.setSize(900, 680);                 // limitado ao ecrã automaticamente
dlg.setOnSave(this::submitXxxOrThrow); // lança RuntimeException em erro
if (dlg.showDialog()) reloadList();
```

## 4. Aplicação

| Separador                         | Lista a ecrã inteiro          | Botão            | Submit (throwing)              |
|-----------------------------------|-------------------------------|------------------|--------------------------------|
| Comercial › Faturação (FT)        | Faturas Recentes              | «Nova Fatura…»   | `submitInvoiceOrThrow`         |
| Compras › Faturas de Compra       | Faturas de Compra Registadas  | «Registar Compra…» | `submitPurchaseOrThrow`      |
| Compras › Encomendas a Fornecedor | Encomendas Registadas         | «Nova Encomenda…» | `submitPurchaseOrderOrThrow`   |

Os modais já existentes (fornecedor, categoria, pagamento) usam o mesmo `ModernFormDialog`,
beneficiando automaticamente do scroll + responsividade.

## 5. Não-objectivos

- POS mantém o seu fluxo dedicado (não é um formulário de documento clássico).
- Persistência de tamanho/posição do modal entre sessões.
