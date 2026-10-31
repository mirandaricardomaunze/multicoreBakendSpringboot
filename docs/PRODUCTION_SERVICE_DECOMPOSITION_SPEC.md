# Especificação Técnica de Decomposição - ProductionService (Fase 115)

## 1. Contexto & Diagnóstico
O serviço `ProductionService.java` (370 linhas) no módulo `backend` (`mz.multicore.erp.modules.manufacturing.service`) agregava:
- Injeção direta de 7 dependências (`BillOfMaterialsRepository`, `ProductionOrderRepository`, `CompanyRepository`, `ProductRepository`, `WarehouseRepository`, `StockRepository`, `ApplicationEventPublisher`).
- Ciclo de vida completo de Fichas Técnicas (BOM) e apuração de custos de matérias-primas e mão-de-obra estimada.
- Ciclo de vida operacional de Ordens de Produção (criação proporcional com `baseRatio` e perdas de sucata, início, conclusão com baixa de insumos no armazém de origem e entrada de produto acabado no armazém de destino, apuração do Custo Industrial de Produção CIP e cancelamento).
- Emissão de eventos de integração contábil (`ProductionCompletedEvent`).

## 2. Metas de Decomposição e Limites Físicos
- Orquestrador:
  - `ProductionService.java`: $\le 110$ linhas (meta atual: ~85 linhas, redução $\ge 75\%$).
- Colaboradores:
  - `BillOfMaterialsService.java`: $\le 165$ linhas (responsabilidade única: ciclo de vida de fichas técnicas e estimativa de custos).
  - `ProductionOrderLifecycleService.java`: $\le 245$ linhas (responsabilidade única: ordens de produção, execução física em stocks, cálculo de CIP e publicação de eventos).
  - `ProductionOrderMapper.java`: $\le 60$ linhas (responsabilidade única: mapeamento determinístico para ProductionOrderDTO).

## 3. Preservação de Contratos & Retrocompatibilidade
- Preserva `@Service` em `ProductionService`.
- Preserva construtor canónico `@Autowired` de 2 colaboradores especializados.
- Preserva construtores retrocompatíveis de 6 e 7 parâmetros para testes legados.
- Preserva todos os métodos públicos usados por `ProductionController`.
- Zero uso de `BigDecimal.ZERO.equals(...)`.
- Zero dependência Swing ou `JOptionPane` no backend.
