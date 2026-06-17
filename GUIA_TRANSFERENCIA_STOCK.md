# Guia de Transferência de Stock — Playbook (origem → destino)

> Como a **Guia de Transferência** tira stock de um armazém e o põe noutro, camada a camada,
> e como **confirmar que funciona** de ponta a ponta. Segue a arquitectura do sistema
> ([ARCHITECTURE.md](ARCHITECTURE.md)). Visão geral dos movimentos em
> [MOVIMENTOS_COMERCIAIS.md](MOVIMENTOS_COMERCIAIS.md).

**Última actualização:** 2026-06-16

---

## 0. Estado actual — IMPORTANTE, lê primeiro

A guia tem **aprovação obrigatória antes de o stock sair**. O ciclo é:

```
criar guia  →  PENDING_APPROVAL  ──aprovar──▶ APPROVED  (stock movido)
                      │
                      ├──rejeitar──▶ REJECTED   (stock NÃO movido)
                      └──cancelar──▶ CANCELLED  (stock NÃO movido)
```

1. **Criar** ([StockTransferService.create()](src/main/java/com/phcpro/modules/inventory/service/StockTransferService.java)):
   persiste a guia como `PENDING_APPROVAL`, regista as linhas (produto + quantidade), **não move
   stock**. Faz uma verificação rápida de disponibilidade para não criar guias impossíveis de aprovar.
2. **Aprovar** (`approve()`, só perfil **MANAGER/ADMIN**): aí sim, **numa transacção atómica**:
   - consome o stock da **origem** por FEFO (validade mais próxima primeiro);
   - cria/abastece os **mesmos lotes** no **destino**;
   - grava **dois** `StockMovement` `TRANSFER` por lote (− origem, + destino) → aparecem na
     aba **Movimentos & Rastreabilidade**;
   - ajusta a tabela agregada `Stock` dos dois armazéns;
   - se faltar stock, **rollback** total e `BusinessRuleException`.
3. **Rejeitar / Cancelar**: encerram a guia sem qualquer efeito no stock.

A lógica do movimento está em
[StockTransferService.moveProduct()](src/main/java/com/phcpro/modules/inventory/service/StockTransferService.java),
chamada **apenas** a partir de `approve()`.

---

## 1. Arquitectura da feature (camada a camada)

Respeita a regra do projecto: Controller → Service → Repository, DTO na fronteira,
`@Entity` nunca sai para fora, `BusinessRuleException` para regras de negócio.

