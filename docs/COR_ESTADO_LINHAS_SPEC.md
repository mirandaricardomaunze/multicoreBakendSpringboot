# Spec — Cor de estado nas linhas das tabelas

> Leitura rápida do estado de cada registo: a linha ganha um **tom subtil** (verde/amarelo/vermelho)
> conforme a sua situação. Só apresentação — sem tocar em regras, Services nem dados.

**Última actualização:** 2026-07-03

## Problema

As tabelas coloriam apenas o **texto** de algumas células de estado (APROVADO/VENCIDO). Faltava uma
leitura de estado **por linha** e consistente — num painel de stock ou de reposição, o operador tinha
de ler número a número para perceber o que está esgotado/em falta.

## Decisão

- `UIHelper.styleTable` passa a detectar automaticamente uma coluna chamada **"Estado"** (ou
  "Situação"/"Status") e a pintar a **linha inteira** com um tom subtil, **misturando** a cor
  semântica com o fundo zebra (`blend`, ~18%) — adapta-se ao tema claro/escuro.
- **Vocabulário semântico** centralizado em `statusColorFor(...)`, partilhado pelo texto da célula e
  pelo tom da linha:
  - **Verde** (APPROVED_GREEN): APROVADO/PAGO/RECEBIDO/ACTIVO/OK/EM STOCK…
  - **Vermelho** (REJECTED_RED): REJEITADO/CANCELADO/ANULADO/**ESGOTADO**/SEM STOCK/VENCIDO…
  - **Amarelo** (PENDING_YELLOW): PENDENTE/**BAIXO**/EM DÍVIDA/PARCIAL/VENCE EM BREVE…
- **Colunas "Estado" acrescentadas** onde faziam falta:
  - **Níveis de Stock:** ESGOTADO / BAIXO / EM STOCK (por `minStock`).
  - **Reposição:** ESGOTADO / BAIXO.
- **Automático e DRY:** qualquer tabela que já tenha (ou venha a ter) uma coluna "Estado" ganha o tom
  sem código extra (Faturas, Encomendas, Transferências, Fornecedores…).

## Não-objetivos

- Não alterar dados, cálculos, Services nem o modelo de colunas de tabelas sem estado.
- Não usar cores berrantes (fundo inteiro saturado) — o tom é subtil, mantém a zebra e a selecção.
- Não colorir por linha tabelas sem semântica de estado (ex.: log de movimentos mantém-se neutro).
