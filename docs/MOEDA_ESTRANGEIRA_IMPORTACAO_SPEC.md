# Spec — Compras em moeda estrangeira e custos de importação

Status: **aprovada**
Effective date: 2026-11-19
Harness: [MOEDA_ESTRANGEIRA_IMPORTACAO_HARNESS.md](MOEDA_ESTRANGEIRA_IMPORTACAO_HARNESS.md)
Goals: [GOAL-008](../tasks/goals/GOAL-008-compras-moeda-estrangeira.md) (moeda e câmbio) ·
[GOAL-009](../tasks/goals/GOAL-009-custos-de-importacao.md) (custos de importação)

## 1. Problema

Em Moçambique compra-se muito em USD, ZAR e EUR, e a mercadoria importada chega com frete,
direitos aduaneiros e despachante. Hoje tudo é em meticais e esses custos não entram no artigo:
o operador converte à mão, o custo médio fica abaixo do real e a margem aparece inflacionada.

## 2. Decisões do responsável (2026-11-19)

| Tema | Decisão |
|---|---|
| Âmbito | **Compras primeiro.** GOAL-008: compras e pagamentos a fornecedores em moeda estrangeira. GOAL-009: custos de importação. Vendas em moeda estrangeira ficam fora. |
| Moeda base | **Por empresa, MZN por omissão**; não muda depois de haver movimentos. |
| Câmbio | **Tabela diária** por empresa; o documento propõe o câmbio da data, o utilizador pode corrigir, e o valor usado fica **gravado no documento**. |
| Diferença cambial | **Lançamento automático** no pagamento, em contas novas **6601** (desfavorável) e **7601** (favorável). |
| Repartição | **Escolhida por documento**: por valor (omissão), por peso ou por quantidade. |
| Custos depois de vender | A parte do stock **ainda existente** sobe o custo médio; a parte **já vendida** vai a **CMV (6101)**. Vendas passadas não mudam. |
| IVA da importação | **IVA dedutível (2432)**, fora do custo. Direitos, frete e despachante entram no custo. |

## 3. Moeda e câmbio (GOAL-008)

### 3.1 Moeda base da empresa

- `Company.baseCurrency` (código ISO 4217), **MZN** por omissão; escolhida no registo da empresa.
- Empresas existentes ficam com **MZN** pela migração.
- Depois do primeiro documento ou movimento (compra, factura, movimento de stock) a moeda base
  **não pode ser alterada** (`BusinessRuleException`).
- Stock, custo médio, contabilidade, SAF-T, declarações de IVA e relatórios ficam **sempre na moeda base**.

### 3.2 Moedas e tabela de câmbios

- Moedas aceites: **MZN, USD, ZAR, EUR** (lista fechada; outras ficam fora de âmbito).
- Câmbio = quantas unidades da **moeda base** vale **1 unidade** da moeda estrangeira
  (ex.: `USD = 63,9000`), > 0, com 4 casas decimais.
- Um câmbio por empresa, moeda e data. Pode ser corrigido; documentos já gravados **não mudam**.
- Câmbio proposto para uma data = o da própria data, ou o da data anterior mais recente. Sem nenhum,
  o documento exige que o utilizador escreva o câmbio.
- Criar e corrigir câmbios: **ADMIN**. Consultar: quem pode registar compras.

### 3.3 Compra em moeda estrangeira

- Fornecedor pode ter **moeda habitual** (opcional), proposta em encomendas e compras novas.
- Encomenda (`PurchaseOrder`) e compra (`Purchase`) guardam `currency` e `exchangeRate` (fotografia).
  Documento na moeda base tem câmbio **1** e comporta-se como hoje; compras existentes migram para a
  moeda base com câmbio 1.
- Preços e totais das linhas ficam na **moeda do documento**; o documento guarda também o **total em
  moeda base** = `total × câmbio`, arredondado a 2 casas (HALF_UP).
- Custo unitário que entra no stock e no **custo médio** = `preço unitário × câmbio`, na moeda base.
- Lançamento automático da compra na moeda base, ao câmbio do documento (mesmas contas de hoje).
- Contas a pagar e extracto de fornecedor mostram a **moeda**, o **valor em dívida na moeda do
  documento** e o equivalente em moeda base.
