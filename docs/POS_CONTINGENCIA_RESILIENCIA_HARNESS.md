# Harness de Conformidade — Modo de Contingência & Resiliência Local no POS

**Código:** HARNESS-PCR-001  
**Especificação:** SPEC-PCR-001  
**Data:** 2026-09-17  

---

## 1. Matriz de Critérios de Conformidade Automatizados

| ID | Descrição do Teste | Nível | Classe de Teste | Estado |
|---|---|---|---|:---:|
| **PCR-01** | Checkout com referência de contingência grava o campo `contingency_reference` na fatura emitida | Backend | `PosContingencyHarnessTest` | ✅ Verde |
| **PCR-02** | Idempotência fiscal: chamada repetida com o mesmo `contingencyReference` devolve a fatura existente sem duplicar número nem stock | Backend | `PosContingencyHarnessTest` | ✅ Verde |
| **PCR-03** | Checkout regular sem contingência funciona normalmente e gera nova numeração `FT` sequencial | Backend | `PosContingencyHarnessTest` | ✅ Verde |
| **PCR-04** | Enfileiramento local, persistência atómica em JSON e recuperação de ficheiro em disco | Desktop | `PosContingencyManagerTest` | ✅ Verde |
| **PCR-05** | Deteção precisa de falhas de conectividade (`ConnectException`, timeout, 502/503/504) vs erros de validação semântica | Desktop | `PosContingencyManagerTest` | ✅ Verde |
| **PCR-06** | Sincronização FIFO com transição de estado para `SYNCED` e registo do número `FT` retornado | Desktop | `PosContingencyManagerTest` | ✅ Verde |
| **PCR-07** | Degradação suave: paragem graciosa da sincronização quando a rede continua indisponível, sem descarte de vendas | Desktop | `PosContingencyManagerTest` | ✅ Verde |
| **PCR-08** | Limite estrito de linhas: `POSPanel.java` decomposto e mantido abaixo de 1000 linhas | Arquitetura | `UiPanelDecompositionTest` | ✅ Verde |
| **PCR-09** | Ausência de literais de cor hardcoded fora do sistema canónico `UIHelper` | UI | `FinalUiUniformityHarnessTest` | ✅ Verde |
| **PCR-10** | Isolamento do reactor: módulo desktop não compila OpenPDF e reactor Maven compila 100% | Arquitetura | `MultiModuleArchitectureHarnessTest` | ✅ Verde |

---

## 2. Instruções de Execução

```powershell
# Executar harness do backend
mvn test -Dtest=PosContingencyHarnessTest -pl backend

# Executar harness do desktop
mvn test -Dtest=PosContingencyManagerTest -pl desktop

# Executar harnesses de conformidade arquitetural
mvn test "-Dtest=UiPanelDecompositionTest,FinalUiUniformityHarnessTest" -pl desktop
mvn test -Dtest=MultiModuleArchitectureHarnessTest -pl backend
```
