# Spec — Importação de dados de outro ERP

Status: **aprovada**
Effective date: 2026-11-18
Harness: [IMPORTACAO_DADOS_HARNESS.md](IMPORTACAO_DADOS_HARNESS.md)
Goals: [GOAL-006](../tasks/goals/GOAL-006-importar-cadastros.md) (cadastros) ·
[GOAL-007](../tasks/goals/GOAL-007-importar-saldos-iniciais.md) (saldos iniciais)

## 1. Problema

Uma empresa que vem do Primavera, do PHC ou de folhas Excel não muda de sistema se tiver de
reescrever à mão centenas de clientes, artigos e dívidas em aberto. Hoje o sistema só importa
extractos bancários. Sem importação não há migração de clientes.

## 2. Decisões do responsável (2026-11-18)

| Tema | Decisão |
|---|---|
| Divisão | Dois goals: cadastros (GOAL-006) e saldos iniciais (GOAL-007). Os saldos exigem os cadastros já importados. |
| Formato | Só **CSV**, com modelo descarregável pelo sistema. Sem biblioteca nova (sem Excel `.xlsx`). |
| Duplicados | Registo que já existe é **ignorado e reportado** ("já existe"); nunca é alterado. |
| Erros | **Tudo ou nada**: com qualquer erro no ficheiro nada é gravado; a pré-visualização lista os erros por linha. |
| Cliente | **Email opcional**, **NUIT obrigatório** e válido. O registo manual de cliente passa também a aceitar email vazio. |
| Categoria / IVA | Categoria inexistente é **criada** (e anunciada na pré-visualização); taxa de IVA inexistente é **erro**. |
| Stock inicial | Movimento `ENTRY` com custo, pelo ledger de stock, só se o artigo **não tem movimentos nesse armazém**. Sem lançamento contabilístico (igual ao stock inicial manual de hoje). |
| Contas correntes | **Um documento por factura em aberto**, série própria, não fiscal. |

## 3. Regras comuns (GOAL-006 e GOAL-007)

### 3.1 Fluxo

1. O utilizador descarrega o **modelo CSV** da entidade (`GET`): UTF-8 com BOM, separador `;`
   (abre bem no Excel em português) e **apenas a linha de cabeçalho**.
2. Envia o ficheiro para **pré-visualizar** (`POST .../preview`). Nada é gravado. A resposta diz, por
   linha: `NOVO`, `JÁ EXISTE`, `ASSOCIADO` (só clientes, ver §4.1) ou `ERRO` com a coluna e a mensagem
   em PT-MZ; e no topo: totais por estado e categorias que serão criadas.
3. Envia o mesmo ficheiro para **confirmar** (`POST .../commit`). O servidor **volta a validar tudo**
   (não confia na pré-visualização) e:
   - com algum `ERRO` recusa com `BusinessRuleException` e não grava nada;
   - sem erros grava todas as linhas `NOVO`/`ASSOCIADO` numa **só transacção** e devolve os totais.
4. Cada confirmação gera **um** registo de auditoria `DATA_IMPORT` com entidade, nome do ficheiro e
   totais (novos, associados, já existentes).

### 3.2 Ficheiro

- Codificação: UTF-8 (com ou sem BOM); se não for UTF-8 válido, lê-se como **Windows-1252**
  (exportações do Excel e do Primavera). Acentos têm de chegar intactos nos dois casos.
- Separador: detectado na linha de cabeçalho — `;` ou `,`. Campos entre aspas (`"..."`) podem conter
  o separador e aspas duplicadas (`""`).
- Cabeçalho obrigatório; colunas reconhecidas pelo nome do modelo, sem distinguir maiúsculas,
  acentos nem espaços nas pontas. Coluna obrigatória em falta é erro do ficheiro inteiro; colunas a
  mais são ignoradas e anunciadas.
- Linhas vazias são ignoradas. Os números de linha reportados contam o cabeçalho como linha 1.
- Limites: **2 MB** e **5 000 linhas** de dados; acima disso o ficheiro é recusado.
- Números: se o valor tem vírgula, a vírgula é decimal e os pontos são separadores de milhar
  (`1.234,56`); sem vírgula, o ponto é decimal (`1234.56`). Valor não numérico é erro da linha.