- PDFs da encomenda e da compra mostram o código da moeda e o câmbio usado (detalhe em §3.3.1).

#### 3.3.1 PDFs da encomenda e da compra (FX-13)

Até aqui o sistema **não tinha** PDF da encomenda a fornecedor nem da compra (o "Imprimir" de
`COMPRAS_APROVISIONAMENTO_SPEC.md` Fase 3 ficou por fazer). FX-13 cria os dois, com os building blocks
de `printing` (cabeçalho da empresa, tabela de linhas, bloco de totais, `MoneyFormat`), um serviço
por documento:

- `GET /api/print/purchase-order/{id}` → "Encomenda a Fornecedor"; `GET /api/print/purchase/{id}` →
  "Compra a Fornecedor". `application/pdf`, sempre na **empresa activa** (documento de outra empresa
  é recusado como não encontrado). Perfis: os mesmos que consultam compras (MANAGER, ADMIN).
- Não são documentos fiscais emitidos pela empresa (a factura é do fornecedor; a encomenda não é
  fiscal): **sem** controlo de reimpressão nem marca "2.ª via".
- Conteúdo: cabeçalho da empresa, título, número do documento, data, fornecedor (nome e NUIT),
  armazém, linhas (artigo, quantidade, preço unitário, IVA %, total da linha) e totais (sem IVA,
  IVA, total), tudo na **moeda do documento** e com o código da moeda junto aos valores.
- Documento em moeda estrangeira mostra também "Moeda: USD · Câmbio: 63,9055" (câmbio gravado no
  documento, 4 casas) e "Total em moeda base: 6 390,55 MZN" (o `baseTotalAmount` gravado).
- Documento na moeda base mostra só "Moeda: MZN", sem câmbio nem total em moeda base.
- Prova: `ForeignCurrencyHttpIntegrationTest` lê o texto dos PDFs (encomenda e compra em USD; compra
  em MZN sem câmbio) e confirma o acesso da empresa activa.

### 3.4 Pagamento e diferença cambial

- O pagamento de uma compra em moeda estrangeira indica o **valor na moeda do documento** e o
  **câmbio do pagamento** (proposto pela tabela para a data do pagamento, editável).
- A conta de tesouraria é sempre na **moeda base**: sai `valor × câmbio do pagamento`.
- A dívida ao fornecedor baixa `valor × câmbio do documento` (proporcional nos pagamentos parciais).
  O pagamento que **liquida** a dívida baixa exactamente o saldo em moeda base que restava (sem
  resíduos de cêntimos). Nos parciais, a baixa é a diferença do saldo em moeda base antes e depois
  (pago acumulado × câmbio do documento, 2 casas HALF_UP), o mesmo saldo que contas a pagar mostram.
- Lançamento automático equilibrado:
  - Débito **2201** Fornecedores — valor ao câmbio do documento;
  - Crédito **1101/1201** (conta usada) — valor ao câmbio do pagamento;
  - diferença a débito de **6601** se o pagamento custou mais (perda), ou a crédito de **7601** se
    custou menos (ganho).
- Plano de contas: semear **66** "Custos financeiros" > **6601** "Diferenças de câmbio desfavoráveis"
  e **76** "Proveitos financeiros" > **7601** "Diferenças de câmbio favoráveis", também nas empresas
  que já têm plano, sem duplicar contas existentes.

## 4. Custos de importação (GOAL-009)

### 4.1 Documento "Custos de Importação" (série **CI**)

- Numeração `CI-aaaa/n` por empresa. Liga-se a **uma ou mais compras recebidas** (estado
  `COMPLETED`) da empresa. Uma compra pode receber vários documentos CI.
- Linhas de custo, cada uma com: **tipo** (`FRETE`, `SEGURO`, `DIREITOS_ADUANEIROS`, `DESPACHANTE`,
  `IVA_ADUANEIRO`, `OUTRO`), **fornecedor** (transportadora, despachante, Autoridade Tributária…),
  **moeda e câmbio** (regras do §3), **valor** e, quando o serviço tem IVA, a **taxa de IVA**
  existente na empresa.
