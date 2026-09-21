package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.LoyaltyEngine;
import mz.multicore.erp.gui.pos.loyalty.PosLoyaltyController;
import mz.multicore.erp.gui.pos.scale.PosScaleLiveWidget;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleReading;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PosScaleLoyaltyHarnessTest {

    @Test
    void testScaleDriverAndWidgetIntegration() {
        SerialScaleReader reader = new SerialScaleReader(true);
        PosScaleLiveWidget widget = new PosScaleLiveWidget(reader);

        AtomicReference<BigDecimal> captured = new AtomicReference<>();
        widget.setOnWeightCapturedCallback(captured::set);

        reader.setSimulatedWeight(new BigDecimal("1.850"), true);
        assertThat(widget.getCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("1.850"));

        ScaleReading reading = reader.getCurrentReading();
        assertThat(reading.status()).isEqualTo(ScaleStatus.STABLE);
    }

    @Test
    void testLoyaltyPointsCalculationAndRedemption() {
        BigDecimal saleTotal = new BigDecimal("1500.00");
        BigDecimal earnedPoints = LoyaltyEngine.calculateEarnedPoints(saleTotal);
        assertThat(earnedPoints).isEqualByComparingTo(new BigDecimal("15"));

        BigDecimal availablePoints = new BigDecimal("200");
        BigDecimal pointsToRedeem = new BigDecimal("100");
        LoyaltyEngine.RedemptionResult result = LoyaltyEngine.applyRedemption(availablePoints, saleTotal, pointsToRedeem);

        assertThat(result.pointsRedeemed()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.discountMzn()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.finalPayableAmount()).isEqualByComparingTo(new BigDecimal("1400.00"));
        assertThat(result.remainingPoints()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void testLoyaltyControllerClientSelection() {
        PosLoyaltyController controller = new PosLoyaltyController(null);
        assertThat(controller.getSelectedLoyaltyClient()).isEmpty();

        ClientDTO client = new ClientDTO(10L, "Maria Silva", "123456789", "maria@test.com", "Maputo", 30, new BigDecimal("5000"), new BigDecimal("150"), "LOY-1001");

        controller.setSelectedLoyaltyClient(client);
        assertThat(controller.getSelectedLoyaltyClient()).contains(client);

        LoyaltyEngine.RedemptionResult result = controller.calculateRedemption(new BigDecimal("300.00"), new BigDecimal("50"));
        assertThat(result.discountMzn()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(controller.getLoyaltyDiscountMzn()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
}
