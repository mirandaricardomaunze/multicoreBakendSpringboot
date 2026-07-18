package com.phcpro.modules.purchases.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.purchases.dto.CreateSupplierRequest;
import com.phcpro.modules.purchases.dto.SupplierDTO;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.repository.PurchaseRepository;
import com.phcpro.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Harness FN-01..FN-05: gestão de fornecedores (editar, desactivar, pesquisar, guarda de empresa). */
class PurchaseServiceTest {

    private static final Long COMPANY_ID = 1L;

    private SupplierRepository supplierRepository;
    private com.phcpro.modules.purchases.repository.PurchaseRepository purchaseRepository;
    private FinanceService financeService;
    private PurchaseService service;

    @BeforeEach
    void setUp() {
        supplierRepository = mock(SupplierRepository.class);
        purchaseRepository = mock(com.phcpro.modules.purchases.repository.PurchaseRepository.class);
        financeService = mock(FinanceService.class);
        service = new PurchaseService(
                supplierRepository,
                purchaseRepository,
                mock(ProductRepository.class),
                mock(WarehouseRepository.class),
                mock(CompanyRepository.class),
                mock(InventoryService.class),
                financeService,
                mock(DocumentNumberService.class),
                mock(AuditLogService.class));
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseRepository.save(any(com.phcpro.modules.purchases.model.Purchase.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private com.phcpro.modules.purchases.model.Purchase purchase(String total, String paid, String status) {
        com.phcpro.modules.purchases.model.Purchase p = new com.phcpro.modules.purchases.model.Purchase();
        p.setId(50L);
        p.setPurchaseNumber("V/FT-2026/1");
        p.setSupplier(supplier(7L, "Acme Lda"));
        Company c = new Company(); c.setId(COMPANY_ID);
        p.setCompany(c);
        p.setTotalAmount(new java.math.BigDecimal(total));
        p.setAmountPaid(new java.math.BigDecimal(paid));
        p.setStatus(status);
        return p;
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Supplier supplier(Long id, String name) {
        Company c = new Company();
        c.setId(COMPANY_ID);
        Supplier s = new Supplier();
        s.setId(id);
        s.setName(name);
        s.setTaxId("123456789");
        s.setActive(true);
        s.setCompany(c);
        return s;
    }

    @Test // FN-01
    void updateSupplier_alteraCampos() {
        when(supplierRepository.findById(5L)).thenReturn(java.util.Optional.of(supplier(5L, "Antigo")));
        SupplierDTO dto = service.updateSupplier(5L, new CreateSupplierRequest(
                "Acme Lda", "123456789", "geral@acme.co.mz", "Maputo", "841234567", "Ana", COMPANY_ID));
        assertEquals("Acme Lda", dto.name());
        assertEquals("841234567", dto.phone());
        assertEquals("Ana", dto.contactPerson());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test // FN-02
    void setSupplierActive_semPermissao_lanca() {
        CurrentUserContext.setCurrentUser("func", "EMPLOYEE");
        when(supplierRepository.findById(5L)).thenReturn(java.util.Optional.of(supplier(5L, "Acme")));
        assertThrows(BusinessRuleException.class, () -> service.setSupplierActive(5L, COMPANY_ID, false));
    }

    @Test // FN-03
    void setSupplierActive_comManager_desactiva() {
        when(supplierRepository.findById(5L)).thenReturn(java.util.Optional.of(supplier(5L, "Acme")));
        SupplierDTO dto = service.setSupplierActive(5L, COMPANY_ID, false);
        assertFalse(dto.active());
    }

    @Test // FN-04
    void searchSuppliers_filtraPorNomeOuNuit() {
        when(supplierRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                supplier(1L, "Acme Lda"), supplier(2L, "Beta SA")));
        assertEquals(1, service.searchSuppliers(COMPANY_ID, "ACM").size());
        assertEquals(2, service.searchSuppliers(COMPANY_ID, "").size());
    }

    @Test // FN-05
    void updateSupplier_empresaDiferente_lanca() {
        assertThrows(BusinessRuleException.class, () -> service.updateSupplier(5L,
                new CreateSupplierRequest("X", "123456789", null, null, null, null, 999L)));
    }

    // ===== Contas a pagar (Fase 4) =====

    @Test // AP-03
    void findPayables_soComSaldoEmDivida() {
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(java.util.List.of(
                purchase("100", "100", "COMPLETED"),  // pago → excluído
                purchase("200", "50", "COMPLETED"),   // em dívida 150 → incluído
                purchase("300", "0", "CANCELLED")));   // anulado → excluído
        var payables = service.findPayablesByCompany(COMPANY_ID);
        assertEquals(1, payables.size());
        assertEquals(new java.math.BigDecimal("150"), payables.get(0).outstanding());
    }

    @Test // AP-04
    void registerSupplierPayment_parcial_abateESaiTesouraria() {
        when(purchaseRepository.findById(50L)).thenReturn(java.util.Optional.of(purchase("200", "50", "COMPLETED")));
        var dto = service.registerSupplierPayment(50L, new java.math.BigDecimal("100"), 9L, "ref-1");
        assertEquals(new java.math.BigDecimal("150"), dto.amountPaid());
        verify(financeService).registerTransaction(eq(9L), eq("CREDIT"), eq(new java.math.BigDecimal("100")), anyString());
    }

    @Test // AP-05
    void registerSupplierPayment_excedeSaldo_lanca() {
        when(purchaseRepository.findById(50L)).thenReturn(java.util.Optional.of(purchase("200", "50", "COMPLETED")));
        assertThrows(BusinessRuleException.class,
                () -> service.registerSupplierPayment(50L, new java.math.BigDecimal("999"), 9L, null));
        verifyNoInteractions(financeService);
    }

    @Test // AP-06
    void registerSupplierPayment_jaPago_lanca() {
        when(purchaseRepository.findById(50L)).thenReturn(java.util.Optional.of(purchase("200", "200", "COMPLETED")));
        assertThrows(BusinessRuleException.class,
                () -> service.registerSupplierPayment(50L, new java.math.BigDecimal("10"), 9L, null));
    }
}
