# Harness de Decomposição - ApprovalService (Fase 122)

## 1. Objectivo

Validar estrutural e funcionalmente a decomposição de `ApprovalService.java` em componentes especializados de política de escalonamento, ciclo de vida/decisão e consultas/sincronização de documentos, garantindo preservação de limites físicos de código e compatibilidade com o ecossistema multi-módulo.

## 2. Casos de Teste

| ID | Descrição | Critério de Aceitação |
|---|---|---|
| **APSD-01** | Limites físicos estritos de linhas | `ApprovalService` $\le 115$, `ApprovalLifecycleService` $\le 210$, `ApprovalQueryService` $\le 100$, `ApprovalEscalationPolicy` $\le 50$. |
| **APSD-02** | Backend headless | Zero dependências de `JOptionPane` ou `javax.swing`. |
| **APSD-03** | Delegação canónica e alçadas | Fachada delega em `lifecycleService` e `queryService`; ciclo de vida mantém registo de histórico e disparo de callbacks; política mantém limites de alçada. |
| **APSD-04** | Preservação de contratos | Preservação de métodos públicos, constantes `BASE_AUTO_APPROVE_LIMIT` e `BASE_MANAGER_LIMIT`, anotação `@Autowired` e construtores retrocompatíveis. |

## 3. Regressão Funcional

- `ApprovalServiceTest`
- `MultiModuleArchitectureHarnessTest`
- `DesktopThinContextTest`
