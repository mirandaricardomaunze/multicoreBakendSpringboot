package mz.multicore.erp.modules.dashboard;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.dashboard.service.OperationalPulseService;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalPulseServiceTest {

    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private FuelLogRepository fuelLogRepository;
    @Mock
    private RecurringContractRepository recurringContractRepository;
    @Mock
    private AssetRepository assetRepository;

    private OperationalPulseService operationalPulseService;

    @BeforeEach
    void setUp() {
        operationalPulseService = new OperationalPulseService(
                productionOrderRepository,
                vehicleRepository,
                fuelLogRepository,
                recurringContractRepository,
                assetRepository
        );
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(10L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void shouldReturnEmptyPulseWhenCompanyHasNoRecords() {
        when(productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(10L)).thenReturn(List.of());
        when(recurringContractRepository.findByCompanyIdWithClient(10L)).thenReturn(List.of());
        when(assetRepository.findByCompanyIdOrderByAssetCodeAsc(10L)).thenReturn(List.of());

        OperationalPulseDTO pulse = operationalPulseService.getOperationalPulse();

        assertThat(pulse).isNotNull();
        assertThat(pulse.manufacturing().plannedOrders()).isZero();
        assertThat(pulse.manufacturing().inProgressOrders()).isZero();
        assertThat(pulse.manufacturing().completedOrders()).isZero();
        assertThat(pulse.manufacturing().totalCipValue()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(pulse.fleet().activeVehicles()).isZero();
        assertThat(pulse.fleet().totalVehicles()).isZero();
        assertThat(pulse.fleet().monthlyFuelLiters()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(pulse.recurring().activeContracts()).isZero();
        assertThat(pulse.recurring().monthlyRecurringRevenue()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(pulse.assets().totalAssets()).isZero();
        assertThat(pulse.assets().netBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldAggregateManufacturingMetricsCorrectly() {
        ProductionOrder o1 = new ProductionOrder();
        o1.setStatus(ProductionOrderStatus.PLANNED);
        o1.setTotalProductionCost(new BigDecimal("15000.00"));

        ProductionOrder o2 = new ProductionOrder();
        o2.setStatus(ProductionOrderStatus.IN_PROGRESS);
        o2.setTotalProductionCost(new BigDecimal("35000.00"));

        ProductionOrder o3 = new ProductionOrder();
        o3.setStatus(ProductionOrderStatus.COMPLETED);
        o3.setTotalProductionCost(new BigDecimal("50000.00"));

        when(productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(o1, o2, o3));
        when(vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(10L)).thenReturn(List.of());
        when(recurringContractRepository.findByCompanyIdWithClient(10L)).thenReturn(List.of());
        when(assetRepository.findByCompanyIdOrderByAssetCodeAsc(10L)).thenReturn(List.of());

        OperationalPulseDTO pulse = operationalPulseService.getOperationalPulse();

        assertThat(pulse.manufacturing().plannedOrders()).isEqualTo(1);
        assertThat(pulse.manufacturing().inProgressOrders()).isEqualTo(1);
        assertThat(pulse.manufacturing().completedOrders()).isEqualTo(1);
        assertThat(pulse.manufacturing().totalCipValue()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void shouldAggregateFleetMetricsCorrectly() {
        Vehicle v1 = new Vehicle();
        v1.setId(1L);
        v1.setLicensePlate("AFG-123-MC");
        v1.setStatus(VehicleStatus.OPERATIONAL);

        FuelLog log = new FuelLog();
        log.setLitersFilled(new BigDecimal("60.00"));
        log.setTotalCost(new BigDecimal("5400.00"));
        log.setConsumptionRateKmPerLiter(new BigDecimal("12.50"));
        log.setLogTimestamp(LocalDateTime.now().minusDays(5));

        when(productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(10L)).thenReturn(List.of(v1));
        when(fuelLogRepository.findByVehicleIdOrderByLogTimestampDesc(1L)).thenReturn(List.of(log));
        when(recurringContractRepository.findByCompanyIdWithClient(10L)).thenReturn(List.of());
        when(assetRepository.findByCompanyIdOrderByAssetCodeAsc(10L)).thenReturn(List.of());

        OperationalPulseDTO pulse = operationalPulseService.getOperationalPulse();

        assertThat(pulse.fleet().activeVehicles()).isEqualTo(1);
        assertThat(pulse.fleet().totalVehicles()).isEqualTo(1);
        assertThat(pulse.fleet().monthlyFuelLiters()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(pulse.fleet().monthlyFuelCost()).isEqualByComparingTo(new BigDecimal("5400.00"));
        assertThat(pulse.fleet().avgEfficiencyKmPerLiter()).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    void shouldAggregateRecurringContractMetricsCorrectly() {
        RecurringContract c1 = new RecurringContract();
        c1.setStatus(ContractStatus.ACTIVE);
        c1.setFrequency(BillingFrequency.MONTHLY);
        c1.setFixedAmount(new BigDecimal("10000.00"));
        c1.setNextBillingDate(LocalDate.now().minusDays(1)); // Due

        RecurringContract c2 = new RecurringContract();
        c2.setStatus(ContractStatus.ACTIVE);
        c2.setFrequency(BillingFrequency.QUARTERLY);
        c2.setFixedAmount(new BigDecimal("30000.00")); // MRR: 10000.00
        c2.setNextBillingDate(LocalDate.now().plusDays(20));

        when(productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(10L)).thenReturn(List.of());
        when(recurringContractRepository.findByCompanyIdWithClient(10L)).thenReturn(List.of(c1, c2));
        when(assetRepository.findByCompanyIdOrderByAssetCodeAsc(10L)).thenReturn(List.of());

        OperationalPulseDTO pulse = operationalPulseService.getOperationalPulse();

        assertThat(pulse.recurring().activeContracts()).isEqualTo(2);
        assertThat(pulse.recurring().monthlyRecurringRevenue()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(pulse.recurring().contractsDueBilling()).isEqualTo(1);
    }

    @Test
    void shouldAggregateAssetMetricsCorrectly() {
        Asset a1 = new Asset();
        a1.setAssetCode("AST-001");
        a1.setStatus(AssetStatus.ACTIVE);
        a1.setAcquisitionCost(new BigDecimal("250000.00"));
        a1.setAccumulatedDepreciation(new BigDecimal("50000.00"));

        when(productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(10L)).thenReturn(List.of());
        when(recurringContractRepository.findByCompanyIdWithClient(10L)).thenReturn(List.of());
        when(assetRepository.findByCompanyIdOrderByAssetCodeAsc(10L)).thenReturn(List.of(a1));

        OperationalPulseDTO pulse = operationalPulseService.getOperationalPulse();

        assertThat(pulse.assets().totalAssets()).isEqualTo(1);
        assertThat(pulse.assets().totalAcquisitionCost()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(pulse.assets().accumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(pulse.assets().netBookValue()).isEqualByComparingTo(new BigDecimal("200000.00"));
    }
}
