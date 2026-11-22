package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.dto.SaveCostCenterRequest;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.repository.CostCenterRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness CC-01 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1): cost centres belong to a company, with a code unique in
 * that company and a type WAREHOUSE or MANUAL. Managers create, rename and deactivate manual centres; a warehouse
 * centre is not edited by hand; a centre with journal lines cannot be deleted; an inactive centre (or one of another
 * company) refuses new postings.
 */
class CostCenterRulesTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTHER_COMPANY_ID = 2L;

    private final CostCenterRepository repository = mock(CostCenterRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final CostCenterService service = new CostCenterService(repository, companyService);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company(COMPANY_ID));
        when(repository.save(any(CostCenter.class))).thenAnswer(invocation -> {
            CostCenter center = invocation.getArgument(0);
            if (center.getId() == null) center.setId(40L);
            return center;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void cc01_criaCentroManualNaEmpresaActiva() {
        CostCenterDTO created = service.create(new SaveCostCenterRequest(" FROTA ", " Frota ", null));

        assertEquals(40L, created.id());
        assertEquals("FROTA", created.code());
        assertEquals("Frota", created.name());
        assertEquals(CostCenterType.MANUAL, created.type());
        assertTrue(created.active());
        assertThat(created.warehouseId()).isNull();
    }

    @Test
    void cc01_codigoUnicoNaEmpresa() {
        when(repository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "FROTA")).thenReturn(true);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.create(new SaveCostCenterRequest("FROTA", "Frota 2", null)));
        assertThat(error.getMessage()).contains("FROTA");
        verify(repository, never()).save(any());
    }

    @Test
    void cc01_codigoENomeObrigatorios() {
        assertThrows(BusinessRuleException.class, () -> service.create(new SaveCostCenterRequest(" ", "Frota", null)));
        assertThrows(BusinessRuleException.class, () -> service.create(new SaveCostCenterRequest("FROTA", "", null)));
    }

    @Test
    void cc01_soGestorOuAdminGere() {
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");

        assertThrows(BusinessRuleException.class,
                () -> service.create(new SaveCostCenterRequest("FROTA", "Frota", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void cc01_renomeiaEDesactivaCentroManual() {
        CostCenter fleet = center(41L, COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(41L)).thenReturn(Optional.of(fleet));

        CostCenterDTO updated = service.update(41L, new SaveCostCenterRequest("FROTA", "Frota ligeira", false));

        assertEquals("Frota ligeira", updated.name());
        assertFalse(updated.active());
        assertEquals("FROTA", updated.code());
    }

    @Test
    void cc01_naoAlteraCodigoParaUmJaUsado() {
        CostCenter fleet = center(41L, COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(41L)).thenReturn(Optional.of(fleet));
        when(repository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "ADM")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.update(41L, new SaveCostCenterRequest("ADM", "Frota", true)));
    }

    @Test
    void cc01_centroDeArmazemNaoSeEditaAMao() {
        CostCenter store = center(42L, COMPANY_ID, "LJ01", CostCenterType.WAREHOUSE, true);
        when(repository.findById(42L)).thenReturn(Optional.of(store));

        assertThrows(BusinessRuleException.class,
                () -> service.update(42L, new SaveCostCenterRequest("LJ01", "Outro nome", true)));
        assertThrows(BusinessRuleException.class, () -> service.delete(42L));
    }

    @Test
    void cc01_naoApagaCentroComLancamentos() {
        CostCenter fleet = center(41L, COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(41L)).thenReturn(Optional.of(fleet));
        when(repository.hasJournalLines(41L)).thenReturn(true);

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.delete(41L));
        assertThat(error.getMessage()).contains("lançamentos");
        verify(repository, never()).delete(any());
    }

    @Test
    void cc01_apagaCentroSemLancamentos() {
        CostCenter fleet = center(41L, COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(41L)).thenReturn(Optional.of(fleet));

        service.delete(41L);

        verify(repository).delete(fleet);
    }

    @Test
    void cc01_centroDeOutraEmpresaNaoSeVeNemSeEdita() {
        CostCenter foreign = center(43L, OTHER_COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(43L)).thenReturn(Optional.of(foreign));

        assertThrows(BusinessRuleException.class,
                () -> service.update(43L, new SaveCostCenterRequest("FROTA", "X", true)));
        assertThrows(BusinessRuleException.class, () -> service.delete(43L));
    }

    @Test
    void cc01_listaOsCentrosDaEmpresaActiva() {
        when(repository.findByCompanyIdOrderByCode(COMPANY_ID)).thenReturn(List.of(
                center(1L, COMPANY_ID, "ADM", CostCenterType.MANUAL, true),
                center(2L, COMPANY_ID, "LJ01", CostCenterType.WAREHOUSE, false)));

        List<CostCenterDTO> listed = service.list();

        assertThat(listed).extracting(CostCenterDTO::code).containsExactly("ADM", "LJ01");
    }

    @Test
    void cc01_centroInactivoRecusaLancamentos() {
        CostCenter closed = center(44L, COMPANY_ID, "FROTA", CostCenterType.MANUAL, false);
        when(repository.findById(44L)).thenReturn(Optional.of(closed));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.requirePostable(COMPANY_ID, 44L));
        assertThat(error.getMessage()).contains("inactivo");
    }

    @Test
    void cc01_centroDeOutraEmpresaRecusaLancamentos() {
        CostCenter foreign = center(43L, OTHER_COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(43L)).thenReturn(Optional.of(foreign));

        assertThrows(BusinessRuleException.class, () -> service.requirePostable(COMPANY_ID, 43L));
    }

    @Test
    void cc01_centroActivoDaEmpresaAceitaLancamentos() {
        CostCenter fleet = center(41L, COMPANY_ID, "FROTA", CostCenterType.MANUAL, true);
        when(repository.findById(41L)).thenReturn(Optional.of(fleet));

        assertSame(fleet, service.requirePostable(COMPANY_ID, 41L));
    }

    private static CostCenter center(Long id, Long companyId, String code, CostCenterType type, boolean active) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCompany(company(companyId));
        center.setCode(code);
        center.setName(code);
        center.setType(type);
        center.setActive(active);
        return center;
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }
}
