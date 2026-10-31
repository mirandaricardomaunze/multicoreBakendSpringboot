# Especificação Técnica: Decomposição Modular do OccupationalHealthService (Fase 109)

## 1. Contexto e Diagnóstico
O `OccupationalHealthService.java` acumulava 399 linhas de código no backend (`mz.multicore.erp.modules.hr.service`), agregando três grandes responsabilidades clínicas e administrativas com alto impacto de conformidade legal e privacidade (Leis n.º 19/2014 e 13/2023):
- **Registo e Validação Clínica de Exames:** Proteção legal contra registo de dados serológicos proibidos (HIV/SIDA), regras de aptidão com restrições, validação de prazos, cifra simétrica de comprovativos médicos e auditoria de registo.
- **Gestão Financeira e Custos Ocupacionais:** Liquidação de exames médicos na tesouraria (`FinanceService`), cadastro e seleção de clínicas/prestadores credenciados e relatório analítico de custos por prestador.
- **Consultas, Sigilo e Conformidade:** Radar de exames a caducar em 60 dias, radar de trabalhadores no activo sem exame inicial de aptidão (`missingExams`), histórico clínico e anexos com acesso auditado e restrito ao próprio colaborador ou gestores de RH.

## 2. Decisão Arquitetural
A decomposição segrega o serviço em 3 colaboradores especializados com responsabilidade única (SRP) apoiados por um mapper determinístico:

1. **`OccupationalHealthMapper.java`** (43 linhas $\le 60$):
   - Mapeamentos puros e determinísticos para `OccupationalHealthExamDTO` e `OccupationalHealthSummaryDTO`.
   - Cálculo de dias para expiração e determinação de estado (`EXPIRED`, `EXPIRING`, `VALID`).
   - Formatação segura de identificação de prestadores e sanitização de strings em branco.

2. **`OccupationalHealthRegistrationService.java`** (150 linhas $\le 160$):
   - Validação formal do resultado de aptidão e coerência temporal entre data de realização e data de validade.
   - Aplicação estrita da barreira legal contra recolha de dados de saúde proibidos (HIV/SIDA/sorologia).
   - Validação de custos e números de fatura, resolução de prestadores credenciados e cifra de comprovativos.
   - Registo persistente de exames e emissão de eventos auditados no `AuditLogService`.

3. **`OccupationalHealthFinanceService.java`** (115 linhas $\le 145$):
   - Pagamento transacional de exames médicos à clínica parceira integrado com a tesouraria via `FinanceService.registerAutoPayout`.
   - Consulta de prestadores de saúde ativos no cadastro de fornecedores.
   - Relatório analítico de custos de medicina do trabalho com discriminação detalhada por prestador.

4. **`OccupationalHealthQueryService.java`** (129 linhas $\le 160$):
   - Resumo executivo de aptidão com barreira de sigilo (acesso restrito ao gestor ou ao próprio colaborador).
   - Histórico clínico ocupacional auditado a cada consulta.
   - Decifra e descarregamento de comprovativos médicos anexos com rasto de auditoria.
   - Radares analíticos de exames a caducar em 60 dias e trabalhadores ativos sem exame admissional.

5. **`OccupationalHealthService.java`** (101 linhas $\le 120$):
   - Fachada unificada que preserva todos os contratos REST e da camada de serviços de RH.
   - Construtor canónico `@Autowired` de 3 colaboradores.
   - Construtor retrocompatível de 7 dependências garantindo 100% de compatibilidade com os testes unitários legados (`OccupationalHealthServiceTest`).

## 3. Conformidade
- Redução de 399 para 101 linhas no orquestrador (redução de 298 linhas / 74.7%).
- 100% retrocompatível com `OccupationalHealthServiceTest` e `OccupationalHealthController`.
- Zero uso de `BigDecimal.ZERO.equals`.
- Mensagens ao utilizador em português de Moçambique.
