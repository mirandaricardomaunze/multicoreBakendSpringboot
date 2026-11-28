package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.DeliveryScheduleDTO;
import mz.multicore.erp.modules.comercial.dto.ScheduleDeliveryRequest;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.model.DeliveryPeriod;
import mz.multicore.erp.modules.comercial.model.DeliveryStatus;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.service.VehicleLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Agenda de entregas das guias de remessa (docs/ENTREGAS_AGENDA_PROVA_SPEC.md §3.1). */
class DeliveryScheduleRulesTest {

    private static final long COMPANY = 1L;

    private DeliveryGuideRepository guides;
    private DeliveryGuideQueryService queries;
    private VehicleLookup vehicles;
    private DeliveryScheduleService service;
    private DeliveryGuide guide;
    private Vehicle truck;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("vendedor", "SELLER");
        guides = mock(DeliveryGuideRepository.class);
        queries = mock(DeliveryGuideQueryService.class);
        vehicles = mock(VehicleLookup.class);
        service = new DeliveryScheduleService(guides, queries, vehicles, mock(AuditLogService.class));

        guide = new DeliveryGuide();
        guide.setId(7L);
        guide.setGuideNumber("GR-2026/7");
        guide.setStatus(DeliveryGuideStatus.APPROVED);
        Company company = new Company();
        company.setId(COMPANY);
        truck = new Vehicle();
        truck.setId(30L);
        truck.setCompany(company);
        truck.setLicensePlate("AAA-123-MC");
        truck.setStatus(VehicleStatus.OPERATIONAL);

