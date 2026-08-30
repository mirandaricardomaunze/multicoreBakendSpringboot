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

## 2. Vocabulário da validade (datas à frente)

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| FD-10 | `Vence em 7 dias` sobre lote que vence **hoje** | Passa — é o mais urgente | ✅ `expiryVocabularyLooksForwards` |
| FD-11 | `Vence em 7 dias` no 7.º dia / no 8.º | Passa / não passa | ✅ fronteira testada |
| FD-12 | `Já vencidos` sobre lote de ontem | Passa | ✅ `alreadyExpiredIsItsOwnQuestion` |
| FD-13 | `Já vencidos` sobre lote que vence **hoje** | **Não** passa — ainda não venceu | ✅ mesmo teste |
| FD-14 | `Vence em 30 dias` sobre lote já vencido | **Não** passa | ✅ "a vencer" e "vencido" não se misturam |
| FD-15 | **O erro que isto evita:** lote que vence daqui a 7 dias, filtrado por `Últimos 30 dias` | Desaparecia da lista | ✅ `theBackwardVocabularyWouldHideABatchAboutToExpire` — testa o defeito, não só a correcção |

## 3. Aplicação nos ecrãs

| ID | Tabela | Coluna | Vocabulário | Estado |
|----|--------|--------|-------------|--------|
| FD-20 | Cotações | `Data` (col. 2) | passado | ✅ ao lado do filtro de estado que já existia |
| FD-21 | Guias de Remessa | `Data` (col. 2) | passado | ✅ idem |
| FD-22 | Lotes & Validades | `Validade` (col. 4) | **validade** | ✅ ganhou também a pesquisa livre — não tinha filtro nenhum |
| FD-23 | Diário da Contabilidade | `Data` | — | ❌ **declarado**: paginado no servidor; filtro de cliente só veria a página carregada. Precisa de `from`/`to` na API |
| FD-24 | Colaboradores | `Admissão` | — | ⚖️ **julgado desnecessário**: um quadro de pessoal é um registo, procura-se por nome |

## 4. DRY

| ID | Verificação | Estado |
|----|-------------|--------|
| FD-30 | Um só `matchesPeriod` para os dois vocabulários | ✅ cinco casos novos num `switch` que já existia |
| FD-31 | Nenhum painel faz *parse* de datas por si | ✅ `TableFilter.parseCellDate` |
| FD-32 | Nenhuma segunda classe de filtro | ✅ `PeriodFilter` é a mesma dos 17 painéis anteriores |

---

## 5. Declarações honestas

- **A lógica está testada; a ligação ao ecrã foi vista, não testada.** Os 4 testes cobrem
  `matchesPeriod`, que é onde um engano esconde um lote a vencer. Que a coluna 2 das Cotações é a
  data e a 4 dos Lotes é a validade foi confirmado a olho nas fotografias dos ecrãs — um índice
  errado não parte nada, filtra a coluna errada em silêncio.
- **O `Já vencidos` não substitui o alerta.** O separador *Alertas* do Stock continua a ser o sítio
  onde a validade a expirar se impõe a quem não foi à procura. Este filtro serve quem já lá está.
- **Não foi medido com muitos dados.** O filtro é de cliente, sobre as linhas carregadas; numa
  tabela com paginação de cliente isso é o conjunto todo, mas nunca foi corrido com milhares de
  lotes.