- Datas: `dd/mm/aaaa` ou `aaaa-mm-dd`. Outra forma é erro da linha.
- Texto: espaços nas pontas são retirados.

### 3.3 Validação e escrita

- Cada linha passa pelas **mesmas validações do registo manual** (NUIT pelo `TaxIdValidator`,
  preços, campos obrigatórios). A importação não tem regras mais fracas do que o ecrã.
- Duas linhas do **mesmo ficheiro** com a mesma chave (§4/§5) são `ERRO` em ambas.
- Tudo na **empresa activa** (`X-Company-Id`); nada toca noutra empresa.
- A escrita passa pelos serviços donos de cada agregado (clientes pelo comercial, fornecedores pelas
  compras, stock pelo ledger do inventário). O módulo de importação **não lê nem escreve Repository
  de outro módulo** (ver `docs/MODULE_DECOUPLING_SPEC.md`).
- Só o perfil **ADMIN** pode descarregar modelos, pré-visualizar e confirmar.

### 3.4 Desktop

- Um ecrã "Importar dados" em Configurações: escolher a entidade, descarregar o modelo, escolher o
  ficheiro, ver a pré-visualização numa tabela (linha, estado, coluna, mensagem) e confirmar.
- O botão **Confirmar** só fica activo quando a pré-visualização não tem erros.
- Segue o padrão de tabelas, tipografia e diálogos do projecto; ícones por `UIHelper.icon`.

## 4. Cadastros (GOAL-006)

### 4.1 Clientes — chave: NUIT

Colunas: `nome`\*, `nuit`\*, `email`, `morada`, `prazo_pagamento_dias`, `limite_credito`.

- NUIT obrigatório e válido; email opcional (se vier, tem de ser válido).
- `prazo_pagamento_dias` vazio usa o prazo por omissão do registo manual; `limite_credito` vazio fica
  sem limite, igual ao registo manual.
- NUIT já existe **nesta empresa** → `JÁ EXISTE`, ignorado.
- NUIT existe **noutra empresa** → `ASSOCIADO`: o cliente partilhado é ligado à empresa activa sem
  alterar os seus dados (mesma regra do registo manual em `ComercialClientAndCreditService`).
- Email passa a opcional no cliente: migração Flyway que retira o `NOT NULL` de `clients.email`,
  `CreateClientRequest` sem `@NotBlank` no email, e o formulário de cliente do desktop aceita vazio.

### 4.2 Fornecedores — chave: NUIT na empresa

Colunas: `nome`\*, `nuit`\*, `email`, `telefone`, `morada`, `contacto`.

- NUIT obrigatório e válido; email opcional.
- NUIT já existe nesta empresa → `JÁ EXISTE`.

### 4.3 Artigos — chave: SKU

Colunas: `sku`\*, `nome`\*, `preco_venda`\*, `iva`\*, `categoria`\*, `codigo_barras`, `referencia`,
`preco_grosso`, `unidade`, `descricao`.

- `iva`: percentagem (ex.: `16`) ou código de uma taxa **já existente** na empresa. Inexistente ou
  ambígua (duas taxas com a mesma percentagem e sem código) → `ERRO`.
- `categoria`: pelo nome, sem distinguir maiúsculas/acentos. Inexistente → criada na confirmação e
  listada na pré-visualização como "categorias a criar".
- `unidade` vazia usa a unidade base por omissão do registo manual.
- SKU já existe → `JÁ EXISTE`. Código de barras ou referência já usados por outro artigo → `ERRO`
  (são únicos no sistema).
  - "Já existe" e "outro artigo" medem-se em **todo o sistema** (as três colunas são únicas na base):
    o SKU de um artigo de outra empresa também é `JÁ EXISTE` e não é associado (`ASSOCIADO` é só de
    clientes, §3.1). "Outro artigo" é um artigo com SKU diferente; o código de barras/referência do
    próprio artigo existente não é conflito.
  - Código de barras ou referência de outro artigo é `ERRO` mesmo que o SKU já exista.
  - Código de barras ou referência repetidos no **mesmo ficheiro** são `ERRO` em ambas as linhas, como a
    chave (§3.3), porque não podem entrar os dois.
  - Só a categoria de uma linha `NOVO` entra em "categorias a criar".
