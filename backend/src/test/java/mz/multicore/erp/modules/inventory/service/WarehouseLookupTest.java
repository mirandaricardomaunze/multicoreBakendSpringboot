package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
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

/** MD-I01 — porta de leitura de armazéns para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class WarehouseLookupTest {

    private final WarehouseRepository warehouses = mock(WarehouseRepository.class);
    private final WarehouseLookup lookup = new WarehouseLookup(warehouses);

    private static Warehouse warehouse(long id, Long companyId) {
        Warehouse w = new Warehouse();
        w.setId(id);
        if (companyId != null) {
            Company c = new Company();
            c.setId(companyId);
            w.setCompany(c);
        }
        return w;
    }

    @Test
    void require_existente_devolveArmazem() {
        Warehouse w = warehouse(7L, 1L);
        when(warehouses.findById(7L)).thenReturn(Optional.of(w));

        assertThat(lookup.require(7L)).isSameAs(w);
    }

    @Test
    void require_inexistente_mensagemNormalizada() {
        when(warehouses.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Armazém não encontrado.");
    }

    @Test
    void require_idNulo_recusaSemTocarNoRepository() {
        assertThatThrownBy(() -> lookup.require(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Armazém não encontrado.");
        assertThat(lookup.find(null)).isEmpty();
        verifyNoInteractions(warehouses);
    }

    @Test
    void requireForCompany_soDevolveArmazemDaEmpresa() {
        Warehouse own = warehouse(4L, 1L);
        Warehouse other = warehouse(5L, 2L);
        Warehouse orphan = warehouse(6L, null);
        when(warehouses.findById(4L)).thenReturn(Optional.of(own));
        when(warehouses.findById(5L)).thenReturn(Optional.of(other));
        when(warehouses.findById(6L)).thenReturn(Optional.of(orphan));

        assertThat(lookup.requireForCompany(4L, 1L)).isSameAs(own);
        assertThat(lookup.findForCompany(5L, 1L)).isEmpty();
        assertThatThrownBy(() -> lookup.requireForCompany(5L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Armazém não encontrado nesta empresa.");
        assertThatThrownBy(() -> lookup.requireForCompany(6L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Armazém não encontrado nesta empresa.");
    }

    @Test
    void listForCompany_delega() {
        List<Warehouse> all = List.of(warehouse(1L, 3L), warehouse(2L, 3L));
        when(warehouses.findByCompanyId(3L)).thenReturn(all);

        assertThat(lookup.listForCompany(3L)).isEqualTo(all);
    }

    @Test
    void contrato_serviceSoDeLeitura() {
        assertThat(WarehouseLookup.class).hasAnnotation(Service.class);
        Transactional tx = WarehouseLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }

    @Test
    void escritaAlheia_passaPeloComandoDoInventario() {
        Warehouse w = warehouse(10L, 1L);
        when(warehouses.save(w)).thenReturn(w);
        WarehouseCommandService commands = new WarehouseCommandService(warehouses);

        assertThat(commands.saveWarehouse(w)).isSameAs(w);
        verify(warehouses).save(w);
        assertThat(WarehouseCommandService.class).hasAnnotation(Service.class);
        Transactional tx = WarehouseCommandService.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }
}
