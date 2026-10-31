# Especificação Técnica: Decomposição do ForensicAuditService (Fase 134)

## Diagnóstico

`ForensicAuditService` (264 linhas) misturava o controlo de acesso multi-tenant/superadmin com a recolha
de anomalias em três fontes (facturas/descontos, quebras de stock e logs de segurança) e o cálculo de
filtros, risco financeiro e índice de conformidade.

## Decisão

- `ForensicAuditService`: fachada que valida permissões (`PermissionGuard`) e posse da empresa activa, orquestrando os colaboradores e preservando o construtor legado de 3 repositórios.
- `ForensicAnomalyCollector`: inspecção de cancelamentos fiscais (`AFF-01`), descontos excessivos (`AFF-02`), quebras de inventário (`AFF-03`) e eventos sensíveis de auditoria.
- `ForensicSummaryEvaluator`: filtragem multicritério, ordenação temporal, acumulação de risco financeiro e cálculo do índice de conformidade (`AFF-05`).
