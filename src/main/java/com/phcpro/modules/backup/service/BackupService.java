package com.phcpro.modules.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.backup.dto.BackupVerificationDTO;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.comercial.repository.ReceiptRepository;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.repository.TreasuryTransactionRepository;
import com.phcpro.modules.inventory.repository.StockMovementRepository;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.pos.repository.TillSessionRepository;
import com.phcpro.modules.purchases.repository.PurchaseRepository;
import com.phcpro.modules.purchases.repository.SupplierRepository;
import com.phcpro.modules.users.repository.AppUserRepository;
import com.phcpro.modules.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BackupService {

    private static final List<String> REQUIRED_COLLECTIONS = List.of(
            "users",
            "companies",
            "clients",
            "suppliers",
            "products",
            "warehouses",
            "stocks",
            "stockMovements",
            "invoices",
            "receipts",
            "purchases",
            "tillSessions",
            "treasuryAccounts",
            "auditLogs"
    );

    private final AppUserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final PurchaseRepository purchaseRepository;
    private final TillSessionRepository tillSessionRepository;
    private final TreasuryAccountRepository accountRepository;
    private final TreasuryTransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public BackupService(
            AppUserRepository userRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            PurchaseRepository purchaseRepository,
            TillSessionRepository tillSessionRepository,
            TreasuryAccountRepository accountRepository,
            TreasuryTransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.purchaseRepository = purchaseRepository;
        this.tillSessionRepository = tillSessionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public synchronized String executeBackup() {
        PermissionGuard.requireAdmin("gerar cópia de segurança");
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            Long companyId = CurrentUserContext.getCurrentCompanyId();
            data.put("companyId", companyId);
            data.put("generatedAt", LocalDateTime.now().toString());

            // 1. Users
            data.put("users", userRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(companyId).stream().map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("username", u.getUsername());
                m.put("name", u.getName());
                m.put("role", u.getRoleForCompany(companyId));
                m.put("active", u.isActive());
                return m;
            }).collect(Collectors.toList()));

            // 2. Companies
            data.put("companies", companyRepository.findById(companyId).stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                m.put("taxId", c.getTaxId());
                m.put("email", c.getEmail());
                return m;
            }).collect(Collectors.toList()));

            // 3. Clients
            data.put("clients", clientRepository.findDistinctByCompaniesIdOrderByName(companyId).stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                m.put("taxId", c.getTaxId());
                m.put("email", c.getEmail());
                return m;
            }).collect(Collectors.toList()));

            // 4. Suppliers
            data.put("suppliers", supplierRepository.findByCompanyId(companyId).stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("taxId", s.getTaxId());
                m.put("email", s.getEmail());
                m.put("companyId", s.getCompany().getId());
                return m;
            }).collect(Collectors.toList()));

            // 5. Products
            data.put("products", productRepository.findDistinctByCompaniesIdOrderByName(companyId).stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("sku", p.getSku());
                m.put("reference", p.getReference());
                m.put("barcode", p.getBarcode());
                m.put("name", p.getName());
                m.put("unitPrice", p.getUnitPrice());
                return m;
            }).collect(Collectors.toList()));

            // 6. Warehouses
            data.put("warehouses", warehouseRepository.findByCompanyId(companyId).stream().map(w -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", w.getId());
                m.put("name", w.getName());
                m.put("location", w.getLocation());
                m.put("companyId", w.getCompany().getId());
                return m;
            }).collect(Collectors.toList()));

            // 7. Stocks
            data.put("stocks", stockRepository.findByWarehouseCompanyId(companyId).stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("productId", s.getProduct().getId());
                m.put("productName", s.getProduct().getName());
                m.put("warehouseId", s.getWarehouse().getId());
                m.put("warehouseName", s.getWarehouse().getName());
                m.put("quantity", s.getQuantity());
                return m;
            }).collect(Collectors.toList()));

            // 8. Stock Movements
            data.put("stockMovements", stockMovementRepository.findByCompanyId(companyId).stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("movementType", m.getMovementType());
                map.put("productId", m.getProduct().getId());
                map.put("productName", m.getProduct().getName());
                map.put("warehouseId", m.getWarehouse().getId());
                map.put("quantity", m.getQuantity());
                map.put("batchNumber", m.getBatchNumber());
                map.put("serialNumber", m.getSerialNumber());
                map.put("description", m.getDescription());
                map.put("date", m.getMovementDate().toString());
                return map;
            }).collect(Collectors.toList()));

            // 9. Invoices
            data.put("invoices", invoiceRepository.findByCompanyId(companyId).stream().map(i -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", i.getId());
                m.put("invoiceNumber", i.getInvoiceNumber());
                m.put("clientId", i.getClient().getId());
                m.put("clientName", i.getClient().getName());
                m.put("total", i.getTotalAmount());
                m.put("status", i.getStatus().name());
                m.put("cancellationReason", i.getCancellationReason());
                return m;
            }).collect(Collectors.toList()));

            // 10. Receipts
            data.put("receipts", receiptRepository.findByCompanyId(companyId).stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("receiptNumber", r.getReceiptNumber());
                m.put("invoiceNumber", r.getInvoice().getInvoiceNumber());
                m.put("amountPaid", r.getAmountPaid());
                m.put("paymentMethod", r.getPaymentMethod());
                m.put("status", r.getStatus());
                return m;
            }).collect(Collectors.toList()));

            // 11. Purchases
            data.put("purchases", purchaseRepository.findByCompanyId(companyId).stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("purchaseNumber", p.getPurchaseNumber());
                m.put("supplierName", p.getSupplier().getName());
                m.put("warehouseName", p.getWarehouse().getName());
                m.put("total", p.getTotalAmount());
                m.put("status", p.getStatus());
                return m;
            }).collect(Collectors.toList()));

            // 12. Till Sessions
            data.put("tillSessions", tillSessionRepository.findByCompanyId(companyId).stream().map(ts -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", ts.getId());
                m.put("operator", ts.getOperator());
                m.put("opening", ts.getOpeningBalance());
                m.put("closingExpected", ts.getClosingBalanceExpected());
                m.put("closingReal", ts.getClosingBalanceReal());
                m.put("status", ts.getStatus());
                return m;
            }).collect(Collectors.toList()));

            // 13. Treasury Accounts
            data.put("treasuryAccounts", accountRepository.findByCompanyIdOrderByName(companyId).stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("name", a.getName());
                m.put("balance", a.getBalance());
                return m;
            }).collect(Collectors.toList()));

            // 14. Audit Logs
            data.put("auditLogs", auditLogRepository.findByCompanyIdOrderByEventTimeDesc(companyId).stream().map(l -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", l.getId());
                m.put("username", l.getUsername());
                m.put("action", l.getAction());
                m.put("details", l.getDetails());
                m.put("time", l.getEventTime().toString());
                return m;
            }).collect(Collectors.toList()));

            // Save to backups folder
            File backupDir = new File("backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backupFile = new File(backupDir, "company_" + companyId + "_backup_" + timestamp + ".json");

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(backupFile, data);

            return backupFile.getAbsolutePath();
        } catch (IOException e) {
            throw new BusinessRuleException("Falha ao criar backup de segurança: " + e.getMessage());
        }
    }

    public BackupVerificationDTO verifyBackup(String path) {
        PermissionGuard.requireAdmin("verificar cópia de segurança");
        if (path == null || path.isBlank()) {
            throw new BusinessRuleException("Selecione um ficheiro de backup para verificar.");
        }

        File backupFile = new File(path);
        if (!backupFile.exists() || !backupFile.isFile()) {
            throw new BusinessRuleException("Ficheiro de backup não encontrado.");
        }
        if (!backupFile.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new BusinessRuleException("O ficheiro selecionado não é um backup JSON válido.");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(backupFile, new TypeReference<>() {});
            Long currentCompanyId = CurrentUserContext.getCurrentCompanyId();
            Long backupCompanyId = readCompanyId(data.get("companyId"));
            if (!currentCompanyId.equals(backupCompanyId)) {
                throw new BusinessRuleException("O backup pertence a outra empresa.");
            }
            Object generatedAt = data.get("generatedAt");
            if (!(generatedAt instanceof String generatedAtText) || generatedAtText.isBlank()) {
                throw new BusinessRuleException("Backup inválido: data de geração ausente.");
            }

            Map<String, Integer> itemCounts = new LinkedHashMap<>();
            for (String section : REQUIRED_COLLECTIONS) {
                Object value = data.get(section);
                if (!(value instanceof List<?> items)) {
                    throw new BusinessRuleException("Backup inválido: secção '" + section + "' ausente ou corrompida.");
                }
                itemCounts.put(section, items.size());
            }

            return new BackupVerificationDTO(
                    backupFile.getName(),
                    backupCompanyId,
                    generatedAtText,
                    REQUIRED_COLLECTIONS.size(),
                    itemCounts
            );
        } catch (BusinessRuleException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessRuleException("Falha ao verificar backup: " + e.getMessage());
        }
    }

    private Long readCompanyId(Object rawCompanyId) {
        if (rawCompanyId instanceof Number number) {
            return number.longValue();
        }
        if (rawCompanyId instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                throw new BusinessRuleException("Backup inválido: empresa com identificador inválido.");
            }
        }
        throw new BusinessRuleException("Backup inválido: empresa ausente.");
    }
}
