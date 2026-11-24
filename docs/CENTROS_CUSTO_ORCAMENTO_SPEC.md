# Spec — Centros de custo e orçamento

Status: **aprovada**
Effective date: 2026-11-19
Harness: [CENTROS_CUSTO_ORCAMENTO_HARNESS.md](CENTROS_CUSTO_ORCAMENTO_HARNESS.md)
Goals: [GOAL-010](../tasks/goals/GOAL-010-centros-de-custo.md) (centros de custo) ·
[GOAL-011](../tasks/goals/GOAL-011-orcamento.md) (orçamento)

## 1. Problema

O gestor e o contabilista querem saber quanto cada loja, armazém ou departamento ganha e gasta, e
comparar com o planeado. Hoje a contabilidade só tem contas: não há como separar a renda da loja da
Baixa da renda do armazém do Porto, nem orçamento contra o qual medir.

## 2. Decisões do responsável (2026-11-19)

| Tema | Decisão |
|---|---|
| O que é um centro | Cada **armazém/loja** é automaticamente um centro; o gestor cria **centros manuais** (Administração, Frota, Produção…). |
| Obrigatoriedade | **Obrigatório nas linhas das classes 6 e 7**; não se usa nas classes 1–5. Automáticos herdam do armazém; manuais e despesas pedem-no. |
| Orçamento | **Anual, por conta × centro × mês**, contas das classes 6 e 7; relatório real vs orçado. |
| Excesso | **Só alerta**: grava normalmente; aviso a 90% e 100% do orçamento do mês. |

## 3. Centros de custo (GOAL-010)

### 3.1 Cadastro

- `CostCenter` por empresa: `code` (único na empresa), `name`, `type` (`WAREHOUSE` ou `MANUAL`),
  armazém ligado (só `WAREHOUSE`), `active`.
- Criar um armazém cria o seu centro (`code` = código do armazém); armazéns existentes recebem centro
  pela migração. Centro de armazém não se edita à mão: acompanha o nome e o estado do armazém.
  Se o número do armazém falta ou já está ocupado na empresa, o código é `ARM-<id do armazém>`; o código
  não muda depois (só nome e estado acompanham). Desactivar o armazém cujo centro era o centro por
  omissão devolve o "por omissão" ao `ADM`.
- Cada empresa recebe um centro manual **`ADM` — Administração**, que é o **centro por omissão**
  (alterável nas Configurações para outro centro activo). Há sempre um e só um por empresa: o centro
  por omissão não se desactiva nem se apaga sem antes escolher outro; empresa nova recebe o `ADM` ao
  primeiro uso, empresas existentes pela migração.
- Centros manuais: criar, renomear, desactivar. Não se apaga um centro com lançamentos.
- Centro inactivo não aceita lançamentos novos.
- Gerir centros: **ADMIN** e **MANAGER**. Consultar: quem vê a contabilidade.
  Concretização (CC-09): `/api/accounting/cost-centers` — `GET` lista, `POST` cria, `PUT /{id}` altera,
  `PUT /{id}/default` torna-o o centro por omissão, `DELETE /{id}` apaga; relatório em
  `GET /api/accounting/cost-center-result?from&to`. Área da contabilidade, logo só ADMIN/MANAGER; tudo na empresa
  activa (centro de outra empresa responde "não encontrado nesta empresa"). Listar é "primeiro uso": a lista
  traz sempre o centro por omissão (cria o `ADM` se a empresa ainda não tiver nenhum).

### 3.2 Marcação nos lançamentos

- `JournalLine.costCenter`: **obrigatório** nas linhas de contas das classes **6** e **7**, e
  **proibido** nas classes 1–5 (`BusinessRuleException` no domínio do lançamento). A regra aplica-se na
  porta única de gravação (`JournalService.save`), que antes de validar completa com o centro por omissão
  as partidas 6/7 que chegam sem centro; um centro numa partida das classes 1–5 é recusado, nunca
  descartado em silêncio.
