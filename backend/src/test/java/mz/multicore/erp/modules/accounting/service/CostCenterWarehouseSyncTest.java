package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.WarehouseSavedEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.repository.CostCenterRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.WarehouseCommandService;
import mz.multicore.erp.modules.inventory.service.WarehouseLifecycleService;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness CC-02 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1): creating a warehouse creates its cost centre (code = the
 * warehouse number), existing warehouses receive one (migration V150 in PostgreSQL, start-up backfill elsewhere) and
 * the warehouse centre follows the warehouse's name and state.
 */
class CostCenterWarehouseSyncTest {

    private static final Long COMPANY_ID = 1L;

    private final CostCenterRepository repository = mock(CostCenterRepository.class);
    private final CostCenterService costCenterService = mock(CostCenterService.class);
    private final CompanyLookup companyLookup = mock(CompanyLookup.class);
    private final WarehouseLookup warehouseLookup = mock(WarehouseLookup.class);
    private final CostCenterWarehouseSync sync =
            new CostCenterWarehouseSync(repository, costCenterService, companyLookup, warehouseLookup);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(companyLookup.reference(COMPANY_ID)).thenReturn(company(COMPANY_ID));
        when(repository.findByWarehouseId(any())).thenReturn(Optional.empty());
        when(repository.save(any(CostCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void cc02_criarArmazemCriaOSeuCentroComOCodigoDoArmazem() {
        sync.onWarehouseSaved(new WarehouseSavedEvent(7L, COMPANY_ID, " LJ01 ", "Loja da Baixa", true));

        CostCenter center = savedCenter();
        assertEquals("LJ01", center.getCode());
        assertEquals("Loja da Baixa", center.getName());
        assertEquals(CostCenterType.WAREHOUSE, center.getType());
        assertEquals(7L, center.getWarehouseId());
        assertTrue(center.isActive());
        assertFalse(center.isDefaultCenter());
        assertTrue(center.belongsTo(COMPANY_ID));
    }

    @Test
    void cc02_armazemSemNumeroOuComCodigoOcupadoUsaArmId() {
        sync.onWarehouseSaved(new WarehouseSavedEvent(8L, COMPANY_ID, null, "Depósito", true));
        when(repository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "ADM")).thenReturn(true);
        sync.onWarehouseSaved(new WarehouseSavedEvent(9L, COMPANY_ID, "ADM", "Armazém ADM", true));
        when(repository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "X")).thenReturn(true);
        when(repository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "ARM-10")).thenReturn(true);
        sync.onWarehouseSaved(new WarehouseSavedEvent(10L, COMPANY_ID, "X", "Armazém X", true));

        List<CostCenter> saved = savedCenters(3);
        assertEquals("ARM-8", saved.get(0).getCode());
        assertEquals("ARM-9", saved.get(1).getCode());
        assertEquals("ARM-10-2", saved.get(2).getCode());
    }

    @Test
    void cc02_centroAcompanhaNomeEEstadoDoArmazemSemMudarCodigo() {
        CostCenter existing = warehouseCenter(50L, 7L, "LJ01", "Loja da Baixa");
        when(repository.findByWarehouseId(7L)).thenReturn(Optional.of(existing));

        sync.onWarehouseSaved(new WarehouseSavedEvent(7L, COMPANY_ID, "NOVO", "Loja da Baixa II", false));

        assertEquals("LJ01", existing.getCode());
        assertEquals("Loja da Baixa II", existing.getName());
        assertFalse(existing.isActive());
        verify(repository).save(existing);

        sync.onWarehouseSaved(new WarehouseSavedEvent(7L, COMPANY_ID, "NOVO", "Loja da Baixa II", true));
        assertTrue(existing.isActive());
        verify(costCenterService, never()).requireDefault(any());
    }

