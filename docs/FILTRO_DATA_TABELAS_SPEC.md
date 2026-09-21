# SPEC — Filtro por data nas tabelas onde é necessário

**Criado em:** 2026-08-30
**Camada:** UI (`TableFilter` + painéis)
**Sem migração, sem alteração de API** — o componente já existia; o que faltava era aplicá-lo, e
faltava-lhe uma linguagem.

---

## 1. Problema

O `TableFilter.PeriodFilter` existe desde as tabelas de documentos comerciais e está aplicado em
**17 painéis**. Duas tabelas de documentos que crescem sem limite ficaram de fora:

| Tabela | Coluna de data | Filtro antes |
|---|---|---|
| **Cotações** | `Data` (emissão) | pesquisa + estado |
| **Guias de Remessa** | `Data` (emissão) | pesquisa + estado |

A consequência é a de sempre: a lista cresce e a única forma de encontrar um documento de Março é
percorrer páginas.

## 2. A regra que delimita onde este filtro serve

O vocabulário do `periodCombo()` é **todo virado para trás**:

```
Todo o período · Hoje · Últimos 7 dias · Últimos 30 dias · Este mês
```

Serve emissão, pagamento, movimento — datas que já aconteceram. **Não serve validade**, que é uma
data à frente: "Últimos 30 dias" numa coluna de validade mostra o que *já* venceu no mês passado e
esconde exactamente o que quem gere lotes procura.

> **Regra:** o vocabulário do período segue a **direcção da coluna**, não o gosto de quem escreve o
> ecrã. Uma data passada e uma data futura são perguntas opostas.

É por isso que este filtro **não** foi aplicado a *Lotes & Validades* — ver §4.

## 3. DRY — o que **não** foi duplicado

- **Um só matcher.** `TableFilter.matchesPeriod(data, opção, hoje)` — sem segundo método, segunda
  classe nem segundo `RowFilter`.
- **Uma só instalação.** `TableFilter.install(tabela, pesquisa, colunas, períodos)` — a mesma
  chamada dos outros 17 painéis.
- **Uma só barra.** `TableFilter.bar(...)` com `TableFilter.label(texto, ícone)`.
- O filtro compara com o **valor do modelo**, e a data é lida da célula por
  `parseCellDate` (dd/MM/yyyy, com ou sem hora) — sem cada painel reinventar o *parse*.

**Não há código novo de filtragem.** As duas tabelas passam a chamar o que já existia — a
alteração são seis linhas em cada painel.

## 4. Lotes & Validades — o erro que a fotografia apanhou

**A primeira versão desta alteração acrescentou um filtro de validade ao separador de lotes, e
estava errada.** O levantamento procurou `TableFilter.install` e concluiu "esta tabela não tem
filtro nenhum". Tem: o painel filtra por mecanismo próprio (`filterBatches()`), com controlos
próprios de armazém, pesquisa e validade — e com um vocabulário **melhor** do que o que eu ia
introduzir:

```
Todos os lotes · Vencidos · Vence em ≤ 30 dias · Vence em ≤ 90 dias · Válidos (> 90 dias)
```

O resultado teria sido dois campos de pesquisa e dois filtros de validade no mesmo ecrã — a
duplicação que esta spec existe para evitar. Só se viu ao **fotografar o ecrã**; nenhum teste e
nenhum `grep` a apanhavam.

Revertido. Fica declarado o que sobra: **existem duas maneiras de filtrar tabelas neste sistema** —
o `TableFilter` partilhado e o `filterBatches()` do painel de lotes. Unificá-las é trabalho próprio,
com decisão própria, e não se faz de passagem: o vocabulário do painel é mais rico e o `TableFilter`
não sabe filtrar por armazém.

## 5. Onde ficou por fazer, e porquê

**Diário da Contabilidade — declarado, não feito.** Tem coluna `Data` e seria o candidato mais
óbvio, mas é paginado **no servidor** (`getJournal(page, size)`). Um filtro de cliente filtraria só
a página carregada e diria "3 lançamentos em Agosto" quando há trinta nas páginas seguintes — pior
do que não ter filtro, porque parece uma resposta. Precisa de intervalo de datas na API
(`GET /api/accounting/journal?from=&to=`), que é alteração de contrato e fica para decisão própria.

**Lotes & Validades — já resolvido pelo painel.** Ver §4.

**Colaboradores (`Admissão`) — julgado desnecessário.** Um quadro de pessoal é um registo, não um
fluxo de documentos; procura-se por nome, não por período de admissão.

## 6. Verificação

Ver [FILTRO_DATA_TABELAS_HARNESS.md](FILTRO_DATA_TABELAS_HARNESS.md).
