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
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.service.VehicleLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Agenda a entrega das guias de remessa aprovadas: dia, período e camião (docs/ENTREGAS_AGENDA_PROVA_SPEC.md
 * §3.1). Agendar não mexe no stock — ele já saiu na aprovação da guia.
 */
@Service
public class DeliveryScheduleService {

    private final DeliveryGuideRepository guideRepository;
    private final DeliveryGuideQueryService queryService;
    private final VehicleLookup vehicleLookup;
    private final AuditLogService auditLogService;

    public DeliveryScheduleService(DeliveryGuideRepository guideRepository, DeliveryGuideQueryService queryService,
                                   VehicleLookup vehicleLookup, AuditLogService auditLogService) {
        this.guideRepository = guideRepository;
        this.queryService = queryService;
        this.vehicleLookup = vehicleLookup;
        this.auditLogService = auditLogService;
    }

    /** Agenda ou reagenda; uma entrega "Não entregue" volta a "Agendada". */
    @Transactional
    public DeliveryScheduleDTO schedule(Long guideId, ScheduleDeliveryRequest request) {
        if (request == null || request.scheduledDate() == null) {
            throw new BusinessRuleException("Indique o dia da entrega.");
        }
        if (request.scheduledDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("O dia da entrega não pode ser anterior a hoje.");
        }
        DeliveryPeriod period = DeliveryPeriod.parse(request.scheduledPeriod());
        if (request.vehicleId() == null) {
            throw new BusinessRuleException("Indique o camião da entrega.");
        }

        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        guideRepository.findForUpdate(guideId, companyId);
        DeliveryGuide guide = queryService.load(guideId);
        if (guide.getStatus() != DeliveryGuideStatus.APPROVED) {
            throw new BusinessRuleException("Só as guias aprovadas podem ser agendadas. Estado actual: "
                    + guide.getStatus().getLabel());
        }
        if (guide.getDeliveryStatus() == DeliveryStatus.DELIVERED) {
            throw new BusinessRuleException("A guia " + guide.getGuideNumber()
                    + " já foi entregue e não pode ser reagendada.");
        }
        Vehicle vehicle = vehicleLookup.requireForCompany(request.vehicleId(), companyId);
        if (vehicle.getStatus() != VehicleStatus.OPERATIONAL) {
            throw new BusinessRuleException("A viatura " + vehicle.getLicensePlate()
                    + " não está operacional (" + vehicle.getStatus().label() + ").");
        }

        boolean rescheduling = guide.getDeliveryStatus() != DeliveryStatus.UNSCHEDULED;
        guide.setDeliveryStatus(DeliveryStatus.SCHEDULED);
        guide.setScheduledDate(request.scheduledDate());
        guide.setScheduledPeriod(period);
        guide.setDeliveryVehicleId(vehicle.getId());
        guide.setDeliveryVehicleLabel(vehicle.getLicensePlate());
        DeliveryGuide saved = guideRepository.save(guide);

        auditLogService.logCurrent("DELIVERY_SCHEDULE", String.format("Guia %s %s para %s (%s), viatura %s.",
                saved.getGuideNumber(), rescheduling ? "reagendada" : "agendada", saved.getScheduledDate(),
                period.getLabel(), saved.getDeliveryVehicleLabel()));
        return toDTO(saved);
    }

    static DeliveryScheduleDTO toDTO(DeliveryGuide guide) {
        DeliveryStatus status = guide.getDeliveryStatus();
        DeliveryPeriod period = guide.getScheduledPeriod();
        return new DeliveryScheduleDTO(
                guide.getId(),
                guide.getGuideNumber(),
                status.name(),
                status.getLabel(),
                guide.getScheduledDate(),
                period != null ? period.name() : null,
                period != null ? period.getLabel() : null,
                guide.getDeliveryVehicleId(),
                guide.getDeliveryVehicleLabel());
    }
}
