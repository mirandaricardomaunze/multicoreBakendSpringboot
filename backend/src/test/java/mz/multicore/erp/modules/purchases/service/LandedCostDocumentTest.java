package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostDocumentDTO;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.LandedCostDocument;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseDocumentKind;
import mz.multicore.erp.modules.purchases.repository.LandedCostDocumentRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LC-01 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1): the "Custos de Importação" document is numbered
 * {@code CI-yyyy/n} per company and links to one or more received ({@code COMPLETED}) purchases of the active
 * company; a purchase may receive several CI documents.
 */
class LandedCostDocumentTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTHER_COMPANY_ID = 2L;

    private final LandedCostDocumentRepository documents = mock(LandedCostDocumentRepository.class);
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final LandedCostDocumentService service = new LandedCostDocumentService(documents, purchases, numbers);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("comprador", "MANAGER");
        when(documents.save(any(LandedCostDocument.class))).thenAnswer(invocation -> {
            LandedCostDocument document = invocation.getArgument(0);
            document.setId(50L);
            return document;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void lc01_serieCi() {
        assertEquals("CI", DocumentSeries.LANDED_COST);
        assertThat(DocumentSeries.ALL).contains(DocumentSeries.LANDED_COST);
    }

    @Test
    void lc01_numeraNaSerieCiELigaAsComprasRecebidasDaEmpresa() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        stubPurchase(11L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/5");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1");

        LandedCostDocumentDTO dto = service.register(new CreateLandedCostRequest(
                List.of(10L, 11L, 10L), LandedCostAllocationMethod.PESO, LocalDate.of(2026, 11, 20)));

        assertEquals(50L, dto.id());
        assertEquals("CI-2026/1", dto.documentNumber());
        assertEquals(LocalDate.of(2026, 11, 20), dto.documentDate());
        assertEquals(LandedCostAllocationMethod.PESO, dto.allocationMethod());
        assertThat(dto.purchaseIds()).containsExactly(10L, 11L);
        assertThat(dto.purchaseNumbers()).containsExactly("V/FT-2026/4", "V/FT-2026/5");
    }

    @Test
    void lc01_metodoPorOmissaoValorEDataDeHoje() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1");

        LandedCostDocumentDTO dto = service.register(new CreateLandedCostRequest(List.of(10L), null, null));

        assertEquals(LandedCostAllocationMethod.VALOR, dto.allocationMethod());
        assertEquals(LocalDate.now(), dto.documentDate());
    }

    @Test
    void lc01_umaCompraPodeReceberVariosDocumentosCi() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1", "CI-2026/2");

        LandedCostDocumentDTO first = service.register(new CreateLandedCostRequest(List.of(10L), null, null));
        LandedCostDocumentDTO second = service.register(new CreateLandedCostRequest(List.of(10L), null, null));

        assertEquals("CI-2026/1", first.documentNumber());
        assertEquals("CI-2026/2", second.documentNumber());
        assertThat(second.purchaseIds()).containsExactly(10L);
    }

    @Test
    void lc01_semComprasEhErro() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(), null, null)));
        assertThat(error.getMessage()).contains("pelo menos uma compra");
        verify(numbers, never()).next(any());
    }

    @Test
    void lc01_compraDeOutraEmpresaNaoEhEncontrada() {
        stubPurchase(10L, OTHER_COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(10L), null, null)));
        assertThat(error.getMessage()).contains("não encontrada");
        verify(documents, never()).save(any());
    }

    @Test
    void lc01_compraInexistenteNaoEhEncontrada() {
        when(purchases.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(99L), null, null)));
        verify(documents, never()).save(any());
    }

    @Test
    void lc01_compraAnuladaEhRecusada() {
        stubPurchase(10L, COMPANY_ID, "CANCELLED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(10L), null, null)));
        assertThat(error.getMessage()).contains("V/FT-2026/4").contains("recebida");
        verify(numbers, never()).next(any());
    }

    @Test
    void lc01_saldoInicialFornecedorNaoEhCompraRecebida() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.OPENING_BALANCE, "SIF-2026/1");

        assertThrows(BusinessRuleException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(10L), null, null)));
        verify(documents, never()).save(any());
    }

    @Test
    void lc01_perfilSemPermissaoDeComprasEhRecusado() {
        CurrentUserContext.setCurrentUser("caixa", "SELLER");
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        assertThrows(RuntimeException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(10L), null, null)));
        verify(documents, never()).save(any());
    }

    private void stubPurchase(Long id, Long companyId, String status, PurchaseDocumentKind kind, String number) {
        Company company = new Company();
        company.setId(companyId);
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setCompany(company);
        purchase.setStatus(status);
        purchase.setDocumentKind(kind);
        purchase.setPurchaseNumber(number);
        when(purchases.findById(id)).thenReturn(Optional.of(purchase));
    }
}
