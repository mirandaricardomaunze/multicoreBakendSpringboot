package mz.multicore.erp.gui;

import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.dto.FleetManifestStopDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FleetManifestUiHarnessTest {

    @Test
    @DisplayName("Garante integridade dos DTOs de Manifesto de Carga e Paragens na UI")
    void validateFleetManifestDtoIntegrity() {
        FleetManifestStopDTO stop = new FleetManifestStopDTO(
                1,
                "GUIA_REMESSA",
                101L,
                "GR-2026/1",
                50L,
                "Cliente Teste Lda",
                "Matola Gare",
                "841122334",
                new BigDecimal("4.50"),
                new BigDecimal("20.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("50000.00"),
                "Pronto Pagamento"
        );

        assertNotNull(stop);
        assertEquals(1, stop.sequence());
        assertEquals("GR-2026/1", stop.documentNumber());
        assertEquals(new BigDecimal("4.50"), stop.boxCount());
        assertEquals(new BigDecimal("50000.00"), stop.amountToCollect());

        FleetManifestDTO manifest = new FleetManifestDTO(
                1L,
                "MC-2026/1",
                10L,
                LocalDateTime.now(),
                "IN_TRANSIT",
                20L,
                "ABC-123-MC",
                "Mitsubishi Canter",
                "Joaquim Macuácua",
                "829900112",
                "Rota Matola / Boane",
                "08:30",
                "Saída com escolta",
                1,
                new BigDecimal("4.50"),
                new BigDecimal("20.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("50000.00"),
                List.of(stop)
        );

        assertEquals("MC-2026/1", manifest.manifestNumber());
        assertEquals("ABC-123-MC", manifest.vehiclePlate());
        assertEquals(1, manifest.totalStops());
        assertEquals(1, manifest.stops().size());
    }

    @Test
    @DisplayName("Garante request de criação de manifesto com listas não nulas")
    void validateCreateRequestDefaults() {
        CreateFleetManifestRequest request = new CreateFleetManifestRequest(
                1L,
                "Motorista A",
                "841234567",
                "Rota Centro",
                "09:00",
                "Urgente",
                null
        );

        assertNotNull(request.deliveryGuideIds());
        assertTrue(request.deliveryGuideIds().isEmpty());
    }
}
