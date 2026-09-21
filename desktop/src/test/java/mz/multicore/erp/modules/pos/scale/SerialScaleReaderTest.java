package mz.multicore.erp.modules.pos.scale;

import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleReading;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SerialScaleReaderTest {

    private SerialScaleReader reader;

    @BeforeEach
    void setUp() {
        reader = new SerialScaleReader(true);
    }

    @Test
    void parseRawFrameExtractsWeightCorrectly() {
        ScaleReading reading = reader.parseRawFrame("\u000200001.450kg\r");
        assertThat(reading.weightKg()).isEqualByComparingTo(new BigDecimal("1.450"));
        assertThat(reading.status()).isEqualTo(ScaleStatus.STABLE);
    }

    @Test
    void parseRawFrameHandlesUnstableStatus() {
        ScaleReading reading = reader.parseRawFrame("\u0002?00002.500kg\r");
        assertThat(reading.weightKg()).isEqualByComparingTo(new BigDecimal("2.500"));
        assertThat(reading.status()).isEqualTo(ScaleStatus.UNSTABLE);
    }

    @Test
    void tareCalculatesNetWeight() {
        reader.setSimulatedWeight(new BigDecimal("2.000"), true);
        reader.applyTare();
        assertThat(reader.getTareWeightKg()).isEqualByComparingTo(new BigDecimal("2.000"));

        reader.setSimulatedWeight(new BigDecimal("3.500"), true);
        ScaleReading net = reader.getCurrentReading();
        assertThat(net.weightKg()).isEqualByComparingTo(new BigDecimal("1.500"));

        reader.clearTare();
        assertThat(reader.getCurrentReading().weightKg()).isEqualByComparingTo(new BigDecimal("3.500"));
    }

    @Test
    void listenerReceivesUpdates() {
        AtomicInteger eventCount = new AtomicInteger(0);
        reader.addListener(r -> eventCount.incrementAndGet());

        reader.setSimulatedWeight(new BigDecimal("0.750"), true);
        assertThat(eventCount.get()).isEqualTo(1);
    }
}
