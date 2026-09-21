package mz.multicore.erp.modules.pos.scale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Driver de comunicação e gestão de tramas de peso para balanças USB / Serial COM no POS.
 * Suporta leitura contínua, tramas ASCII standard (NCI / Toledo / STX-ETX), tara e simulação.
 */
public class SerialScaleReader {

    public enum ScaleStatus {
        STABLE,
        UNSTABLE,
        OVERLOAD,
        DISCONNECTED
    }

    public record ScaleReading(
            BigDecimal weightKg,
            ScaleStatus status,
            String unit,
            long timestampMs
    ) {
        public static ScaleReading zero() {
            return new ScaleReading(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), ScaleStatus.STABLE, "kg", System.currentTimeMillis());
        }

        public static ScaleReading disconnected() {
            return new ScaleReading(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), ScaleStatus.DISCONNECTED, "kg", System.currentTimeMillis());
        }
    }

    public interface ScaleWeightListener {
        void onWeightUpdated(ScaleReading reading);
    }

    private final AtomicReference<ScaleReading> currentReading = new AtomicReference<>(ScaleReading.zero());
    private final List<ScaleWeightListener> listeners = new CopyOnWriteArrayList<>();
    private BigDecimal tareWeightKg = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    private boolean simulationMode = true;

    public SerialScaleReader() {
        this(true);
    }

    public SerialScaleReader(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    public ScaleReading getCurrentReading() {
        ScaleReading raw = currentReading.get();
        if (tareWeightKg.signum() > 0 && raw.status() == ScaleStatus.STABLE) {
            BigDecimal netWeight = raw.weightKg().subtract(tareWeightKg).max(BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
            return new ScaleReading(netWeight, raw.status(), raw.unit(), raw.timestampMs());
        }
        return raw;
    }

    public void addListener(ScaleWeightListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ScaleWeightListener listener) {
        listeners.remove(listener);
    }

    /**
     * Efetua o parse de uma trama bruta lida da porta Serial COM (formato NCI / Toledo ou simples).
     * Exemplo de trama NCI: "\u000200001.450kg\r" ou "1.450"
     */
    public ScaleReading parseRawFrame(String rawFrame) {
        if (rawFrame == null || rawFrame.isBlank()) {
            return currentReading.get();
        }

        try {
            String cleaned = rawFrame.replaceAll("[^0-9.\\-+]", "").trim();
            if (cleaned.isEmpty()) {
                return currentReading.get();
            }

            BigDecimal parsedWeight = new BigDecimal(cleaned).setScale(3, RoundingMode.HALF_UP);
            ScaleStatus status = rawFrame.contains("U") || rawFrame.contains("?") ? ScaleStatus.UNSTABLE : ScaleStatus.STABLE;
            ScaleReading reading = new ScaleReading(parsedWeight, status, "kg", System.currentTimeMillis());
            
            updateReading(reading);
            return reading;
        } catch (Exception e) {
            return currentReading.get();
        }
    }

    /** Define manualmente um peso simulado (útil para desenvolvimento, testes e checkout sem hardware físico). */
    public void setSimulatedWeight(BigDecimal weightKg, boolean stable) {
        BigDecimal weight = weightKg != null ? weightKg.setScale(3, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        ScaleStatus status = stable ? ScaleStatus.STABLE : ScaleStatus.UNSTABLE;
        updateReading(new ScaleReading(weight, status, "kg", System.currentTimeMillis()));
    }

    /** Aplica a tara do peso actual presente na balança. */
    public void applyTare() {
        ScaleReading current = currentReading.get();
        if (current.status() == ScaleStatus.STABLE) {
            this.tareWeightKg = current.weightKg();
            notifyListeners(getCurrentReading());
        }
    }

    /** Repõe a tara a zero. */
    public void clearTare() {
        this.tareWeightKg = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        notifyListeners(getCurrentReading());
    }

    public BigDecimal getTareWeightKg() {
        return tareWeightKg;
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    private void updateReading(ScaleReading reading) {
        currentReading.set(reading);
        notifyListeners(getCurrentReading());
    }

    private void notifyListeners(ScaleReading reading) {
        for (ScaleWeightListener listener : listeners) {
            try {
                listener.onWeightUpdated(reading);
            } catch (Exception ignored) {}
        }
    }
}
