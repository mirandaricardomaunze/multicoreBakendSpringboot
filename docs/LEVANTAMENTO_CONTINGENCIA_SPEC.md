# Spec — Talão de levantamento sem internet (SA-C20)

**Data:** 2026-11-28 · **Estado:** aprovada pelo responsável · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Base:** `docs/MATERIAL_POR_LEVANTAR_SPEC.md` (talão LV) e `docs/POS_CONTINGENCIA_RESILIENCIA_SPEC.md` (fila local)
**Harness:** `docs/LEVANTAMENTO_CONTINGENCIA_HARNESS.md` · **Goal:** GOAL-018

## 1. Problema

Quando a internet cai no estaleiro, o cliente que já pagou o material não consegue levantá-lo: o talão LV
precisa do servidor. O camião fica parado ou sai sem papel nenhum.

## 2. Decisões do responsável (2026-11-28)

| Tema | Decisão |
|---|---|
| Documentos sem internet | **Só o talão de levantamento LV** (material já pago). Guias de remessa e faturas continuam a precisar de internet |
| Saldo sem internet | **Usa o último saldo conhecido** guardado no computador; não deixa levantar mais do que isso |
| Talão recusado ao sincronizar | **Vai para o gestor resolver**: fica "Para rever", aviso no sino; o gestor aceita, cobra a diferença ou anula. Nada se apaga sozinho |

O talão LV continua **não fiscal e sem mexer no stock** (MATERIAL_POR_LEVANTAR §Decisões 1 e 3).

## 3. Regras

### 3.1 Saldo guardado no computador

- Sempre que o ecrã Material por Levantar carrega com internet, o desktop guarda localmente, por empresa,
  as contas abertas: fatura, cliente, prazo, estado e saldo por linha (artigo, quantidade por levantar).
- Ficheiro local em `%LOCALAPPDATA%/MulticoreERP/data` (ou `~/.multicore/data`), gravado de forma atómica
  (escreve num temporário e troca), com a data/hora da última actualização.
- Sem internet, o ecrã mostra essas contas com o aviso "Sem ligação — saldos de {data/hora}".

### 3.2 Emitir talão provisório

- Só quando o servidor não responde (falha de rede/timeout), nunca quando o servidor recusa por regra de
  negócio. Erro de negócio com internet continua a ser mostrado como hoje.
- Limite por linha: **saldo guardado − quantidades dos talões provisórios ainda por sincronizar** dessa
  linha. Acima disso → recusado no campo, com mensagem PT-MZ.
- Conta fechada ou cancelada no saldo guardado → recusado. Prazo vencido (pelo prazo guardado) → só um
  utilizador de gestão com sessão aberta pode emitir, como hoje com internet.
- Cada talão provisório tem uma chave única `CONT-LV-YYYYMMDD-HHMMSS-XXXX` (sufixo aleatório seguro), que
  serve de `requestKey` ao sincronizar (idempotência já existente do talão LV).
- Guarda-se na fila local durável (mesmo formato atómico do POS): chave, empresa, conta, linhas,
  quem levanta, documento, viatura, utilizador, data/hora de emissão.
- Imprime-se no desktop, sem o servidor, um talão com o título
  `TALÃO PROVISÓRIO DE LEVANTAMENTO (SEM LIGAÇÃO)`, a chave, cliente, fatura, artigos e quantidades,
  quem levanta, viatura, data/hora e espaço para assinaturas, e o aviso
  `Documento provisório. Será confirmado quando a ligação voltar.`

### 3.3 Sincronizar

- Um serviço em segundo plano tenta a cada 45 segundos, por ordem de emissão (FIFO); há também
  "Sincronizar agora" no ecrã.
- Envia `POST /api/pickups/slips` com `requestKey` = chave e mais `offlineIssuedAt` e `offlineIssuedBy`.
  - **Aceite:** o servidor emite o talão LV normal (número da série `LV`) e guarda a chave e a data/hora
    real de emissão sem ligação; a fila marca `SYNCED` com o número LV. Reenviar a mesma chave devolve o
    mesmo talão (sem duplicar).
  - **Recusado por regra de negócio** (saldo insuficiente, conta fechada/cancelada, fatura anulada, prazo
    vencido sem gestão): o servidor **não emite** o talão; grava um **incidente de contingência**
    (`PickupContingencyIncident`, estado `REVIEW_NEEDED` "Para rever") com tudo o que veio do talão e o
    motivo da recusa, e avisa no sino os ADMIN/MANAGER. A fila marca `REVIEW_NEEDED`. Reenviar a mesma
    chave não cria segundo incidente.
  - **Falha de rede:** fica na fila e volta a tentar.
- O indicador "Sem ligação (N)" no ecrã mostra quantos talões esperam e abre a lista (estado de cada um).

### 3.4 O gestor resolve o incidente

Só ADMIN/MANAGER, sempre com nota/motivo, auditado (`PICKUP_INCIDENT_RESOLVE`). Três saídas:

- **Aceitar:** emite o talão LV agora, contra a mesma conta, se o saldo actual chegar. Se não chegar, é
  recusado com a mensagem do saldo (o saldo nunca fica negativo, MATERIAL_POR_LEVANTAR §Decisões 4).
- **Cobrar a diferença:** o gestor escolhe uma fatura emitida do mesmo cliente que cobre o material em
  excesso (feita por ele pelo circuito normal). O sistema abre/usa a conta de levantamento dessa fatura e
  emite os talões LV: na conta original o que o saldo ainda cobre, na conta da nova fatura o resto. Se as
  duas não cobrirem tudo → recusado.
- **Anular:** fecha o incidente sem talão (material não saiu, ou perda tratada à parte), com motivo.
- Estado final `RESOLVED` com a decisão, quem e quando. Resolver duas vezes é recusado.

### 3.5 Permissões e empresa

- Emitir provisório: os mesmos perfis que emitem o talão LV hoje.
- Sincronizar: o próprio desktop com a sessão do utilizador que emitiu, ou de qualquer utilizador com os
  mesmos perfis na mesma empresa.
- Ver e resolver incidentes: ADMIN/MANAGER. Sempre na empresa activa.

## 4. Fora de âmbito

- Entrar no sistema sem internet (a sessão tem de estar aberta antes da falha).
- Guias de remessa, faturas, apontamentos de produção sem internet.
- Mudanças ao stock: o LV continua a não mexer no stock.
