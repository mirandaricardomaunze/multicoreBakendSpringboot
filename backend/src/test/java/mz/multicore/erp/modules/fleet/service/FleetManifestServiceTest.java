package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FleetManifestServiceTest {

    private FleetManifestRepository manifestRepository;
    private VehicleRepository vehicleRepository;
    private DeliveryGuideRepository guideRepository;
    private CompanyRepository companyRepository;
    private DocumentNumberService documentNumberService;
    private FleetManifestService service;

    private Company company;
    private Vehicle vehicle;

    @BeforeEach
    void setup() {
        manifestRepository = mock(FleetManifestRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        guideRepository = mock(DeliveryGuideRepository.class);
        companyRepository = mock(CompanyRepository.class);
        documentNumberService = mock(DocumentNumberService.class);

        service = new FleetManifestService(
                manifestRepository, vehicleRepository, guideRepository, companyRepository, documentNumberService
        );

        company = new Company();
        company.setId(10L);
        company.setName("Multicore Teste Lda");

        vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setCompany(company);
        vehicle.setLicensePlate("ABC-123-MC");
        vehicle.setMake("Toyota");
        vehicle.setModel("Hino 300");

        CurrentUserContext.setCurrentCompanyId(10L);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(vehicleRepository.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(vehicle));
        when(documentNumberService.next("MC")).thenReturn("MC-2026/1");
        when(manifestRepository.save(any(FleetManifest.class))).thenAnswer(inv -> {
            FleetManifest m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("Cria manifesto agregando caixas, embalagens, paragens e valores correctamente")
    void createManifest_success() {
        Client c1 = new Client();
        c1.setId(201L);
        c1.setName("Supermercado Central");
        c1.setAddress("Av. Eduardo Mondlane, 123");
        c1.setEmail("super@central.co.mz");

        Product p1 = new Product();
        p1.setId(501L);
        p1.setName("Óleo Alimentar 1L");
        p1.setPackagesPerBox(12);
        p1.setUnitsPerPackage(1);

        DeliveryGuide g1 = new DeliveryGuide();
        g1.setId(1001L);
        g1.setCompany(company);
        g1.setGuideNumber("GR-2026/1");
        g1.setClient(c1);
        g1.setTotalAmount(new BigDecimal("15000.00"));
        g1.setNotes("Entrega com pronto pagamento no acto");

        DeliveryGuideLine l1 = new DeliveryGuideLine();
        l1.setProduct(p1);
        l1.setQuantity(new BigDecimal("24.000")); // 24 unidades = 2 caixas
        g1.addLine(l1);

        when(guideRepository.findByIdWithLinesAndCompanyId(1001L, 10L)).thenReturn(Optional.of(g1));

        CreateFleetManifestRequest request = new CreateFleetManifestRequest(
                5L,
                "Carlos Sitoe",
                "829988776",
                "Rota Maputo Centro",
                "08:30",
                "Carga prioritária",
                List.of(1001L)
        );

        FleetManifestDTO dto = service.createManifest(10L, request);

        assertNotNull(dto);
        assertEquals("MC-2026/1", dto.manifestNumber());
        assertEquals("ABC-123-MC", dto.vehiclePlate());
        assertEquals("Carlos Sitoe", dto.driverName());
        assertEquals("Rota Maputo Centro", dto.routeName());
        assertEquals(1, dto.totalStops());
        assertEquals(new BigDecimal("2.00"), dto.totalBoxes());
        assertEquals(new BigDecimal("15000.00"), dto.totalAmount());
        assertEquals(new BigDecimal("15000.00"), dto.totalAmountToCollect());
        assertEquals(1, dto.stops().size());

        var stop = dto.stops().get(0);
        assertEquals(1, stop.sequence());
        assertEquals("Supermercado Central", stop.clientName());
        assertEquals("GR-2026/1", stop.documentNumber());
        assertEquals(new BigDecimal("2.00"), stop.boxCount());
        assertEquals(new BigDecimal("15000.00"), stop.amountToCollect());
    }

    @Test
    @DisplayName("Valida campos obrigatórios de condutor, rota e documentos")
    void createManifest_validations() {
        assertThrows(BusinessRuleException.class, () ->
                service.createManifest(10L, new CreateFleetManifestRequest(
                        5L, "", "829988776", "Rota 1", "08:00", "", List.of(1L)
                )));

        assertThrows(BusinessRuleException.class, () ->
                service.createManifest(10L, new CreateFleetManifestRequest(
                        5L, "Motorista", "829988776", "", "08:00", "", List.of(1L)
                )));

        assertThrows(BusinessRuleException.class, () ->
                service.createManifest(10L, new CreateFleetManifestRequest(
                        5L, "Motorista", "829988776", "Rota 1", "08:00", "", List.of()
                )));
    }

    @Test
    @DisplayName("Actualiza estado do manifesto para CONCLUIDO")
    void updateStatus_success() {
        FleetManifest m = new FleetManifest();
        m.setId(100L);
        m.setCompany(company);
        m.setManifestNumber("MC-2026/1");
        m.setStatus("IN_TRANSIT");

        when(manifestRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(m));
        when(manifestRepository.save(any(FleetManifest.class))).thenAnswer(inv -> inv.getArgument(0));

        FleetManifestDTO updated = service.updateStatus(10L, 100L, "COMPLETED");

        assertEquals("COMPLETED", updated.status());
        verify(manifestRepository).save(m);
    }
}
