package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-R03 — porta de leitura de fornecedores, documentos de compra e linhas de negócio (MODULE_DECOUPLING_SPEC §2). */
class SupplierLookupTest {

    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
    private final BusinessUnitRepository units = mock(BusinessUnitRepository.class);
    private final SupplierLookup lookup = new SupplierLookup(suppliers, purchases, orders);
    private final BusinessUnitLookup unitLookup = new BusinessUnitLookup(units);

    private static Supplier supplier(long id) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName("Fornecedor " + id);
        return s;
    }

    @Test
    void require_existente_devolveFornecedor() {
        Supplier s = supplier(4L);
        when(suppliers.findById(4L)).thenReturn(Optional.of(s));

        assertThat(lookup.require(4L)).isSameAs(s);
        assertThat(lookup.find(4L)).containsSame(s);
    }

    @Test
    void require_inexistente_mensagemNormalizada() {
        when(suppliers.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Fornecedor não encontrado.");
    }

    @Test
    void nulos_recusamSemTocarNoRepository() {
        assertThat(lookup.find(null)).isEmpty();
        assertThatThrownBy(() -> lookup.require(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Fornecedor não encontrado.");
        assertThat(unitLookup.findForCompany(null, 1L)).isEmpty();
        verifyNoInteractions(suppliers, purchases, orders, units);
    }

    @Test
    void fornecedores_daEmpresa_delegam() {
        List<Supplier> all = List.of(supplier(1L), supplier(2L));
        List<Supplier> active = List.of(supplier(1L));
        when(suppliers.findByCompanyId(3L)).thenReturn(all);
        when(suppliers.findByCompanyIdAndActiveTrue(3L)).thenReturn(active);

        assertThat(lookup.listForCompany(3L)).isSameAs(all);
        assertThat(lookup.listActiveForCompany(3L)).isSameAs(active);
    }

    @Test
    void documentosDeCompra_delegam() {
        List<Purchase> companyPurchases = List.of(new Purchase());
        List<Purchase> allPurchases = List.of(new Purchase(), new Purchase());
        List<PurchaseOrder> companyOrders = List.of(new PurchaseOrder());
        when(purchases.findByCompanyId(3L)).thenReturn(companyPurchases);
        when(purchases.findAll()).thenReturn(allPurchases);
        when(orders.findByCompanyIdOrderByOrderDateDesc(3L)).thenReturn(companyOrders);

        assertThat(lookup.listPurchasesForCompany(3L)).isSameAs(companyPurchases);
        assertThat(lookup.listAllPurchases()).isSameAs(allPurchases);
        assertThat(lookup.listPurchaseOrdersForCompany(3L)).isSameAs(companyOrders);
        verify(orders).findByCompanyIdOrderByOrderDateDesc(3L);
    }

    @Test
    void linhaDeNegocio_daEmpresa_delega() {
        BusinessUnit unit = new BusinessUnit();
        when(units.findByIdAndCompanyId(6L, 1L)).thenReturn(Optional.of(unit));
        when(units.findByIdAndCompanyId(6L, 2L)).thenReturn(Optional.empty());

        assertThat(unitLookup.findForCompany(6L, 1L)).containsSame(unit);
        assertThat(unitLookup.findForCompany(6L, 2L)).isEmpty();
    }

    @Test
    void saoPortasDeLeituraSoDeLeitura() {
        for (Class<?> type : List.of(SupplierLookup.class, BusinessUnitLookup.class)) {
            assertThat(type).hasAnnotation(Service.class);
            Transactional tx = type.getAnnotation(Transactional.class);
            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }
    }
}