| Camada       | Ficheiro                                                                                                       | Papel |
|--------------|----------------------------------------------------------------------------------------------------------------|-------|
| Model        | [StockTransfer](src/main/java/com/phcpro/modules/inventory/model/StockTransfer.java) + [StockTransferLine](src/main/java/com/phcpro/modules/inventory/model/StockTransferLine.java) | Cabeçalho (origem, destino, responsável, viatura) + linhas |
| DTO entrada  | [CreateStockTransferRequest](src/main/java/com/phcpro/modules/inventory/dto/CreateStockTransferRequest.java) + [CreateStockTransferLineRequest](src/main/java/com/phcpro/modules/inventory/dto/CreateStockTransferLineRequest.java) | `@NotNull`, `@NotEmpty`, `@Positive` |
| DTO saída    | [StockTransferDTO](src/main/java/com/phcpro/modules/inventory/dto/StockTransferDTO.java) | O que o UI/REST recebe |
| Service      | [StockTransferService](src/main/java/com/phcpro/modules/inventory/service/StockTransferService.java) | `@Transactional create(...)` — orquestra o movimento |
| FEFO/lotes   | [ProductBatchService.consumeFEFO()](src/main/java/com/phcpro/modules/inventory/service/ProductBatchService.java#L64) + `addToBatch(...)` | Baixa lotes na origem, cria no destino |
| Repository   | StockTransferRepository · StockRepository · StockMovementRepository · ProductBatchRepository | Persistência |
| Numeração    | [DocumentSeries.STOCK_TRANSFER](src/main/java/com/phcpro/modules/numbering/service/DocumentSeries.java) = `TRF` | Nº de guia sequencial sem saltos |
| Controller   | [StockTransferController](src/main/java/com/phcpro/modules/inventory/controller/StockTransferController.java) | `POST/GET /api/inventory/transfers` |
| GUI (Swing)  | [StockPanel](src/main/java/com/phcpro/gui/StockPanel.java) — aba "Transferências entre Armazéns" | Botão "Nova Transferência" + "Imprimir Guia" |
| PDF          | [StockTransferPrintService](src/main/java/com/phcpro/modules/printing/StockTransferPrintService.java) | Imprime a "Guia de Transferência" |

### Fluxo do movimento

```
Operador → "Nova Transferência" (StockPanel)
         → CreateStockTransferRequest { origem, destino, linhas[] }
         → StockTransferController.create()           POST /api/inventory/transfers
         → StockTransferService.create()              [@Transactional]
             guardas: origem ≠ destino · mesma empresa · produto existe · stock disponível
             grava guia PENDING_APPROVAL + linhas (SEM mover stock)
         → guia fica pendente

MANAGER/ADMIN → "Aprovar" (StockPanel)
         → StockTransferController.approve()           POST /api/inventory/transfers/{id}/approve
         → StockTransferService.approve()              [@Transactional, guarda de perfil]
             para cada linha → moveProduct():
                 consumeFEFO(produto, ORIGEM, qty)     → baixa lotes na origem
                 addToBatch(produto, DESTINO, lote…)   → cria lote no destino
                 StockMovement TRANSFER  (−qty) origem  ┐ rastreáveis na aba
                 StockMovement TRANSFER  (+qty) destino  ┘ "Movimentos"
                 adjustStock(−qty) origem ; adjustStock(+qty) destino
             status → APPROVED, approvedBy, approvedAt
         → COMMIT (tudo ou nada)

(em alternativa) "Rejeitar" → reject(id, motivo) → REJECTED, sem mover stock
```

---

## 2. Pré-condições para funcionar

- [ ] Existem **≥ 2 armazéns** na mesma empresa ([Warehouse](src/main/java/com/phcpro/modules/inventory/model/Warehouse.java)).
- [ ] O produto tem **stock em lotes na origem** (`ProductBatch` com `quantity > 0`). ⚠️ Ver §4 — stock só agregado sem lote **não** transfere.
- [ ] Empresa activa no `CurrentUserContext` (sessão multi-empresa).

---

## 3. Passo a passo — verificar de ponta a ponta ("até funcionar")

Executa por ordem. Em cada passo está o critério de sucesso.

1. **Compilar** (a verdade do projecto — diagnostics Lombok do IDE são ruído):
   ```
   mvn clean compile
   ```
   ✅ `BUILD SUCCESS`.

2. **Arranque** — backend ou desktop:
   ```
   mvn spring-boot:run                              # backend (8080)
   mvn spring-boot:run -Dspring-boot.run.profiles=desktop   # desktop Swing
   ```

3. **Preparar dados**: 2 armazéns (ex.: "Loja", "Depósito") + 1 produto com **lote e stock**
   na origem. No desktop: StockPanel → "Adicionar Lote/Validade".

4. **Anotar stock ANTES** (StockPanel → aba "Níveis"): origem `X`, destino `Y`.

5. **Criar a guia**:
   - Desktop: aba "Transferências entre Armazéns" → "Nova Transferência" → origem, destino, produto, quantidade `q`.
   - OU REST:
     ```
     POST /api/inventory/transfers
     { "companyId":1, "originWarehouseId":1, "destinationWarehouseId":2,
       "responsible":"João", "lines":[ { "productId":10, "quantity":5 } ] }
     ```
   ✅ Devolve `StockTransferDTO` com `transferNumber` `TRF-2026/…` e `status: PENDING_APPROVAL`.

6. **Confirmar que o stock NÃO mudou ainda**: origem ainda `X`, destino ainda `Y`. ← o stock
   só sai na aprovação.

7. **Aprovar a guia** (com perfil MANAGER/ADMIN):
   - Desktop: selecionar a guia → "Aprovar".
   - OU REST: `POST /api/inventory/transfers/{id}/approve`.
   ✅ `status: APPROVED`. (Com perfil sem permissão → erro de autorização.)

8. **Confirmar o stock DEPOIS da aprovação**: origem = `X − q`, destino = `Y + q`. ← **teste principal.**

9. **Confirmar o ledger** (aba "Movimentos & Rastreabilidade" / `stock_movements`): dois registos
   `TRANSFER` por lote — um negativo (origem) e um positivo (destino), com a descrição da guia.

10. **Confirmar lotes**: o lote FEFO desceu na origem e apareceu/cresceu no destino, com a
    **mesma validade**.

11. **Imprimir a Guia** → "Imprimir Guia" → PDF com origem, destino, linhas e nº `TRF`.

12. **Caso rejeição** (`POST …/reject` com motivo, ou botão "Rejeitar"): ✅ `status: REJECTED`
    e **stock inalterado**.

13. **Caso negativo** (aprovar guia sem stock suficiente): ✅ erro
    *"Stock insuficiente de '…' no armazém '…'"* e **stock inalterado** (rollback).

---

## 4. Regras de negócio garantidas

- **Aprovação obrigatória**: a guia nasce `PENDING_APPROVAL`; o stock só sai em `approve()`.
- Só **MANAGER/ADMIN** aprovam ou rejeitam (`requireApproverRole()` em StockTransferService).
- Rejeição exige **motivo**; rejeitar/cancelar nunca move stock.
- Origem ≠ destino; ambos os armazéns da mesma empresa da guia.
- Quantidade `@Positive`; lista de linhas `@NotEmpty`.
- Verificação de disponibilidade na **criação** (falha cedo) e FEFO autoritativo na **aprovação**.
- FEFO na origem (validade mais próxima primeiro), pode atravessar vários lotes.
- Lote e **validade replicados** no destino (rastreabilidade mantida).
- **Atomicidade**: qualquer falha numa linha faz rollback de toda a aprovação.

---

## 5. Diagnóstico — "o stock não está a sair"

| Sintoma | Causa provável | Onde olhar |
|---------|----------------|------------|
| Erro "Stock insuficiente" mesmo havendo stock | Produto tem stock **agregado** (`Stock`) mas **sem lote** (`ProductBatch`) — FEFO não vê o que não está em lote | `consumeFEFO` em [ProductBatchService](src/main/java/com/phcpro/modules/inventory/service/ProductBatchService.java#L64); migrar via `ensureLegacyBatch(...)` |
| Stock não sai depois de criar a guia | É **esperado** — só sai na aprovação. Aprovar a guia | botão "Aprovar" / `POST …/{id}/approve` |
| "Apenas perfis MANAGER ou ADMIN podem aprovar…" | Sessão com perfil insuficiente | `CurrentUserContext.getRole()` / login do operador |
| Stock baixa na origem mas não sobe no destino (ou vice-versa) | Transacção não está a fazer commit / exceção engolida | `@Transactional` em `approve()`; ver logs do `GlobalExceptionHandler` |
| Níveis no UI não mudam mas a BD sim | UI não refrescou | recarregar aba "Níveis" / `loadStockLevels()` em StockPanel |
| Nº de guia repetido/erro de unicidade | `DocumentNumberService` / série `TRF` | [DocumentSeries](src/main/java/com/phcpro/modules/numbering/service/DocumentSeries.java) |
| 400 no POST | Validação do DTO (origem=destino, qty ≤ 0, linhas vazias) | mensagem do `BusinessRuleException` |

---

## 6. Endurecimento profissional (melhorias opcionais)

Prioriza só se o negócio pedir; nenhuma é bloqueante para funcionar.

1. **Stock legado sem lote** — antes do `consumeFEFO`, chamar `ensureLegacyBatch` para produtos
   migrados sem rastreio de lote, evitando o falso "stock insuficiente". *(maior risco real)*
2. ✅ **Teste unitário** [StockTransferServiceTest](src/test/java/com/phcpro/modules/inventory/service/StockTransferServiceTest.java)
   (Mockito, **9/9 verde**): `create()` fica pendente sem mover stock; `approve()` move stock e
   grava 2 movimentos; `approve()` com perfil sem permissão rejeita e não move stock; guia já
   aprovada não re-aprova; `reject()`/`cancel()` não movem stock.
3. ✅ **`TransferStatus` enum** — o estado é o enum
   [TransferStatus](src/main/java/com/phcpro/modules/inventory/model/TransferStatus.java)
   (`PENDING_APPROVAL/APPROVED/REJECTED/CANCELLED`), com rótulo PT para UI/PDF — consistente com
   `InvoiceStatus`/`NoteStatus`.
4. **Reversão de guia aprovada** — `cancel()` só cobre guias pendentes; uma guia **aprovada** por
   engano não se estorna. Adicionar reversão simétrica (movimentos `REVERSAL`) se necessário.
5. **Estado "em trânsito"** — se a mercadoria viaja fisicamente, separar a saída da origem
   (na aprovação) da entrada no destino (numa recepção): `APPROVED → IN_TRANSIT → RECEIVED`.
   *Decisão de negócio — perguntar antes (regra 7 do [CLAUDE.md](CLAUDE.md)).*

---

## 7. Definição de pronto (checklist)

- [x] `mvn clean compile` verde.
- [x] Criar guia → `PENDING_APPROVAL`, **sem** mover stock.
- [x] Só MANAGER/ADMIN aprovam/rejeitam.
- [ ] Aprovar move stock: origem `−q`, destino `+q` (§3 passo 8) — *validar manualmente*.
- [ ] Dois `StockMovement TRANSFER` por lote visíveis na aba Movimentos — *validar manualmente*.
- [ ] Lote + validade replicados no destino — *validar manualmente*.
- [ ] Aprovar sem stock suficiente faz rollback e devolve mensagem clara — *validar manualmente*.
- [ ] Rejeitar/Cancelar não movem stock — *validar manualmente*.
- [ ] PDF da Guia imprime com nº `TRF` e ambos os armazéns — *validar manualmente*.