- Centro de outra empresa ou inactivo é recusado.
- Lançamentos automáticos: centro = centro do **armazém do documento de origem** (vendas, POS,
  compras, quebras, ajustes de stock, custos de importação). Evento sem armazém (salários,
  amortizações, combustível da frota, despesas de tesouraria sem centro) usa o **centro por omissão**.
  Concretização (CC-05): o evento leva o `warehouseId` e as partidas 6/7 sem centro ficam no centro activo desse
  armazém (`CostCenterService.postingCenterOf`); armazém sem centro activo da empresa cai no centro por omissão, para
  nunca bloquear a operação. Armazém de cada origem: venda/POS e nota de débito — o da fatura; nota de crédito — o da
  nota, senão o da fatura; quebra, ajuste, desmancha e lote de produção — o do movimento; ordem de produção — o
  consumo no armazém de origem e a variação de produção no de destino; diferença de câmbio do pagamento a fornecedor
  — o da compra; custos de importação — a parte já vendida (6101) repartida pelo armazém da compra de cada linha.
  A perda em trânsito (entre armazéns) usa o centro por omissão. A anulação de venda ou de recebimento estorna cada
  partida no centro da original (centro entretanto inactivo cede ao por omissão).
- Lançamento manual e despesa de tesouraria: o pedido aceita `costCenterId` nas linhas/na despesa;
  vazio numa linha de classe 6/7 usa o **centro por omissão**. O desktop pede o centro com o centro por
  omissão pré-seleccionado.
  A "despesa de tesouraria" é a **despesa do colaborador** (`CreateExpenseClaimRequest.costCenterId`), paga pela
  tesouraria ao aprovar e lançada a débito de 6201: o centro fica guardado na despesa e segue no
  `TreasuryPayoutEvent` para a partida de custo. O módulo de RH guarda só o id; quem valida o centro (existe, é da
  empresa, está activo) é a contabilidade ao lançar o reembolso — um centro que entretanto ficou inactivo faz
  falhar a aprovação com a mensagem do centro, sem pagamento nem lançamento.
- Linhas das classes 6/7 já existentes recebem o centro por omissão pela migração (V151).

### 3.3 Resultado por centro

- Relatório "Resultados por centro de custo" para um período: por centro, proveitos (classe 7,
  crédito − débito), custos (classe 6, débito − crédito) e resultado; mais a coluna "Total".
- O total do relatório é **igual** ao resultado da demonstração de resultados do mesmo período.
  Concretização (CC-07): `AccountingReportService.getCostCenterResult` lê as mesmas partidas que a demonstração e
  dá a cada valor o sinal da natureza da conta, como ela (no plano, classe 6 devedora e 7 credora — as fórmulas
  acima); uma linha por centro com movimentos nas classes 6/7, ordenada pelo código; partidas 6/7 sem centro
  (anteriores aos centros) aparecem numa linha "Sem centro", para o total bater sempre.
- Razão e balancete aceitam filtro opcional por centro.
  Concretização (CC-08): `costCenterId` opcional (`getLedger`/`getTrialBalance`, parâmetro `costCenterId` nos
  endpoints); vazio dá o razão/balancete completos, como antes. Com centro, só contam as partidas desse centro —
  também no saldo inicial do razão —, pelo que o balancete filtrado só tem contas das classes 6/7 e normalmente
  não fica equilibrado (os totais e o indicador "equilibrado" reflectem só as partidas filtradas). As
  partidas são sempre da empresa activa, logo um centro de outra empresa não mostra nada; um centro inactivo
  continua consultável (histórico).
- Desktop: ecrã em Contabilidade com o relatório (período, tabela por centro) e exportação pelos
  meios normais de tabela (PDF/Excel/CSV); gestão de centros em Configurações.
  Concretização (CC-UI-01): Configurações › "Centros de Custo" (último separador) lista código, nome, tipo
  (Manual/Armazém), estado e qual é o por omissão, e mostra o centro por omissão por cima da tabela. "Novo centro"
  e "Editar" (código, nome, estado) só para centros manuais; "Apagar" só manual e nunca o por omissão (com
  confirmação; o backend recusa se tiver lançamentos); "Tornar por omissão" só para centro activo que ainda não o
  é. Sem ADMIN/MANAGER os botões ficam desactivados; o backend é quem decide.
  Concretização (CC-UI-03): Contabilidade › "Resultados por centro" (última aba): período De/A (por omissão o mês
  corrente) e "Calcular"; tabela com código, centro, proveitos, custos e resultado, a linha "Sem centro" quando
  existir e, no fim, a linha "Total" (igual à demonstração de resultados), sem ordenação nem paginação para o Total
  ficar no fim; exportação PDF com os dados da empresa, Excel e CSV (menu de contexto da tabela).

