# Especificação Técnica: Decomposição Modular do DeliveryGuideService (Fase 111)

## 1. Contexto e Diagnóstico
O `DeliveryGuideService.java` acumulava 402 linhas de código no backend (`mz.multicore.erp.modules.comercial.service`), agrupando a lógica mercantil, operacional e logística de expedição de mercadorias a clientes:
- **Criação de Guias Avulsas e a partir de Encomendas:** Validação de transportador, matrícula da viatura, separação estrita de caminhos (a encomenda gera guia OU fatura), trava da encomenda para prevenir dupla faturação (`GUIDE_PENDING`), bloqueio de encomendas de reposição interna (que usam guia de transferência).
- **Máquina de Estados e Movimento de Inventário:** A guia nasce sempre `PENDING_APPROVAL` sem mover inventário; no ato da aprovação exclusiva de gestores/administradores é executada a saída física do armazém (`SALE`, FEFO) e a encomenda passa a `GUIDED`; a rejeição ou cancelamento liberta a encomenda de volta para `PENDING`.
- **Cálculo de Carga Logística e Consultas:** Apuração volumétrica e ponderal da expedição (`LogisticsLoadCalculator`), segregação por unidade de negócio (`BusinessUnit` / `Misto`) e projeção DTO.

## 2. Decisão Arquitetural
A decomposição segrega o serviço em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`DeliveryGuideCreationService.java`** (186 linhas $\le 190$):
   - Emissão de guias avulsas com validação de cliente ativo, armazém, dados do transportador e integridade de linhas sem repetição de produto.
   - Emissão a partir de encomendas aprovadas com cópia integral de lotes e números de série e trava de faturamento da encomenda (`GUIDE_PENDING`).
   - Registo inicial no estado `PENDING_APPROVAL` e auditoria de criação.

2. **`DeliveryGuideLifecycleService.java`** (138 linhas $\le 160$):
   - Aprovação de guias com movimentação transacional de stock na saída (`SALE`) no armazém via `InventoryService` e transição da encomenda para `GUIDED`.
   - Rejeição e cancelamento com registo de motivos e libertação reversível da encomenda de volta para `PENDING`.
   - Auditoria rigorosa de todas as decisões com utilizador e carimbo temporal.

3. **`DeliveryGuideQueryService.java`** (124 linhas $\le 145$):
   - Consultas de guias por empresa com validação de tenant (`findByCompany`, `findById`).
   - Carregamento de entidade com linhas para o serviço de impressão (`loadForPrint`).
   - Mapeamento determinístico `toDTO` integrando o motor de cálculo logístico de rateio de peso (`LogisticsLoadCalculator`) e apuração de unidades de negócio.

4. **`DeliveryGuideService.java`** (97 linhas $\le 120$):
   - Fachada unificada delegando nos três colaboradores especializados.
   - Construtor canónico `@Autowired` de 3 colaboradores.
   - Construtor retrocompatível de 7 dependências garantindo 100% de compatibilidade com os testes unitários legados (`DeliveryGuideServiceTest`) e controladores REST (`DeliveryGuideController`).

## 3. Conformidade
- Redução de 402 para 97 linhas no orquestrador (redução de 305 linhas / 75.9%).
- 100% retrocompatível com `DeliveryGuideServiceTest` e `DeliveryGuideController`.
- Zero uso de `BigDecimal.ZERO.equals`.
- Mensagens ao utilizador em português de Moçambique.