    @Test
    void cc02_armazemDesactivadoDevolveOCentroPorOmissaoAoAdm() {
        CostCenter existing = warehouseCenter(50L, 7L, "LJ01", "Loja da Baixa");
        existing.setDefaultCenter(true);
        when(repository.findByWarehouseId(7L)).thenReturn(Optional.of(existing));

        sync.onWarehouseSaved(new WarehouseSavedEvent(7L, COMPANY_ID, "LJ01", "Loja da Baixa", false));

        assertFalse(existing.isActive());
        assertFalse(existing.isDefaultCenter());
        verify(costCenterService).requireDefault(COMPANY_ID);
    }

    @Test
    void cc02_arranqueDaCentroAosArmazensExistentesQueNaoOTem() {
        when(companyLookup.findAll()).thenReturn(List.of(company(COMPANY_ID)));
        Warehouse withCenter = warehouse(7L, "LJ01", "Loja da Baixa", true);
        Warehouse without = warehouse(8L, "DP01", "Depósito Porto", false);
        when(warehouseLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(withCenter, without));
        when(repository.findByWarehouseId(7L)).thenReturn(Optional.of(warehouseCenter(50L, 7L, "LJ01", "Loja")));

        sync.run(null);

        CostCenter created = savedCenter();
        assertEquals(8L, created.getWarehouseId());
        assertEquals("DP01", created.getCode());
        assertEquals("Depósito Porto", created.getName());
        assertFalse(created.isActive());
    }

    @Test
    void cc02_gravarArmazemAnunciaOEventoComNomeEEstado() {
        WarehouseRepository warehouses = mock(WarehouseRepository.class);
        when(warehouses.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse w = invocation.getArgument(0);
            if (w.getId() == null) w.setId(11L);
            return w;
        });
        List<Object> events = new ArrayList<>();
        WarehouseCommandService commands = new WarehouseCommandService(warehouses, events::add);
        WarehouseQueryService query = mock(WarehouseQueryService.class);
        WarehouseLifecycleService lifecycle = new WarehouseLifecycleService(commands, companyLookup,
                mock(CompanyService.class), mock(AuditLogService.class), query);

        lifecycle.createWarehouse("Loja Matola", "LJ02", null, null, null, true, null, null, company(COMPANY_ID));
        Warehouse stored = warehouse(11L, "LJ02", "Loja Matola", true);
        when(query.loadWarehouseForActiveCompany(11L)).thenReturn(stored);
        lifecycle.setWarehouseActive(11L, false);

        assertThat(events).containsExactly(
                new WarehouseSavedEvent(11L, COMPANY_ID, "LJ02", "Loja Matola", true),
                new WarehouseSavedEvent(11L, COMPANY_ID, "LJ02", "Loja Matola", false));
    }

    @Test
    void cc02_migracaoDaCentroAosArmazensExistentes() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V150__cost_center_warehouses.sql");
        String sql = Files.readString(Files.exists(migration) ? migration : Path.of("backend").resolve(migration));

        assertThat(sql).contains("INSERT INTO cost_centers").contains("FROM warehouses")
                .contains("'WAREHOUSE'").contains("'ARM-'")
                .contains("NOT EXISTS (SELECT 1 FROM cost_centers c WHERE c.warehouse_id = w.id)")
                .contains("uk_cost_centers_warehouse");
    }

    private CostCenter savedCenter() {
        return savedCenters(1).get(0);
    }

    private List<CostCenter> savedCenters(int count) {
        ArgumentCaptor<CostCenter> captor = ArgumentCaptor.forClass(CostCenter.class);
        verify(repository, times(count)).save(captor.capture());
        return captor.getAllValues();
    }

    private static CostCenter warehouseCenter(Long id, Long warehouseId, String code, String name) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCompany(company(COMPANY_ID));
        center.setCode(code);
        center.setName(name);
        center.setType(CostCenterType.WAREHOUSE);
        center.setWarehouseId(warehouseId);
        center.setActive(true);
        return center;
    }

    private static Warehouse warehouse(Long id, String number, String name, boolean active) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setWarehouseNumber(number);
        warehouse.setName(name);
        warehouse.setActive(active);
        warehouse.setCompany(company(COMPANY_ID));
        return warehouse;
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }
}
