# Especificação Técnica de Decomposição - BackupService (Fase 117)

## 1. Contexto & Diagnóstico
O serviço `BackupService.java` (357 linhas) no módulo `backend` (`mz.multicore.erp.modules.backup.service`) agregava múltiplas responsabilidades:
- Injeção e manuseio direto de 15 repositórios (`userRepository`, `companyRepository`, `clientRepository`, `supplierRepository`, `productRepository`, `warehouseRepository`, `stockRepository`, `stockMovementRepository`, `invoiceRepository`, `receiptRepository`, `purchaseRepository`, `tillSessionRepository`, `accountRepository`, `transactionRepository`, `auditLogRepository`).
- Extração e mapeamento em memória de 14 coleções de dados da empresa ativa (`executeBackup`).
- Gravação física do JSON formatado com `ObjectMapper` no disco e criação de pastas.
- Parsing, validação estrutural e verificação de integridade de backups existentes (`verifyBackup`, `readCompanyId`).

## 2. Metas de Decomposição e Limites Físicos
- Orquestrador:
  - `BackupService.java`: $\le 95$ linhas (meta atual: ~75 linhas, redução $\ge 75\%$).
- Colaboradores:
  - `BackupDataCollector.java`: $\le 245$ linhas (responsabilidade única: extração e mapeamento dos 14 agregados de dados corporativos).
  - `BackupVerificationService.java`: $\le 105$ linhas (responsabilidade única: validação estrutural de ficheiros JSON, integridade e contagem de itens).
  - `BackupExecutionService.java`: $\le 85$ linhas (responsabilidade única: serialização e gravação física em ficheiro).

## 3. Preservação de Contratos & Retrocompatibilidade
- Preserva `@Service` em `BackupService`.
- Preserva método sincronizado `@Transactional(readOnly = true) public synchronized String executeBackup()`.
- Preserva `public BackupVerificationDTO verifyBackup(String path)`.
- Preserva construtor retrocompatível de 15 parâmetros para total compatibilidade com `BackupServiceTest`.
- Zero uso de `BigDecimal.ZERO.equals(...)`.
- Zero dependência Swing ou `JOptionPane` no backend.
