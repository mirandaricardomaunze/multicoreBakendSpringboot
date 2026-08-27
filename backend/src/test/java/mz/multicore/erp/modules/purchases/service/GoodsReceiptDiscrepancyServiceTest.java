package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.purchases.dto.SupplierDiscrepancyDTO;
import mz.multicore.erp.modules.purchases.model.DiscrepancyType;
import mz.multicore.erp.modules.purchases.model.GoodsReceiptDiscrepancy;
import mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Relatório de divergências da conferência à chegada (CC-10..CC-16).
 * Ver docs/CONFERENCIA_CHEGADA_SPEC.md.
 */
class GoodsReceiptDiscrepancyServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate DE = LocalDate.of(2026, 8, 1);
    private static final LocalDate ATE = LocalDate.of(2026, 8, 31);

    private GoodsReceiptDiscrepancyRepository repository;
    private GoodsReceiptDiscrepancyService service;

    @BeforeEach
    void setUp() {
        repository = mock(GoodsReceiptDiscrepancyRepository.class);
        service = new GoodsReceiptDiscrepancyService(repository, mock(AuditLogService.class));
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private GoodsReceiptDiscrepancy item(Long supplierId, String supplier, DiscrepancyType type,
                                         String qty, String price, boolean resolved) {
        GoodsReceiptDiscrepancy d = new GoodsReceiptDiscrepancy();
        Company company = new Company();
        company.setId(COMPANY_ID);
        d.setCompany(company);
        d.setSupplierId(supplierId);
        d.setSupplierName(supplier);
        d.setType(type);
        d.setQuantity(new BigDecimal(qty));
        d.setUnitPrice(new BigDecimal(price));
        d.setOccurredOn(LocalDate.of(2026, 8, 10));
        d.setResolved(resolved);
        return d;
    }

    private void stub(List<GoodsReceiptDiscrepancy> items) {
        when(repository.findByCompanyIdAndOccurredOnBetweenOrderByOccurredOnDesc(eq(COMPANY_ID), any(), any()))
                .thenReturn(items);
    }

    @Test // CC-10
    void resumo_somaPorFornecedorESeparaDanificadoDeEmFalta() {
        stub(List.of(
                item(7L, "Acme", DiscrepancyType.DAMAGED, "2", "25", false),
                item(7L, "Acme", DiscrepancyType.MISSING, "1", "25", false)));

        SupplierDiscrepancyDTO acme = service.summaryBySupplier(DE, ATE).get(0);

        assertEquals(2, acme.occurrences());
        assertEquals(0, acme.damagedAmount().compareTo(new BigDecimal("50")));
        assertEquals(0, acme.missingAmount().compareTo(new BigDecimal("25")));
        assertEquals(0, acme.totalAmount().compareTo(new BigDecimal("75")));
    }

    @Test // CC-11
    void resumo_ordenaDoQueMaisCustou() {
        // É esta ordenação que faz o trabalho: quem está em cima é com quem se vai falar.
        stub(List.of(
                item(7L, "Pequeno", DiscrepancyType.DAMAGED, "1", "10", false),
                item(8L, "Grande", DiscrepancyType.DAMAGED, "10", "100", false)));

        List<SupplierDiscrepancyDTO> resumo = service.summaryBySupplier(DE, ATE);

        assertEquals("Grande", resumo.get(0).supplierName());
        assertEquals("Pequeno", resumo.get(1).supplierName());
    }

    @Test // CC-12
    void resumo_separaOQueJaFoiResolvidoDoQueFaltaReclamar() {
        stub(List.of(
                item(7L, "Acme", DiscrepancyType.DAMAGED, "2", "25", /*resolvido*/ true),
                item(7L, "Acme", DiscrepancyType.MISSING, "4", "25", /*por resolver*/ false)));

        SupplierDiscrepancyDTO acme = service.summaryBySupplier(DE, ATE).get(0);

        assertEquals(0, acme.totalAmount().compareTo(new BigDecimal("150")), "total ocorrido");
        assertEquals(0, acme.openAmount().compareTo(new BigDecimal("100")), "só o que falta reclamar");
    }

    @Test // CC-13
    void resumo_semDivergenciasVemVazio() {
        stub(List.of());
        assertTrue(service.summaryBySupplier(DE, ATE).isEmpty());
    }

    @Test // CC-14
    void periodoInvalidoERecusado() {
        assertThrows(BusinessRuleException.class, () -> service.summaryBySupplier(null, ATE));
        assertThrows(BusinessRuleException.class, () -> service.summaryBySupplier(ATE, DE));
    }

    @Test // CC-15
    void resolver_exigeExplicacao() {
        GoodsReceiptDiscrepancy item = item(7L, "Acme", DiscrepancyType.DAMAGED, "2", "25", false);
        when(repository.findById(5L)).thenReturn(Optional.of(item));

        // Sem explicação, "resolvido" não vale nada daqui a seis meses.
        assertThrows(BusinessRuleException.class, () -> service.resolve(5L, "  "));
        assertFalse(item.isResolved());
    }

    @Test // CC-16
    void resolver_duasVezesERecusado() {
        GoodsReceiptDiscrepancy item = item(7L, "Acme", DiscrepancyType.DAMAGED, "2", "25", true);
        when(repository.findById(5L)).thenReturn(Optional.of(item));

        assertThrows(BusinessRuleException.class,
                () -> service.resolve(5L, "nota de crédito 123"));
    }

    @Test // CC-17
    void resolver_gravaAExplicacao() {
        GoodsReceiptDiscrepancy item = item(7L, "Acme", DiscrepancyType.DAMAGED, "2", "25", false);
        when(repository.findById(5L)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        var dto = service.resolve(5L, "Nota de crédito NC-88 do fornecedor");

        assertTrue(dto.resolved());
        assertEquals("Nota de crédito NC-88 do fornecedor", dto.resolutionNotes());
    }
}
