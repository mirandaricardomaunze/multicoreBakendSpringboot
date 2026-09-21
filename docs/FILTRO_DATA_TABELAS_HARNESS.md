# Harness — Filtro por data nas tabelas

Mede o progresso contra [FILTRO_DATA_TABELAS_SPEC.md](FILTRO_DATA_TABELAS_SPEC.md).
Legenda: ✅ feito · 🟡 parcial · ❌ em falta · ⚖️ decisão que não é de código.

**Data:** 2026-08-30 · Sem migração, sem alteração de API.

---

## 1. Vocabulário do passado (emissão, pagamento, movimento)

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| FD-01 | `Hoje` sobre a data de hoje | Passa | ✅ `backwardVocabularyLooksBackwards` |
| FD-02 | `Últimos 7 dias` no 6.º dia atrás | Passa; no 7.º já não | ✅ mesmo teste — a fronteira é onde o erro vive |
| FD-03 | `Últimos 7 dias` sobre data **futura** | **Não** passa | ✅ o que impede um documento pós-datado de aparecer como recente |
| FD-04 | Opção "Todo o período" | Passa tudo | ✅ e "Toda a validade" também, por prefixo |

## 3. Aplicação nos ecrãs

| ID | Tabela | Coluna | Vocabulário | Estado |
|----|--------|--------|-------------|--------|
| FD-20 | Cotações | `Data` (col. 2) | passado | ✅ ao lado do filtro de estado que já existia — **confirmado no ecrã** |
| FD-21 | Guias de Remessa | `Data` (col. 2) | passado | ✅ idem, **confirmado no ecrã** |
| FD-22 | Lotes & Validades | `Validade` | — | 🔴→✅ **erro meu, revertido.** Acrescentei um filtro a um ecrã que já tinha o seu, com vocabulário melhor. Ver SPEC §4 |
| FD-23 | Diário da Contabilidade | `Data` | — | ❌ **declarado**: paginado no servidor; filtro de cliente só veria a página carregada. Precisa de `from`/`to` na API |
| FD-24 | Colaboradores | `Admissão` | — | ⚖️ **julgado desnecessário**: um quadro de pessoal é um registo, procura-se por nome |

## 4. DRY

| ID | Verificação | Estado |
|----|-------------|--------|
| FD-30 | Nenhum código de filtragem novo | ✅ as duas tabelas chamam o que já existia |
| FD-31 | Nenhum painel faz *parse* de datas por si | ✅ `TableFilter.parseCellDate` |
| FD-32 | Nenhuma segunda classe de filtro | ✅ `PeriodFilter` é a mesma dos 17 painéis anteriores |

---

## 5. Declarações honestas

- **A lógica está testada; a ligação ao ecrã foi vista, não testada.** Os 6 testes cobrem
  `matchesPeriod`. Que a coluna 2 é a data foi **confirmado nas fotografias** das Cotações e das
  Guias — um índice errado não parte nada, filtra a coluna errada em silêncio.
- **A fotografia apanhou o que o código não apanhava.** A primeira versão acrescentava um filtro
  duplicado ao ecrã de lotes; nenhum teste e nenhum `grep` o mostravam, porque o filtro que já lá
  estava usa outro mecanismo. Ver SPEC §4.
- **Não foi medido com muitos dados.** O filtro é de cliente, sobre as linhas carregadas; numa
  tabela com paginação de cliente isso é o conjunto todo, mas nunca foi corrido com milhares de
  lotes.
