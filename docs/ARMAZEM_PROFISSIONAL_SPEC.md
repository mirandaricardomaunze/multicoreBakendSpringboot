# Spec — Campos profissionais do armazém

> Enriquecer o cadastro de armazém com campos operacionais: activo, tipo, permite vendas (POS),
> responsável e telefone.

**Última actualização:** 2026-07-04

## Problema

O armazém só tinha nome, número, capacidade e localização. Faltava classificá-lo e controlar o seu
papel operacional — em especial distinguir uma **Loja** (que vende ao balcão) de um **Depósito** (que
não vende), e poder **desactivar** um local sem apagar o histórico.

## Decisão

`Warehouse` ganha (migração `V21`, defaults preservam o comportamento antigo):

- **`active`** (bool, default true) — armazém inactivo **não aparece nos fluxos operacionais**
  (`getWarehousesByCompany` passa a filtrar inactivos), mas preserva o histórico de stock.
- **`type`** (`WarehouseType`: STORE/DEPOT/CENTRAL/TRANSIT) — classificação do local.
- **`allowsSales`** (bool, default true) — se o local pode vender ao balcão. Novo
  `getSalesWarehousesByCompany` (activo **e** allowsSales) é usado pelo **POS** — deixa de ser
  possível vender de um depósito.
- **`manager`** (responsável) e **`phone`** (contacto).

**UI:** o diálogo "Criar Armazém" ganha Tipo, Responsável, Telefone e a opção "Permite vendas ao
balcão (POS)". `InventoryService.createWarehouse` tem um novo overload com estes campos (os antigos
delegam com defaults — retrocompatível).

## Gestão de armazéns (UI)

Nova aba **"Gestão de Armazéns"** no `StockPanel`: tabela com todos os armazéns (activos e inactivos)
— Nome, Nº, Tipo, Capacidade, Localização, Responsável, Telefone, Vendas, **Estado** (ACTIVO/INATIVO,
com cor). Acções: **Novo**, **Editar** (duplo-clique ou botão), **Activar/Desactivar**. Backend:
`getAllWarehousesByCompany` (inclui inactivos), `updateWarehouse(...)` e `setWarehouseActive(id,
active)` (MANAGER/ADMIN + auditoria `WAREHOUSE_STATUS`/`WAREHOUSE_UPDATE`). O diálogo de criar/editar
é partilhado (`warehouseDialog`).

## Não-objetivos

- Não liga o tipo a regras fiscais nem a logística/GPS.
- Não altera transferências nem o stock (continuam a operar sobre armazéns activos).
