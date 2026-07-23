# HARNESS — Guia de Remessa a partir da Encomenda

Complementa [GUIA_REMESSA_ENCOMENDA_SPEC.md](GUIA_REMESSA_ENCOMENDA_SPEC.md). Cenários `GR-01..`
automáticos (`DeliveryGuideServiceTest`, Mockito puro — molde `StockTransferServiceTest`);
`GR-50..` manuais (ao vivo / UI).

## Automáticos — `DeliveryGuideServiceTest`

| ID     | Cenário                                                                                 | Esperado |
|--------|------------------------------------------------------------------------------------------|----------|
| GR-01  | Criar guia de encomenda `PENDING`                                                        | Guia `PENDING_APPROVAL`, nº série `GR`, encomenda → `GUIDE_PENDING`, **sem** movimento de stock |
| GR-02  | Criar guia de encomenda que **não** está `PENDING` (ex.: `BILLED`, `GUIDED`, `PENDING_APPROVAL`) | `BusinessRuleException` (só encomendas aprovadas e ainda não expedidas/faturadas) |
| GR-03  | Aprovar guia pendente                                                                    | Guia `APPROVED`, **um `registerMovement("SALE")` por linha** (qtd negativa), encomenda → `GUIDED` |
| GR-04  | Aprovar sem perfil `MANAGER`/`ADMIN`                                                     | `BusinessRuleException` (permissão); **sem** movimento de stock |
| GR-05  | Aprovar guia já `APPROVED`/`REJECTED`/`CANCELLED`                                        | `BusinessRuleException`; **sem** stock |
| GR-06  | Rejeitar guia pendente com motivo                                                       | Guia `REJECTED`, motivo gravado, **sem** stock, encomenda volta a `PENDING` |
| GR-07  | Rejeitar sem motivo                                                                      | `BusinessRuleException` (motivo obrigatório) |
| GR-08  | Cancelar guia pendente                                                                   | Guia `CANCELLED`, **sem** stock, encomenda volta a `PENDING` |
| GR-09  | Cancelar guia já `APPROVED`                                                              | `BusinessRuleException` (stock já saiu) |
| GR-10  | Número da guia por empresa                                                               | `documentNumberService.next(DELIVERY_GUIDE)` chamado; nº guardado na guia |

## Manuais (ao vivo / integração)

| ID     | Cenário                                                                                 | Evidência |
|--------|------------------------------------------------------------------------------------------|-----------|
| GR-50  | `POST /api/comercial/delivery-guides` sobre encomenda aprovada real                     | 200 + DTO `PENDING_APPROVAL` |
| GR-51  | `POST /{id}/approve` (MANAGER) baixa stock real (PostgreSQL) e encomenda fica `GUIDED`  | stock do armazém decresce pela qtd das linhas |
| GR-52  | Tentar `billOrder` de encomenda `GUIDED`                                                | Rejeitada ("apenas encomendas PENDENTE podem ser faturadas") — **caminhos separados** |
| GR-53  | `GET /api/print/delivery-guide/{id}` gera PDF                                            | PDF "Guia de Remessa", nº `GR-...`, destinatário/linhas/transporte/assinaturas |
| GR-54  | Duas empresas emitem `GR-2026/N` com o mesmo N                                           | Coexistem (UNIQUE por empresa, não global) |
| GR-55  | Rejeitar/cancelar guia devolve a encomenda a faturável                                  | Encomenda volta a aparecer como `PENDING`/faturável |

## Desktop cliente-fino + UI Swing (`GR-60+`)

| ID     | Cenário                                                                                 | Esperado / evidência |
|--------|------------------------------------------------------------------------------------------|----------------------|
| GR-60  | `ComercialApiClient` expõe list/get/create/approve/reject/cancel/print por HTTP          | Desktop não chama Service/Repository; `mvn clean compile` valida os contratos DTO |
| GR-61  | Selecionar encomenda `PENDING` e clicar **Converter em Guia**                            | Modal mostra encomenda/cliente/total e recolhe Responsável, Viatura/Matrícula e Observações |
| GR-62  | Confirmar o diálogo de transporte                                                       | Cria GR `PENDING_APPROVAL`; informa que o stock só sai na aprovação; recarrega Encomendas + GR |
| GR-63  | Tentar converter encomenda fora de `PENDING`                                             | UI bloqueia com estado atual; backend mantém a guarda autoritativa |
| GR-64  | Abrir aba **Guias de Remessa (GR)**                                                      | Tabela com nº/data/encomenda/cliente/armazém/transporte/total/estado; pesquisa e filtro de estado funcionam |
| GR-65  | Aprovar GR pendente                                                                      | Confirma explicitamente a saída de stock; chama endpoint; recarrega GR + Encomendas |
| GR-66  | Rejeitar GR pendente                                                                     | Motivo obrigatório; encomenda volta a ficar disponível; tabelas recarregadas |
| GR-67  | Cancelar GR pendente                                                                     | Confirma cancelamento; encomenda volta a ficar disponível; GR fica `CANCELLED` |
| GR-68  | Imprimir GR selecionada                                                                  | Obtém PDF por `/api/print/delivery-guide/{id}` e abre `guia-remessa-<nº>.pdf` |
| GR-69  | Clicar **Atualizar** na aba GR                                                           | Reconsulta a empresa ativa e preserva o filtro instalado na tabela |

## Definition of done (v1 completa)

- `mvn clean compile` passa.
- Suite completa `mvn test` passa (inclui `DeliveryGuideServiceTest`).
- Nenhum endpoint sem tenant/permissão/auditoria onde a spec o exige.
- Cliente-fino usa apenas HTTP/DTO; a UI não contém regra de stock.
- GR-60 está coberto pelo build; GR-61..69 ficam disponíveis para validação manual no desktop ligado ao backend.
- `MOVIMENTOS_COMERCIAIS.md` e `tasks/current.md` actualizados.
</content>
