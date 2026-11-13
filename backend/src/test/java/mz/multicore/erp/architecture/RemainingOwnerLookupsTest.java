package mz.multicore.erp.architecture;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.assets.service.AssetLookup;
import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.audit.service.AuditLogLookup;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import mz.multicore.erp.modules.crm.service.WorkSheetLookup;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.fiscal.service.TaxRateCommandService;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.fleet.service.VehicleLookup;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.manufacturing.model.BillOfMaterials;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import mz.multicore.erp.modules.manufacturing.service.BillOfMaterialsLookup;
import mz.multicore.erp.modules.manufacturing.service.ProductionOrderLookup;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import mz.multicore.erp.modules.performance.service.SalesGoalBonusCommandService;
import mz.multicore.erp.modules.performance.service.SalesGoalBonusLookup;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import mz.multicore.erp.modules.quality.service.WorkerEquipmentAssignmentLookup;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import mz.multicore.erp.modules.recurring.service.RecurringContractLookup;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MD-R06 — fleet, fiscal, approvals, manufacturing, assets, recurring, performance, audit, quality e crm
 * lidos fora do módulo dono só pelos Lookups desse módulo (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
class RemainingOwnerLookupsTest {

    private static final List<String> OWNERS = List.of(
            "fleet", "fiscal", "approvals", "manufacturing", "assets", "recurring", "performance", "audit",
            "quality", "crm");

    @Test
    void fiscal_taxasLidasPeloLookupEGravadasPeloComando() {
        TaxRateRepository repo = mock(TaxRateRepository.class);
        TaxRateLookup lookup = new TaxRateLookup(repo);
        TaxRate iva = new TaxRate();
        when(repo.count()).thenReturn(4L);
        when(repo.findAll()).thenReturn(List.of(iva));
        when(repo.findByCode("IVA16")).thenReturn(Optional.of(iva));
        when(repo.findByIdAndCompaniesId(5L, 1L)).thenReturn(Optional.of(iva));
        when(repo.findByIdAndCompaniesId(6L, 1L)).thenReturn(Optional.empty());
        when(repo.findDistinctByCompaniesIdAndActiveTrueOrderByTypeAscRateDesc(1L)).thenReturn(List.of(iva));

        assertThat(lookup.countAll()).isEqualTo(4L);
        assertThat(lookup.listAll()).containsExactly(iva);
        assertThat(lookup.findByCode("IVA16")).containsSame(iva);
        assertThat(lookup.findByCode(null)).isEmpty();
        assertThat(lookup.requireForCompany(5L, 1L)).isSameAs(iva);
        assertThatThrownBy(() -> lookup.requireForCompany(6L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Taxa de IVA não encontrada.");
        assertThat(lookup.listActiveForCompany(1L)).containsExactly(iva);

        new TaxRateCommandService(repo).saveTaxRate(iva);
        verify(repo).save(iva);
    }

    @Test
    void audit_aprovacoes_activos_contratos_listagensDelegam() {
        AuditLogRepository logs = mock(AuditLogRepository.class);
        ApprovalRequestRepository approvals = mock(ApprovalRequestRepository.class);
        AssetRepository assets = mock(AssetRepository.class);
        RecurringContractRepository contracts = mock(RecurringContractRepository.class);
        List<AuditLog> l = List.of(new AuditLog());
        List<ApprovalRequest> pending = List.of(new ApprovalRequest());
        List<ApprovalRequest> doc = List.of(new ApprovalRequest(), new ApprovalRequest());
        List<Asset> a = List.of(new Asset());
        List<RecurringContract> c = List.of(new RecurringContract());
        when(logs.findByCompanyIdOrderByEventTimeDesc(1L)).thenReturn(l);
        when(approvals.findByCompanyIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(pending);
        when(approvals.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(1L, "CREDIT", 9L, ApprovalStatus.APPROVED))
                .thenReturn(doc);
        when(assets.findByCompanyIdOrderByAssetCodeAsc(1L)).thenReturn(a);
        when(contracts.findByCompanyIdWithClient(1L)).thenReturn(c);

        assertThat(new AuditLogLookup(logs).listForCompany(1L)).isEqualTo(l);
        ApprovalRequestLookup approvalLookup = new ApprovalRequestLookup(approvals);
        assertThat(approvalLookup.listForCompanyByStatus(1L, ApprovalStatus.PENDING)).isEqualTo(pending);
        assertThat(approvalLookup.listForDocument(1L, "CREDIT", 9L, ApprovalStatus.APPROVED)).isEqualTo(doc);
        assertThat(new AssetLookup(assets).listForCompany(1L)).isEqualTo(a);
        assertThat(new RecurringContractLookup(contracts).listForCompanyWithClient(1L)).isEqualTo(c);
    }

    @Test
    void frota_viaturasAbastecimentosEManifestos() {
        VehicleRepository vehicles = mock(VehicleRepository.class);
        FuelLogRepository fuel = mock(FuelLogRepository.class);
        FleetManifestRepository manifests = mock(FleetManifestRepository.class);
        VehicleLookup lookup = new VehicleLookup(vehicles, fuel, manifests);
        List<Vehicle> v = List.of(new Vehicle());
        List<FuelLog> f = List.of(new FuelLog());
        FleetManifest manifest = new FleetManifest();
        when(vehicles.findByCompanyIdOrderByLicensePlateAsc(1L)).thenReturn(v);
        when(fuel.findByVehicleIdOrderByLogTimestampDesc(3L)).thenReturn(f);
        when(manifests.findByIdAndCompanyIdWithStops(8L, 1L)).thenReturn(Optional.of(manifest));
        when(manifests.findByIdAndCompanyIdWithStops(9L, 1L)).thenReturn(Optional.empty());

        assertThat(lookup.listForCompany(1L)).isEqualTo(v);
        assertThat(lookup.listFuelLogsForVehicle(3L)).isEqualTo(f);
        assertThat(lookup.requireManifestForCompany(8L, 1L)).isSameAs(manifest);
        assertThatThrownBy(() -> lookup.requireManifestForCompany(9L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Manifesto de carga não encontrado.");
    }

    @Test
    void producao_ordensEReceitas() {
        ProductionOrderRepository orders = mock(ProductionOrderRepository.class);
        BillOfMaterialsRepository boms = mock(BillOfMaterialsRepository.class);
        List<ProductionOrder> o = List.of(new ProductionOrder());
        BillOfMaterials bom = new BillOfMaterials();
        when(orders.findByCompanyIdOrderByCreatedAtDesc(1L)).thenReturn(o);
        when(boms.findByCompanyIdAndActiveTrueOrderByNameAsc(1L)).thenReturn(List.of(bom));
        when(boms.findByIdAndCompanyId(4L, 1L)).thenReturn(Optional.of(bom));
        when(boms.findByIdAndCompanyId(5L, 1L)).thenReturn(Optional.empty());

        assertThat(new ProductionOrderLookup(orders).listForCompany(1L)).isEqualTo(o);
        BillOfMaterialsLookup bomLookup = new BillOfMaterialsLookup(boms);
        assertThat(bomLookup.listActiveForCompany(1L)).containsExactly(bom);
        assertThat(bomLookup.requireForCompany(4L, 1L)).isSameAs(bom);
        assertThatThrownBy(() -> bomLookup.requireForCompany(5L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Receita não encontrada.");
    }

    @Test
    void premios_soAprovadosSemReciboELiquidadosNoRecibo() {
        SalesGoalBonusRepository repo = mock(SalesGoalBonusRepository.class);
        SalesGoalBonus porLiquidar = new SalesGoalBonus();
        SalesGoalBonus jaNoRecibo = new SalesGoalBonus();
        jaNoRecibo.setPayslip(new Payslip());
        when(repo.findByCompanyIdAndEmployeeIdAndStatus(1L, 2L, BonusStatus.APPROVED))
                .thenReturn(List.of(porLiquidar, jaNoRecibo));

        assertThat(new SalesGoalBonusLookup(repo).listApprovedUnsettledForEmployee(1L, 2L))
                .containsExactly(porLiquidar);

        Payslip payslip = new Payslip();
        new SalesGoalBonusCommandService(repo).settleOnPayslip(List.of(porLiquidar), payslip);
        assertThat(porLiquidar.getPayslip()).isSameAs(payslip);
        assertThat(porLiquidar.getStatus()).isEqualTo(BonusStatus.PAID);
        verify(repo).save(porLiquidar);
    }

    @Test
    void qualidadeECrm_documentosParaImpressao() {
        WorkerEquipmentAssignmentRepository assignments = mock(WorkerEquipmentAssignmentRepository.class);
        WorkSheetRepository sheets = mock(WorkSheetRepository.class);
        WorkerEquipmentAssignment assignment = new WorkerEquipmentAssignment();
        WorkSheet sheet = new WorkSheet();
        when(assignments.findByIdWithDetailsAndCompanyId(3L, 1L)).thenReturn(Optional.of(assignment));
        when(assignments.findByIdWithDetailsAndCompanyId(4L, 1L)).thenReturn(Optional.empty());
        when(sheets.findByIdAndSupportTicketCompanyId(6L, 1L)).thenReturn(Optional.of(sheet));
        when(sheets.findByIdAndSupportTicketCompanyId(7L, 1L)).thenReturn(Optional.empty());

        WorkerEquipmentAssignmentLookup assignmentLookup = new WorkerEquipmentAssignmentLookup(assignments);
        assertThat(assignmentLookup.requireForCompany(3L, 1L)).isSameAs(assignment);
        assertThatThrownBy(() -> assignmentLookup.requireForCompany(4L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Atribuição de equipamento não encontrada.");
        WorkSheetLookup sheetLookup = new WorkSheetLookup(sheets);
        assertThat(sheetLookup.requireForCompany(6L, 1L)).isSameAs(sheet);
        assertThatThrownBy(() -> sheetLookup.requireForCompany(7L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Folha de obra não encontrada.");
    }

    @Test
    void idsNulos_naoTocamNoRepository() {
        TaxRateRepository taxes = mock(TaxRateRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        FuelLogRepository fuel = mock(FuelLogRepository.class);
        FleetManifestRepository manifests = mock(FleetManifestRepository.class);
        AuditLogRepository logs = mock(AuditLogRepository.class);
        SalesGoalBonusRepository bonuses = mock(SalesGoalBonusRepository.class);
        WorkSheetRepository sheets = mock(WorkSheetRepository.class);

        assertThatThrownBy(() -> new TaxRateLookup(taxes).requireForCompany(null, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Taxa de IVA não encontrada.");
        assertThat(new VehicleLookup(vehicles, fuel, manifests).listForCompany(null)).isEmpty();
        assertThat(new VehicleLookup(vehicles, fuel, manifests).listFuelLogsForVehicle(null)).isEmpty();
        assertThat(new AuditLogLookup(logs).listForCompany(null)).isEmpty();
        assertThat(new SalesGoalBonusLookup(bonuses).listApprovedUnsettledForEmployee(1L, null)).isEmpty();
        assertThatThrownBy(() -> new WorkSheetLookup(sheets).requireForCompany(null, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Folha de obra não encontrada.");
        verifyNoInteractions(taxes, vehicles, fuel, manifests, logs, bonuses, sheets);
    }

    @Test
    void contrato_lookupsSoDeLeitura() {
        for (Class<?> lookup : List.of(TaxRateLookup.class, AuditLogLookup.class, ApprovalRequestLookup.class,
                AssetLookup.class, RecurringContractLookup.class, VehicleLookup.class, ProductionOrderLookup.class,
                BillOfMaterialsLookup.class, SalesGoalBonusLookup.class, WorkerEquipmentAssignmentLookup.class,
                WorkSheetLookup.class)) {
            assertThat(lookup).hasAnnotation(Service.class);
            Transactional tx = lookup.getAnnotation(Transactional.class);
            assertThat(tx).as(lookup.getSimpleName()).isNotNull();
            assertThat(tx.readOnly()).as(lookup.getSimpleName()).isTrue();
            List<Method> publicMethods = Arrays.stream(lookup.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers())).toList();
            assertThat(publicMethods).as(lookup.getSimpleName()).isNotEmpty()
                    .allMatch(m -> m.getName().startsWith("find") || m.getName().startsWith("list")
                            || m.getName().startsWith("require") || m.getName().startsWith("count"))
                    .noneMatch(m -> m.getReturnType() == void.class);
        }
    }

    /** Nenhum módulo fora do dono importa um repositório de fleet, fiscal, approvals, ... ou crm. */
    @Test
    void foraDoDono_naoHaRepositoriosDestesModulos() throws IOException {
        Path main = moduleRoot().resolve("src/main/java/mz/multicore/erp");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String owner : OWNERS) {
                    if (!file.startsWith(main.resolve("modules/" + owner))
                            && source.contains("modules." + owner + ".repository.")) {
                        offenders.add(main.relativize(file) + " -> " + owner);
                    }
                }
            }
        }
        assertThat(offenders).as("Repositórios destes módulos lidos fora do dono passam pelos Lookups").isEmpty();
    }

    private static Path moduleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("backend");
    }
}
