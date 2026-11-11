package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coletor de dados corporativos para cópias de segurança (backup).
 */
@Service
public class BackupDataCollector {

    private final AppUserRepository userRepository;
    private final CompanyLookup companyLookup;
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

    @Autowired
    public BackupDataCollector(
            AppUserRepository userRepository,
            CompanyLookup companyLookup,
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
        this.companyLookup = companyLookup;
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

    public Map<String, Object> collectBackupData(Long companyId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("companyId", companyId);
        data.put("generatedAt", LocalDateTime.now().toString());

        data.put("users", userRepository == null ? List.of() : userRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(companyId).stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("name", u.getName());
            m.put("role", u.getRoleForCompany(companyId));
            m.put("active", u.isActive());
            return m;
        }).collect(Collectors.toList()));

        data.put("companies", companyLookup == null ? List.of() : companyLookup.find(companyId).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("taxId", c.getTaxId());
            m.put("email", c.getEmail());
            return m;
        }).collect(Collectors.toList()));

        data.put("clients", clientRepository == null ? List.of() : clientRepository.findDistinctByCompaniesIdOrderByName(companyId).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("taxId", c.getTaxId());
            m.put("email", c.getEmail());
            return m;
        }).collect(Collectors.toList()));

        data.put("suppliers", supplierRepository == null ? List.of() : supplierRepository.findByCompanyId(companyId).stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("taxId", s.getTaxId());
            m.put("email", s.getEmail());
            m.put("companyId", s.getCompany().getId());
            return m;
        }).collect(Collectors.toList()));

        data.put("products", productRepository == null ? List.of() : productRepository.findDistinctByCompaniesIdOrderByName(companyId).stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("sku", p.getSku());
            m.put("reference", p.getReference());
            m.put("barcode", p.getBarcode());
            m.put("name", p.getName());
            m.put("unitPrice", p.getUnitPrice());
            return m;
        }).collect(Collectors.toList()));

        data.put("warehouses", warehouseRepository == null ? List.of() : warehouseRepository.findByCompanyId(companyId).stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getId());
            m.put("name", w.getName());
            m.put("location", w.getLocation());
            m.put("companyId", w.getCompany().getId());
            return m;
        }).collect(Collectors.toList()));

        data.put("stocks", stockRepository == null ? List.of() : stockRepository.findByWarehouseCompanyId(companyId).stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("productId", s.getProduct().getId());
            m.put("productName", s.getProduct().getName());
            m.put("warehouseId", s.getWarehouse().getId());
            m.put("warehouseName", s.getWarehouse().getName());
            m.put("quantity", s.getQuantity());
            return m;
        }).collect(Collectors.toList()));

        data.put("stockMovements", stockMovementRepository == null ? List.of() : stockMovementRepository.findByCompanyId(companyId).stream().map(m -> {
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

        data.put("invoices", invoiceRepository == null ? List.of() : invoiceRepository.findByCompanyId(companyId).stream().map(i -> {
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

        data.put("receipts", receiptRepository == null ? List.of() : receiptRepository.findByCompanyId(companyId).stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("receiptNumber", r.getReceiptNumber());
            m.put("invoiceNumber", r.getInvoice().getInvoiceNumber());
            m.put("amountPaid", r.getAmountPaid());
            m.put("paymentMethod", r.getPaymentMethod());
            m.put("status", r.getStatus());
            return m;
        }).collect(Collectors.toList()));

        data.put("purchases", purchaseRepository == null ? List.of() : purchaseRepository.findByCompanyId(companyId).stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("purchaseNumber", p.getPurchaseNumber());
            m.put("supplierName", p.getSupplier().getName());
            m.put("warehouseName", p.getWarehouse().getName());
            m.put("total", p.getTotalAmount());
            m.put("status", p.getStatus());
            return m;
        }).collect(Collectors.toList()));

        data.put("tillSessions", tillSessionRepository == null ? List.of() : tillSessionRepository.findByCompanyId(companyId).stream().map(ts -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ts.getId());
            m.put("operator", ts.getOperator());
            m.put("opening", ts.getOpeningBalance());
            m.put("closingExpected", ts.getClosingBalanceExpected());
            m.put("closingReal", ts.getClosingBalanceReal());
            m.put("status", ts.getStatus());
            return m;
        }).collect(Collectors.toList()));

        data.put("treasuryAccounts", accountRepository == null ? List.of() : accountRepository.findByCompanyIdOrderByName(companyId).stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getName());
            m.put("balance", a.getBalance());
            return m;
        }).collect(Collectors.toList()));

        data.put("auditLogs", auditLogRepository == null ? List.of() : auditLogRepository.findByCompanyIdOrderByEventTimeDesc(companyId).stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("username", l.getUsername());
            m.put("action", l.getAction());
            m.put("details", l.getDetails());
            m.put("time", l.getEventTime().toString());
            return m;
        }).collect(Collectors.toList()));

        return data;
    }
}
