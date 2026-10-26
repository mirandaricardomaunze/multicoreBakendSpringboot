package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.model.FleetManifestStop;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetManifestPrintServiceTest {

    private FleetManifestRepository manifestRepository;
    private FleetManifestPrintService service;

    @BeforeEach
    void setup() {
        manifestRepository = mock(FleetManifestRepository.class);
        service = new FleetManifestPrintService(manifestRepository);
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("Renderiza Manifesto de Carga A4 executivo com dados da viatura, rota, KPIs e paragens")
    void renderA4_generatesValidPdfDocument() throws IOException {
        Company company = new Company();
        company.setId(1L);
        company.setName("Distribuidora Moçambique Lda");
        company.setTaxId("400123456");

        FleetManifest manifest = new FleetManifest();
        manifest.setId(50L);
        manifest.setCompany(company);
        manifest.setManifestNumber("MC-2026/1");
        manifest.setManifestDate(LocalDateTime.now());
        manifest.setStatus("IN_TRANSIT");
        manifest.setVehiclePlate("AFM-345-MC");
        manifest.setVehicleModel("Isuzu NPR 400");
        manifest.setDriverName("Armando Cossa");
        manifest.setDriverPhone("845566778");
        manifest.setRouteName("Rota Boane - Bela Vista");
        manifest.setDepartureTime("07:00");
        manifest.setTotalStops(2);
        manifest.setTotalBoxes(new BigDecimal("15.50"));
        manifest.setTotalPackages(new BigDecimal("120.00"));
        manifest.setTotalAmount(new BigDecimal("85000.00"));
        manifest.setTotalAmountToCollect(new BigDecimal("25000.00"));

        FleetManifestStop s1 = new FleetManifestStop();
        s1.setSequence(1);
        s1.setDocumentType("GUIA_REMESSA");
        s1.setDocumentNumber("GR-2026/12");
        s1.setClientName("Mercearia do Povo");
        s1.setDeliveryAddress("Vila de Boane, Bairro 2");
        s1.setContactPhone("823344556");
        s1.setBoxCount(new BigDecimal("10.00"));
        s1.setPackageCount(new BigDecimal("80.00"));
        s1.setTotalAmount(new BigDecimal("60000.00"));
        s1.setAmountToCollect(BigDecimal.ZERO);
        s1.setPaymentTerms("Faturado / Conta Corrente");
        manifest.addStop(s1);

        FleetManifestStop s2 = new FleetManifestStop();
        s2.setSequence(2);
        s2.setDocumentType("GUIA_REMESSA");
        s2.setDocumentNumber("GR-2026/13");
        s2.setClientName("Cantina Central");
        s2.setDeliveryAddress("Bela Vista Mercado");
        s2.setContactPhone("849988112");
        s2.setBoxCount(new BigDecimal("5.50"));
        s2.setPackageCount(new BigDecimal("40.00"));
        s2.setTotalAmount(new BigDecimal("25000.00"));
        s2.setAmountToCollect(new BigDecimal("25000.00"));
        s2.setPaymentTerms("Pronto Pagamento");
        manifest.addStop(s2);

        when(manifestRepository.findByIdAndCompanyIdWithStops(50L, 1L)).thenReturn(Optional.of(manifest));

        byte[] pdfBytes = service.renderA4(50L);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        PdfReader reader = new PdfReader(pdfBytes);
        assertTrue(reader.getNumberOfPages() >= 1);
        reader.close();
    }
}
