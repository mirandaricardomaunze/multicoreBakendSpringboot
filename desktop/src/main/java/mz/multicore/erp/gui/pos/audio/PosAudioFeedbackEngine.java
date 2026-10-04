package mz.multicore.erp.gui.pos.audio;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Motor Singleton de feedback sonoro sintetizado em tempo real para o POS.
 * Gera tons sintetizados em memória PCM sem depender de ficheiros externos.
 */
public class PosAudioFeedbackEngine {

    private static final Logger LOGGER = Logger.getLogger(PosAudioFeedbackEngine.class.getName());
    private static final PosAudioFeedbackEngine INSTANCE = new PosAudioFeedbackEngine();

    public enum SoundEvent {
        SUCCESS(800, 65),
        WARNING(600, 120),
        ERROR(350, 160),
        SCALE_STABLE(1000, 40);

        private final int frequencyHz;
        private final int durationMs;

        SoundEvent(int frequencyHz, int durationMs) {
            this.frequencyHz = frequencyHz;
            this.durationMs = durationMs;
        }

        public int getFrequencyHz() { return frequencyHz; }
        public int getDurationMs() { return durationMs; }
    }

    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "POS-AudioFeedback-Thread");
        t.setDaemon(true);
        return t;
    });

    private final File settingsFile;
    private boolean enabled = true;
    private float volume = 0.8f;
    private boolean audioHardwareAvailable = true;

    private PosAudioFeedbackEngine() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".multicore");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.settingsFile = new File(dir, "audio_settings.json");
        loadSettings();
    }

    public static PosAudioFeedbackEngine getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveSettings();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        saveSettings();
    }

    public void playAsync(SoundEvent event) {
        if (!enabled || event == null) return;
        if (GraphicsEnvironment.isHeadless() || "true".equalsIgnoreCase(System.getProperty("java.awt.headless"))
                || "true".equalsIgnoreCase(System.getProperty("multicore.test.headless"))) {
            return;
        }
        if (!audioHardwareAvailable) return;

        asyncExecutor.submit(() -> playToneInternal(event.getFrequencyHz(), event.getDurationMs()));
    }

    public byte[] generateToneBytes(int frequencyHz, int durationMs) {
        float sampleRate = 8000f;
        int numSamples = (int) ((durationMs / 1000f) * sampleRate);
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i / (sampleRate / frequencyHz);
            buffer[i] = (byte) (Math.sin(angle) * 127.0 * volume);
        }
        return buffer;
    }

    private void playToneInternal(int frequencyHz, int durationMs) {
        try {
            float sampleRate = 8000f;
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            byte[] pcmData = generateToneBytes(frequencyHz, durationMs);

            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, pcmData.length);
            line.start();
            line.write(pcmData, 0, pcmData.length);
            line.drain();
            line.close();
        } catch (Throwable ex) {
            audioHardwareAvailable = false;
            LOGGER.log(Level.FINE, "Dispositivo de áudio indisponível ou desativado: " + ex.getMessage());
        }
    }

    private synchronized void loadSettings() {
        if (!settingsFile.exists()) return;
        try (FileReader reader = new FileReader(settingsFile, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[512];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            String content = sb.toString();
            if (content.contains("\"enabled\": false")) {
                this.enabled = false;
            } else if (content.contains("\"enabled\": true")) {
                this.enabled = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Não foi possível carregar definições de áudio: " + ex.getMessage());
        }
    }

    private synchronized void saveSettings() {
        try (FileWriter writer = new FileWriter(settingsFile, StandardCharsets.UTF_8)) {
            String json = "{\n  \"enabled\": " + enabled + ",\n  \"volume\": " + volume + "\n}";
            writer.write(json);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Não foi possível guardar definições de áudio: " + ex.getMessage());
        }
    }
}