- Cada linha gera uma **dívida ao seu fornecedor**, visível em contas a pagar e paga com a mesma
  acção de pagamento a fornecedor (com diferença cambial, §3.4).
- Como fica gravado (LC-02): a dívida é um documento em `purchases` do tipo `LANDED_COST` (como o
  Saldo Inicial Fornecedor), numerado `<nº do CI>-<n.º da linha>` (ex.: `CI-2026/3-2`), na moeda e ao
  câmbio da linha, com total = valor sem IVA + IVA do serviço (no IVA aduaneiro, o próprio valor) e o
  IVA no imposto do documento; sem armazém nem linhas, não mexe no stock e não publica evento de
  compra (o lançamento é o do CI, §4.4). O pagamento identifica-a como "Custos de Importação CI-…".
  Não é compra recebida: não pode ser ligada a outro CI. A taxa de IVA vem das taxas da empresa; o
  IVA aduaneiro não leva taxa (é recusado). Um CI sem linhas de custo é recusado.

### 4.2 O que entra no custo

- Entram no custo: `FRETE`, `SEGURO`, `DIREITOS_ADUANEIROS`, `DESPACHANTE`, `OUTRO` — sempre o valor
  **sem IVA**, convertido à moeda base.
- **Não entra**: `IVA_ADUANEIRO` e o IVA dos serviços — vão a **IVA dedutível (2432)** e à declaração
  de IVA.

### 4.3 Repartição

- Método escolhido por documento: **VALOR** (omissão — valor em moeda base de cada linha de compra),
  **PESO** (`netUnitWeightKg × quantidade`; se alguma linha não tem peso, é erro) ou **QUANTIDADE**.
- Arredondamento a 2 casas; o resíduo de cêntimos vai para a linha com maior quota, para a soma
  repartida ser exactamente o total. Quotas a 2 casas HALF_UP; a "maior quota" é a da maior base do
  método e, em empate, a primeira linha.
- **Pré-visualização** antes de gravar: por artigo, custo unitário original, custo acrescentado e
  custo unitário final.
- Como fica gravado (LC-08): `POST /api/purchases/landed-costs/preview` recebe o mesmo pedido da confirmação,
  aplica as mesmas validações (compras recebidas da empresa activa, linhas de custo) e devolve, em moeda base,
  o custo que entra nos artigos, o IVA dedutível, o total das dívidas e, por linha de compra, quota (custo
  acrescentado), parte para o stock, parte já vendida e médio antes/depois; não consome número, não cria
  dívidas nem muda o custo médio (o médio é simulado e acumula quando o mesmo artigo aparece em várias
  linhas, como na confirmação). `POST /api/purchases/landed-costs` confirma: CI, dívidas, fotografia e custo
  médio numa só transacção (um erro não deixa nada gravado). Perfis: MANAGER e ADMIN.

### 4.4 Efeito no stock e na contabilidade

- Para cada linha de compra com quantidade recebida `Q` e quota `C`:
  - `S` = stock actual do artigo nesse armazém;
  - parte para o stock = `C × min(1, S / Q)`; o resto (`C` − parte para o stock) é a parte já vendida.
- A parte para o stock **sobe o custo médio** do artigo: `novo médio = (S × médio + parte) / S`.
- A parte já vendida vai a **CMV (6101)**. **Vendas passadas não mudam** (o custo da linha de venda
  é fotografado, ver `MARGEM_CUSTO_HISTORICO_SPEC.md`).
- Como fica gravado (LC-06): o efeito aplica-se na confirmação do CI (a mesma transacção que grava as
  dívidas). `Q` é a quantidade da linha de compra; a base VALOR é o valor **sem IVA** da linha em moeda
  base (`preço × quantidade × câmbio da compra`). Parte para o stock a 2 casas HALF_UP; a parte já vendida
  é o resto da quota (as duas somam sempre `C`). O custo médio do projecto é **por artigo na empresa**
  (`WeightedAverageCost`), por isso o `S` da fórmula do médio é o stock do artigo em todos os armazéns da
  empresa (`novo médio = (stock da empresa × médio + parte) / stock da empresa`, 2 casas HALF_UP); com um
  só armazém coincide com a fórmula acima. Sem stock no armazém, a quota toda vai a CMV e o médio não muda.
  Quantidades de stock não mexem. Cada linha fica fotografada em `landed_cost_allocations` (quota, `Q`,
  `S`, parte para o stock, parte já vendida, médio antes e depois), que o lançamento (LC-07) e a
  pré-visualização (LC-08) lêem.
