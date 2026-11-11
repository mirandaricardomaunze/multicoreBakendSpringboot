package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.model.FleetManifestStop;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Monta e persiste manifestos de carga a partir de Guias de Remessa e viaturas da frota. */
@Component
public class FleetManifestBuilder {

    private final FleetManifestRepository manifestRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryGuideRepository guideRepository;
    private final CompanyLookup companyLookup;
    private final DocumentNumberService documentNumberService;

    public FleetManifestBuilder(FleetManifestRepository manifestRepository,
                                VehicleRepository vehicleRepository,
                                DeliveryGuideRepository guideRepository,
                                CompanyLookup companyLookup,
                                DocumentNumberService documentNumberService) {
        this.manifestRepository = manifestRepository;
        this.vehicleRepository = vehicleRepository;
        this.guideRepository = guideRepository;
        this.companyLookup = companyLookup;
        this.documentNumberService = documentNumberService;
    }

    public FleetManifest createManifest(Long companyId, CreateFleetManifestRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        if (request.driverName() == null || request.driverName().isBlank()) {
            throw new BusinessRuleException("O nome do condutor/motorista é obrigatório.");
        }
        if (request.routeName() == null || request.routeName().isBlank()) {
            throw new BusinessRuleException("A rota de distribuição é obrigatória.");
        }
        if (request.deliveryGuideIds() == null || request.deliveryGuideIds().isEmpty()) {
            throw new BusinessRuleException("Seleccione pelo menos uma Guia de Remessa para o manifesto.");
        }
        Company company = companyLookup.require(companyId);

        Vehicle vehicle = null;
        String vehiclePlate = "N/A";
        String vehicleModel = "Viatura de Transporte";
        if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findByIdAndCompanyId(request.vehicleId(), companyId).orElse(null);
            if (vehicle != null) {
                vehiclePlate = vehicle.getLicensePlate();
                vehicleModel = vehicle.getMake() + " " + vehicle.getModel();
            }
        }

        FleetManifest manifest = new FleetManifest();
        manifest.setCompany(company);
        manifest.setManifestNumber(documentNumberService.next("MC"));
        manifest.setManifestDate(LocalDateTime.now());
        manifest.setStatus("IN_TRANSIT");
        manifest.setVehicle(vehicle);
        manifest.setVehiclePlate(vehiclePlate);
        manifest.setVehicleModel(vehicleModel);
        manifest.setDriverName(request.driverName().trim());
        manifest.setDriverPhone(request.driverPhone() != null ? request.driverPhone().trim() : "");
        manifest.setRouteName(request.routeName().trim());
        manifest.setDepartureTime(request.departureTime() != null ? request.departureTime().trim() : "");
        manifest.setNotes(request.notes());

        BigDecimal totalBoxes = BigDecimal.ZERO;
        BigDecimal totalPackages = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalAmountToCollect = BigDecimal.ZERO;
        int sequence = 1;

        for (Long guideId : request.deliveryGuideIds()) {
            DeliveryGuide guide = guideRepository.findByIdWithLinesAndCompanyId(guideId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada: " + guideId));

            Client client = guide.getClient();
            String clientName = client != null ? client.getName() : (guide.getWalkInName() != null ? guide.getWalkInName() : "Consumidor Final");
            String address = client != null && client.getAddress() != null ? client.getAddress() : "Destino do Cliente";
            String contact = client != null && client.getEmail() != null ? client.getEmail() : "";

            BigDecimal guideBoxes = BigDecimal.ZERO;
            BigDecimal guidePackages = BigDecimal.ZERO;
            for (DeliveryGuideLine line : guide.getLines()) {
                Product product = line.getProduct();
                BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
                guideBoxes = guideBoxes.add(calculateBoxes(product, qty));
                int pkgsPerBox = product != null ? product.getPackagesPerBox() : 1;
                guidePackages = guidePackages.add(BigDecimal.valueOf(pkgsPerBox));
            }

            BigDecimal guideTotal = guide.getTotalAmount() != null ? guide.getTotalAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(guideTotal);
            totalBoxes = totalBoxes.add(guideBoxes);
            totalPackages = totalPackages.add(guidePackages);

            BigDecimal toCollect = BigDecimal.ZERO;
            String paymentTerms = "Faturado / Conta Corrente";
            if (guide.getNotes() != null && guide.getNotes().toLowerCase().contains("pronto")) {
                toCollect = guideTotal;
                paymentTerms = "Pronto Pagamento no Destino";
            }
            totalAmountToCollect = totalAmountToCollect.add(toCollect);

            FleetManifestStop stop = new FleetManifestStop();
            stop.setSequence(sequence++);
            stop.setDocumentType("GUIA_REMESSA");
            stop.setDocumentId(guide.getId());
            stop.setDocumentNumber(guide.getGuideNumber());
            stop.setClient(client);
            stop.setClientName(clientName);
            stop.setDeliveryAddress(address);
            stop.setContactPhone(contact);
            stop.setBoxCount(guideBoxes.setScale(2, RoundingMode.HALF_UP));
            stop.setPackageCount(guidePackages.setScale(2, RoundingMode.HALF_UP));
            stop.setTotalAmount(guideTotal);
            stop.setAmountToCollect(toCollect);
            stop.setPaymentTerms(paymentTerms);
            manifest.addStop(stop);
        }

        manifest.setTotalStops(manifest.getStops().size());
        manifest.setTotalBoxes(totalBoxes.setScale(2, RoundingMode.HALF_UP));
        manifest.setTotalPackages(totalPackages.setScale(2, RoundingMode.HALF_UP));
        manifest.setTotalAmount(totalAmount);
        manifest.setTotalAmountToCollect(totalAmountToCollect);
        return manifestRepository.save(manifest);
    }

    private BigDecimal calculateBoxes(Product product, BigDecimal quantity) {
        if (product == null) return BigDecimal.ZERO.setScale(2);
        int pkgsPerBox = product.getPackagesPerBox();
        int unitsPerPkg = product.getUnitsPerPackage();
        if (pkgsPerBox <= 1 && unitsPerPkg <= 1) return BigDecimal.ZERO.setScale(2);

        long unitsPerBox = (long) pkgsPerBox * (unitsPerPkg > 0 ? unitsPerPkg : 1);
        if (unitsPerBox <= 0) return BigDecimal.ZERO.setScale(2);

        return quantity.divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
    }
}
