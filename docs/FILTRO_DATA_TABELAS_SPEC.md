# SPEC — Filtro por data nas tabelas onde é necessário

**Criado em:** 2026-08-30
**Camada:** UI (`TableFilter` + painéis)
**Sem migração, sem alteração de API** — o componente já existia; o que faltava era aplicá-lo, e
faltava-lhe uma linguagem.

---

## 1. Problema

O `TableFilter.PeriodFilter` existe desde as tabelas de documentos comerciais e está aplicado em
**17 painéis**. Mas três tabelas que crescem sem limite ficaram de fora, e uma delas não tinha
filtro nenhum:

| Tabela | Coluna de data | Filtro antes |
|---|---|---|
| **Cotações** | `Data` (emissão) | pesquisa + estado |
| **Guias de Remessa** | `Data` (emissão) | pesquisa + estado |
| **Lotes & Validades** | `Validade` | **nenhum** — só paginação |

Nos dois primeiros a consequência é a de sempre: a lista cresce e a única forma de encontrar um
documento de Março é percorrer páginas.

**No terceiro é pior**, e é o que motiva esta spec. Um ecrã de lotes existe para responder a uma
pergunta que se faz todos os dias — *o que vence a seguir?* — e a resposta era ler página a página
uma coluna de datas. Num sistema com FEFO, o lote que se perde por caducar é dinheiro deitado fora.

## 2. A descoberta que muda o desenho

O vocabulário existente do `periodCombo()` é **todo virado para trás**:

```
Todo o período · Hoje · Últimos 7 dias · Últimos 30 dias · Este mês
```

Serve emissão, pagamento, movimento — datas que já aconteceram. **Não serve validade**, que é uma
data à frente. "Últimos 30 dias" numa coluna de validade mostra o que *já* venceu no mês passado, e
esconde exactamente o que se procura.

> **Regra:** o vocabulário do período segue a **direcção da coluna**, não o gosto de quem escreve o
> ecrã. Uma data passada e uma data futura são perguntas opostas.

Por isso não se reutiliza o combo: reutiliza-se o **componente**, com uma segunda linguagem.

```
expiryPeriodCombo()
Toda a validade · Já vencidos · Vence em 7 dias · Vence em 30 dias · Vence em 90 dias
```

`Já vencidos` é opção própria e não um extremo da escala — é outra pergunta e outra urgência: o que
venceu é perda a registar, o que vai vencer é acção a tomar.

## 3. DRY — o que **não** foi duplicado

- **Um só matcher.** `TableFilter.matchesPeriod(data, opção, hoje)` ganhou os casos novos. Não há
  segundo método, segunda classe nem segundo `RowFilter`.
- **Uma só instalação.** `TableFilter.install(tabela, pesquisa, colunas, períodos)` — a mesma
  chamada dos outros 17 painéis.
- **Uma só barra.** `TableFilter.bar(...)` com `TableFilter.label(texto, ícone)`.
- O filtro compara com o **valor do modelo**, e a data é lida da célula por
  `parseCellDate` (dd/MM/yyyy, com ou sem hora) — sem cada painel reinventar o *parse*.

O que é novo é **uma fábrica de combo e cinco linhas no `switch`**. Nada mais.

## 4. Onde ficou por fazer, e porquê

**Diário da Contabilidade — declarado, não feito.** Tem coluna `Data` e seria o candidato mais
óbvio, mas é paginado **no servidor** (`getJournal(page, size)`). Um filtro de cliente filtraria só
a página carregada e diria "3 lançamentos em Agosto" quando há trinta nas páginas seguintes — pior
do que não ter filtro, porque parece uma resposta. Precisa de intervalo de datas na API
(`GET /api/accounting/journal?from=&to=`), que é alteração de contrato e fica para decisão própria.

**Colaboradores (`Admissão`) — julgado desnecessário.** Um quadro de pessoal é um registo, não um
fluxo de documentos; procura-se por nome, não por período de admissão.

## 5. Verificação

Ver [FILTRO_DATA_TABELAS_HARNESS.md](FILTRO_DATA_TABELAS_HARNESS.md).