- Lançamento automático equilibrado, na moeda base:
  - Débito **3201** Mercadorias (parte para o stock) e **6101** CMV (parte já vendida);
  - Débito **2432** IVA dedutível (IVA aduaneiro e IVA dos serviços);
  - Crédito **2201** Fornecedores (total de cada linha com IVA).
- Como fica gravado (LC-07): um lançamento por CI, origem `LANDED_COST`, data e número do CI, feito na
  confirmação (evento `LandedCostConfirmedEvent`, ouvido pela contabilidade; não lança duas vezes nem sem
  plano de contas). 3201 e 6101 são as somas das partes fotografadas em `landed_cost_allocations`; uma linha
  de 2201 por dívida `<CI>-n`, pelo total com IVA em moeda base (o que se vai pagar); 2432 é a diferença
  (dívidas − 3201 − 6101), para o lançamento fechar mesmo com arredondamentos do câmbio. Valores zero não
  geram linha.
- Mesmos perfis que registam compras.

### 4.5 Desktop

- Compras: moeda e câmbio no formulário de encomenda e de compra; contas a pagar com a moeda.
- Pagamento a fornecedor: campo de câmbio quando a compra é estrangeira, com a diferença cambial
  mostrada antes de confirmar.
- Configurações: tabela de câmbios (data, moeda, câmbio) e moeda base da empresa (só leitura depois
  de haver movimentos). Separador "Câmbios e Moeda Base": novo câmbio (moeda estrangeira, data, câmbio)
  e corrigir câmbio (só o valor) por diálogo; a moeda base vem de `GET /api/exchange-rates/base-currency`
  (`currency`, `editable`) e muda por `PUT` no mesmo caminho. Mesma regra de perfis do §3.2: consultar
  MANAGER, criar/corrigir câmbios e mudar a moeda base só ADMIN (o ecrã desactiva os botões aos outros).
- Compras › Custos de Importação: escolher compras, linhas de custo, método, pré-visualização por
  artigo e confirmação. Padrão de tabelas, tipografia e diálogos do projecto; ícones por `UIHelper.icon`.
- Como fica (LC-UI-01): separador "Custos de Importação" em Compras. Card "Compras recebidas" só com compras
  `COMPLETED` com armazém (dívidas de CI e Saldo Inicial ficam de fora), com coluna de escolha, Nº, fornecedor,
  data, moeda e total em moeda base. Card "Linhas de custo" (Tipo, Fornecedor, Moeda, Câmbio, Valor sem IVA,
  IVA): linha nova pelo diálogo canónico com tipo, fornecedor, moeda e câmbio (o mesmo campo dos documentos de
  compra: câmbio proposto e editável), valor e taxa de IVA da empresa ("Sem IVA" por omissão; o IVA aduaneiro
  não leva taxa); remover a linha seleccionada. Método de repartição com Valor por omissão. "Pré-visualizar"
  chama `/preview` e mostra, por linha de compra, compra, artigo, quantidade recebida, custo unitário original,
  custo acrescentado e custo unitário final, mais o resumo (custo que entra nos artigos, IVA dedutível, total
  das dívidas). "Confirmar CI" só fica activo depois de uma pré-visualização dos dados actuais: mudar compras,
  linhas ou método apaga a pré-visualização. Sem compras escolhidas ou sem linhas, o aviso aparece no ecrã sem
  chamar o backend; os erros do backend aparecem no ecrã. Confirmado, mostra o número do CI e as dívidas
  criadas e limpa o formulário.

## 5. Fora de âmbito

- Facturas de venda, recibos e POS em moeda estrangeira.
- Contas de tesouraria (banco/caixa) em moeda estrangeira.
- Reavaliação cambial de saldos em aberto no fecho do período.
- Câmbio automático do Banco de Moçambique.
- Moedas além de MZN, USD, ZAR e EUR.
- Anular um documento CI depois de confirmado.
