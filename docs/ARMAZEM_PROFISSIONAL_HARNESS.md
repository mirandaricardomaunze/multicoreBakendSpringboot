# Harness — Campos profissionais do armazém

> Cenários para [ARMAZEM_PROFISSIONAL_SPEC.md](ARMAZEM_PROFISSIONAL_SPEC.md).
> AR-01 automático (`InventoryServiceTest`); AR-50..AR-53 manuais.

**Última actualização:** 2026-07-04

## Automático — `InventoryServiceTest`

| ID    | Cenário | Esperado |
|-------|---------|----------|
| AR-01 | 3 armazéns: Loja (activo, vende), Depósito (activo, não vende), Antigo (inactivo). | `getSalesWarehousesByCompany` → só **Loja**; `getWarehousesByCompany` → Loja + Depósito (exclui inactivo). |

## Manuais (UI)

| ID    | Passos | Esperado |
|-------|--------|----------|
| AR-50 | Stock → "Criar Armazém": Tipo=Depósito, **desmarcar** "Permite vendas", Responsável+Telefone. | Grava sem erro. |
| AR-51 | Abrir o **POS**. | O depósito criado em AR-50 **não** aparece no combo "Armazém"; só as Lojas. |
| AR-52 | Criar armazém tipo Loja com "Permite vendas" marcado. | Aparece no POS. |
| AR-53 | Stock → **Gestão de Armazéns** → seleccionar um armazém → **Desactivar**. | Estado passa a INATIVO (linha vermelha); deixa de aparecer em stock/POS/transferências; histórico intacto. |
| AR-54 | Gestão de Armazéns → **Editar** (ou duplo-clique) → mudar Tipo/Responsável/Vendas → gravar. | Alterações reflectidas na tabela; auditoria `WAREHOUSE_UPDATE`. |
| AR-55 | Desactivar sem perfil MANAGER/ADMIN. | Bloqueado com aviso de permissão. |

## Verificação

- `mvn clean test` → verde (AR-01 em `InventoryServiceTest`). Flyway aplica `V21` no arranque.