## 4. Orçamento (GOAL-011)

### 4.1 Orçamento anual

- Um orçamento por empresa e ano, com linhas: conta **movimentável das classes 6 ou 7**, centro de
  custo activo, mês (1–12) e valor ≥ 0. Uma linha por conta × centro × mês.
  Concretização (OR-01): `BudgetService.save` recebe o ano inteiro (`SaveBudgetRequest`, linhas com código da conta,
  centro, mês e valor); células enviadas são criadas ou actualizadas e as omitidas saem. Valor zero é uma célula
  preenchida (conta para "só células vazias" no OR-02). Ano sem orçamento devolve grelha vazia.
- Editável a qualquer momento; cada gravação gera auditoria `BUDGET_CHANGE` com ano e totais.
- **Copiar do ano anterior**: preenche cada conta × centro × mês com o **real** do mesmo mês do ano
  anterior, com percentagem opcional de ajuste (ex.: +10%), arredondado a 2 casas. Só preenche células
  vazias, nunca sobrescreve.
  Concretização (OR-02): `BudgetService.copyFromPreviousYear` (`CopyBudgetRequest`: ano destino e ajuste % opcional,
  vazio = 0, nunca abaixo de −100%) lê o real do ano anterior por conta × centro × mês (sinal do §4.2, só partidas com
  centro), arredonda `real × (1 + ajuste/100)` a 2 casas (meio para cima) e **grava** as células em falta no orçamento
  do ano destino (criando-o se não existir). Ficam de fora: células já preenchidas (mesmo a zero), real ≤ 0 e centros
  hoje inactivos. Devolve o orçamento resultante.
- Gerir orçamento: **ADMIN** e **MANAGER**.

### 4.2 Real vs orçado

- Real = soma das linhas de lançamento da conta e centro no mês (classe 6: débito − crédito;
  classe 7: crédito − débito).
  Concretização (OR-03): `BudgetVarianceService.report` (ano, mês 1–12, acumulado sim/não, centro opcional; ADMIN e
  MANAGER) devolve uma linha por conta × centro com o **orçado** e o **real** do período — só o mês, ou de janeiro até
  ao mês quando acumulado — da empresa activa. Entram as contas × centros com orçamento **ou** com real no período
  (falta de um lado vale zero); partidas sem centro ficam de fora (não comparam com o orçamento); centros hoje
  inactivos continuam a aparecer com o seu histórico. Ordem: código da conta, depois código do centro.
- Relatório por centro e conta, para um mês e acumulado do ano: orçado, real, desvio (real − orçado)
  e desvio %. Desvio **desfavorável**: custo acima do orçado ou proveito abaixo; favorável no inverso.
  Orçado zero mostra desvio % vazio, não infinito.

### 4.3 Alertas

- Por centro e mês: quando o **real dos custos (classe 6)** atinge **90%** e **100%** do **orçado dos
  custos** desse centro nesse mês, gera-se **um** alerta por limiar, centro e mês (não repete).
- O alerta aparece no sino dos perfis ADMIN e MANAGER.
- Ao gravar uma compra ou despesa de tesouraria que faz o centro passar um limiar, a resposta traz um
  **aviso** que o desktop mostra como toast. **Nada é bloqueado**.

### 4.4 Desktop

- Contabilidade › Orçamento: escolher ano, grelha conta × centro com os 12 meses, copiar do ano
  anterior, gravar.
- Contabilidade › Real vs Orçado: mês ou acumulado, filtro por centro, desvios coloridos pelo
  vocabulário de estados do projecto (`StatusTone`).

## 5. Fora de âmbito

- Repartição automática de custos comuns entre centros (ex.: renda da sede pelas lojas).
- Centros de custo em documentos comerciais sem armazém escolhido à mão (o centro vem do armazém).
- Orçamento de tesouraria e de investimento; aprovação ou bloqueio por orçamento.
- Vários orçamentos/versões por ano.
