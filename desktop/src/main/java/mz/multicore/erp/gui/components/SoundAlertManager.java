package mz.multicore.erp.gui.components;

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
 * Gestor Singleton de alarmes sonoros em tempo real para o Multicore ERP.
 * Dispara tons sintetizados de alta atenção quando falhas operacionais críticas são detectadas.
 * Não requer ficheiros .wav externos e possui salvaguarda estrita para ambientes de teste headless.
 */
public class SoundAlertManager {

    private static final Logger LOGGER = Logger.getLogger(SoundAlertManager.class.getName());
    private static final SoundAlertManager INSTANCE = new SoundAlertManager();

    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Multicore-SoundAlert-Thread");
        t.setDaemon(true);
        return t;
    });

    private final File settingsFile;
    private boolean soundAlertEnabled = true;
    private float volume = 0.85f;
    private boolean audioHardwareAvailable = true;

    private SoundAlertManager() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".multicore");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.settingsFile = new File(dir, "alert_sound_settings.json");
        loadSettings();
    }

    public static SoundAlertManager getInstance() {
        return INSTANCE;
    }

    public boolean isSoundAlertEnabled() {
        return soundAlertEnabled;
    }

    public void setSoundAlertEnabled(boolean soundAlertEnabled) {
        this.soundAlertEnabled = soundAlertEnabled;
        saveSettings();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        saveSettings();
    }

    public boolean isHeadless() {
        return GraphicsEnvironment.isHeadless()
                || "true".equalsIgnoreCase(System.getProperty("java.awt.headless"))
                || "true".equalsIgnoreCase(System.getProperty("multicore.test.headless"));
    }

    /**
     * Dispara o alarme sonoro crítico de forma assíncrona.
     */
    public void playAlertAsync() {
        if (!soundAlertEnabled || isHeadless() || !audioHardwareAvailable) {
            return;
        }
        asyncExecutor.submit(this::playAlertInternal);
    }

    /**
     * Dispara o alarme sonoro em modo de teste manual.
     */
    public void playTestAlertAsync() {
        if (isHeadless() || !audioHardwareAvailable) {
            return;
        }
        asyncExecutor.submit(this::playAlertInternal);
    }

    /**
     * Gera os bytes PCM de onda senoidal de pulso duplo de alerta (900 Hz + pausa + 1200 Hz).
     */
    public byte[] generateAlertToneBytes() {
        float sampleRate = 8000f;
        int p1Samples = (int) (0.090f * sampleRate);
        int gapSamples = (int) (0.040f * sampleRate);
        int p2Samples = (int) (0.140f * sampleRate);
        int totalSamples = p1Samples + gapSamples + p2Samples;

        byte[] buffer = new byte[totalSamples];

        // Pulso 1: 900 Hz
        for (int i = 0; i < p1Samples; i++) {
            double angle = 2.0 * Math.PI * i / (sampleRate / 900.0);
            buffer[i] = (byte) (Math.sin(angle) * 127.0 * volume);
        }
        // Intervalo de silêncio (gapSamples) permanece em 0
        // Pulso 2: 1200 Hz
        int offset = p1Samples + gapSamples;
        for (int i = 0; i < p2Samples; i++) {
            double angle = 2.0 * Math.PI * i / (sampleRate / 1200.0);
            buffer[offset + i] = (byte) (Math.sin(angle) * 127.0 * volume);
        }

        return buffer;
    }

    private void playAlertInternal() {
        try {
            float sampleRate = 8000f;
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            byte[] pcmData = generateAlertToneBytes();

            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, pcmData.length);
            line.start();
            line.write(pcmData, 0, pcmData.length);
            line.drain();
            line.close();
        } catch (Throwable ex) {
            audioHardwareAvailable = false;
            LOGGER.log(Level.FINE, "Dispositivo de áudio indisponível para alarme: " + ex.getMessage());
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
                this.soundAlertEnabled = false;
            } else if (content.contains("\"enabled\": true")) {
                this.soundAlertEnabled = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Não foi possível carregar definições de alarme sonoro: " + ex.getMessage());
        }
    }

    private synchronized void saveSettings() {
        try (FileWriter writer = new FileWriter(settingsFile, StandardCharsets.UTF_8)) {
            String json = "{\n  \"enabled\": " + soundAlertEnabled + ",\n  \"volume\": " + volume + "\n}";
            writer.write(json);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Não foi possível guardar definições de alarme sonoro: " + ex.getMessage());
        }
    }
}