        when(guides.findForUpdate(7L, COMPANY)).thenReturn(Optional.of(guide));
        when(queries.load(7L)).thenReturn(guide);
        when(vehicles.requireForCompany(30L, COMPANY)).thenReturn(truck);
        when(guides.save(any(DeliveryGuide.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private static ScheduleDeliveryRequest request(LocalDate day, String period, Long vehicleId) {
        return new ScheduleDeliveryRequest(day, period, vehicleId);
    }

    @Test
    void en01_guiaNovaNasceSemAgenda() {
        DeliveryGuide fresh = new DeliveryGuide();
        assertEquals(DeliveryStatus.UNSCHEDULED, fresh.getDeliveryStatus());
        assertEquals("Por agendar", DeliveryStatus.UNSCHEDULED.getLabel());
        assertEquals("Agendada", DeliveryStatus.SCHEDULED.getLabel());
        assertEquals("Entregue", DeliveryStatus.DELIVERED.getLabel());
        assertEquals("Não entregue", DeliveryStatus.NOT_DELIVERED.getLabel());
        assertEquals("Manhã", DeliveryPeriod.MORNING.getLabel());
        assertEquals("Tarde", DeliveryPeriod.AFTERNOON.getLabel());
    }

    @Test
    void en01_agendarGuiaAprovada_ficaAgendadaComDiaPeriodoECamiao() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        DeliveryScheduleDTO dto = service.schedule(7L, request(tomorrow, "MORNING", 30L));

        assertEquals(DeliveryStatus.SCHEDULED, guide.getDeliveryStatus());
        assertEquals(tomorrow, guide.getScheduledDate());
        assertEquals(DeliveryPeriod.MORNING, guide.getScheduledPeriod());
        assertEquals(30L, guide.getDeliveryVehicleId());
        assertEquals("AAA-123-MC", guide.getDeliveryVehicleLabel());
        assertEquals("SCHEDULED", dto.deliveryStatus());
        assertEquals("Agendada", dto.deliveryStatusLabel());
        assertEquals("MORNING", dto.scheduledPeriod());
        assertEquals("Manhã", dto.scheduledPeriodLabel());
        assertEquals("AAA-123-MC", dto.vehicleLabel());
        verify(guides).findForUpdate(7L, COMPANY);
    }

    @Test
    void en01_agendarParaHoje_aceite() {
        service.schedule(7L, request(LocalDate.now(), "AFTERNOON", 30L));
        assertEquals(DeliveryPeriod.AFTERNOON, guide.getScheduledPeriod());
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryGuideStatus.class, names = "APPROVED", mode = EnumSource.Mode.EXCLUDE)
    void en01_guiaNaoAprovada_recusada(DeliveryGuideStatus status) {
        guide.setStatus(status);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.schedule(7L, request(LocalDate.now().plusDays(1), "MORNING", 30L)));

        assertTrue(error.getMessage().contains("aprovadas"), error.getMessage());
        assertEquals(DeliveryStatus.UNSCHEDULED, guide.getDeliveryStatus());
        verify(guides, never()).save(any());
    }

    @Test
    void en01_diaNoPassado_recusado() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.schedule(7L, request(LocalDate.now().minusDays(1), "MORNING", 30L)));
        assertTrue(error.getMessage().contains("anterior a hoje"), error.getMessage());
        verify(guides, never()).save(any());
    }

    @Test
    void en01_camposObrigatoriosEPeriodoInvalido_recusados() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThrows(BusinessRuleException.class, () -> service.schedule(7L, request(null, "MORNING", 30L)));
        assertThrows(BusinessRuleException.class, () -> service.schedule(7L, request(tomorrow, null, 30L)));
        assertThrows(BusinessRuleException.class, () -> service.schedule(7L, request(tomorrow, "NIGHT", 30L)));
        assertThrows(BusinessRuleException.class, () -> service.schedule(7L, request(tomorrow, "MORNING", null)));
        verify(guides, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class, names = "OPERATIONAL", mode = EnumSource.Mode.EXCLUDE)
    void en01_viaturaInactiva_recusada(VehicleStatus status) {
        truck.setStatus(status);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.schedule(7L, request(LocalDate.now().plusDays(1), "MORNING", 30L)));

        assertTrue(error.getMessage().contains("AAA-123-MC"), error.getMessage());
        verify(guides, never()).save(any());
    }

    @Test
    void en01_viaturaDeOutraEmpresa_recusada() {
        when(vehicles.requireForCompany(99L, COMPANY))
                .thenThrow(new BusinessRuleException("Viatura não encontrada."));

        assertThrows(BusinessRuleException.class,
                () -> service.schedule(7L, request(LocalDate.now().plusDays(1), "MORNING", 99L)));
        verify(guides, never()).save(any());
    }

    @Test
    void en01_guiaDeOutraEmpresa_recusada() {
        when(guides.findForUpdate(8L, COMPANY)).thenReturn(Optional.empty());
        when(queries.load(8L)).thenThrow(new BusinessRuleException("Guia de remessa não encontrada."));

        assertThrows(BusinessRuleException.class,
                () -> service.schedule(8L, request(LocalDate.now().plusDays(1), "MORNING", 30L)));
        verify(guides, never()).save(any());
    }

    @Test
    void en01_reagendarEntregaAgendada_mudaDiaECamiao() {
        service.schedule(7L, request(LocalDate.now().plusDays(1), "MORNING", 30L));
        Vehicle other = new Vehicle();
        other.setId(31L);
        other.setLicensePlate("BBB-456-MC");
        other.setStatus(VehicleStatus.OPERATIONAL);
        when(vehicles.requireForCompany(31L, COMPANY)).thenReturn(other);

        service.schedule(7L, request(LocalDate.now().plusDays(2), "AFTERNOON", 31L));

        assertEquals(DeliveryStatus.SCHEDULED, guide.getDeliveryStatus());
        assertEquals(LocalDate.now().plusDays(2), guide.getScheduledDate());
        assertEquals(31L, guide.getDeliveryVehicleId());
        assertEquals("BBB-456-MC", guide.getDeliveryVehicleLabel());
    }

    @Test
    void en01_entregaJaEntregue_naoSeReagenda() {
        guide.setDeliveryStatus(DeliveryStatus.DELIVERED);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.schedule(7L, request(LocalDate.now().plusDays(1), "MORNING", 30L)));

        assertTrue(error.getMessage().contains("entregue"), error.getMessage());
        assertEquals(DeliveryStatus.DELIVERED, guide.getDeliveryStatus());
        verify(guides, never()).save(any());
    }
}