- A categoria criada na confirmação recebe como código o nome sem acentos, em maiúsculas, com `_` no lugar
  de espaços e símbolos (até 30 caracteres: `Lacticínios Frescos` → `LACTICINIOS_FRESCOS`), pelo serviço de
  categorias do comercial. Se esse código já pertencer a outra categoria da empresa, a confirmação falha e,
  por ser uma só transacção, nada fica gravado.

## 5. Saldos iniciais (GOAL-007)

Pré-requisito: os clientes, fornecedores e artigos referidos já existem na empresa activa. Referência
a um que não existe é `ERRO` da linha ("importe primeiro os clientes").

### 5.1 Stock inicial — chave: SKU + armazém + lote

Colunas: `sku`\*, `armazem`\*, `quantidade`\*, `custo_unitario`\*, `lote`, `validade`.

- `armazem`: código ou nome de um armazém da empresa.
- `quantidade` > 0; `custo_unitario` ≥ 0.
- Lote e validade seguem as regras da entrada manual (artigo que exige validade sem `validade` → `ERRO`).
- Artigo que **já tem movimentos nesse armazém** → `ERRO` ("já tem stock neste armazém; use um
  ajuste"). Assim reimportar o mesmo ficheiro nunca duplica stock.
- Grava pelo ledger do inventário um movimento `ENTRY` por linha, com lote e saldo, como o stock
  inicial manual. **Sem lançamento contabilístico**.

### 5.2 Dívidas de clientes — documento "Saldo Inicial" (série **SI**)

Colunas: `nuit_cliente`\*, `numero_original`\*, `data`\*, `vencimento`\*, `valor_em_divida`\*, `descricao`.

- Uma linha = uma factura em aberto do sistema antigo. Chave: cliente + `numero_original`.
  Já importada → `JÁ EXISTE`.
- `valor_em_divida` > 0; `vencimento` ≥ `data`; `data` não pode ser futura.
- Numeração própria **SI** por empresa (`SI-2026/1`, …); `numero_original` fica guardado e visível.
- **Não é documento fiscal**: sem IVA, fora do SAF-T, das declarações de IVA e dos relatórios de
  vendas/receita.
- Entra no **extracto de conta corrente** como débito na `data`, na **antiguidade de saldos** pelo
  `vencimento`, na exposição de **limite de crédito** e na **mora** como qualquer factura.
- Liquida-se com **recibos normais**, total ou parcialmente, como uma factura.
- **Sem lançamento contabilístico automático** (a abertura contabilística é feita pelo contabilista).

### 5.3 Dívidas a fornecedores — documento "Saldo Inicial Fornecedor" (série **SIF**)

Colunas: `nuit_fornecedor`\*, `numero_original`\*, `data`\*, `vencimento`\*, `valor_em_divida`\*, `descricao`.

- Mesmas regras de chave, valores, datas e numeração do §5.2 (série **SIF**).
- Não gera entrada de stock nem documento fiscal.
- Aparece na lista de **contas a pagar** (`/api/purchases/payables`) e paga-se com a mesma acção de
  pagamento a fornecedor, total ou parcialmente, com a mesma conta financeira.
- Sem lançamento contabilístico automático.

## 6. Fora de âmbito

- Ficheiros Excel `.xlsx`, ligação directa à base de dados do Primavera/PHC e mapeamento livre de colunas.
- Actualizar registos existentes a partir do ficheiro.
- Importar documentos históricos já pagos, movimentos de stock passados, funcionários, salários ou
  activos.
- Lançamentos contabilísticos de abertura.
- Anular uma importação inteira de uma vez (cada registo anula-se pelos meios normais).
