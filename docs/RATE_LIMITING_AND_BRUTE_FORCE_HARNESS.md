# Harness de Testes: Rate Limiting e Proteção contra Força Bruta (HARNESS-SEC-RL-001)

## 1. Objectivo

Este harness homologa e audita a implementação da especificação [RATE_LIMITING_AND_BRUTE_FORCE_SPEC.md](RATE_LIMITING_AND_BRUTE_FORCE_SPEC.md) (`SPEC-SEC-RL-001`), validando os mecanismos activos de defesa contra força bruta no login e adivinhação de PINs de supervisor/gestor.

---

## 2. Cenários de Teste

| ID | Cenário | Ação de Teste | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **RL-01** | Bloqueio de Login por Utilizador | 5 tentativas falhadas consecutivas para o mesmo username. | A 6ª tentativa é sumariamente rejeitada com `BusinessRuleException` indicando o tempo de bloqueio; tentativas legítimas são impedidas durante a janela. |
| **RL-02** | Reset de Contador de Login no Sucesso | 4 tentativas falhadas seguidas de 1 tentativa com credenciais válidas. | O contador é resetado; a próxima tentativa falhada começa novamente em 1 falha sem bloqueio. |
| **RL-03** | Proteção de Login por IP (Credential Stuffing) | 30 tentativas falhadas com diferentes nomes de utilizador a partir do mesmo IP. | O endereço IP é bloqueado por 15 minutos, impedindo tentativas adicionais mesmo para outros utilizadores. |
| **RL-04** | Bloqueio Ativo de PIN de Gerente (3 Falhas) | 3 tentativas de PIN inválido para o mesmo contexto/empresa. | O sistema bloqueia a verificação de PIN por 5 minutos, retornando resposta estruturada com `valid = false` e mensagem clara de bloqueio. |
| **RL-05** | Reset de PIN no Sucesso | 2 falhas de PIN seguidas de 1 PIN correto de gestor ativo. | PIN verificado com sucesso; contador de falhas é limpo imediatamente. |
| **RL-06** | Isolamento Multi-Tenant e Multi-IP de PIN | Falhas de PIN na Empresa 1 ou IP A não afetam nem bloqueiam a Empresa 2 ou IP B. | Cada tenant/origem mantém o seu próprio estado de tentativa independente. |

---

## 3. Execução Automatizada

Classe de teste canónica:
- [RateLimitingAndBruteForceHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/architecture/security/RateLimitingAndBruteForceHarnessTest.java)

Comando de teste:
```powershell
mvn test -pl backend -Dtest=RateLimitingAndBruteForceHarnessTest
```

Critério de aprovação:
- 100% de testes aprovados com 0 falhas e 0 erros.
