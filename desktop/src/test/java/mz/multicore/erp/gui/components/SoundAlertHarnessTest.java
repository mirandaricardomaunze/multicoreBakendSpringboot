package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoundAlertHarnessTest {

    @Test
    @DisplayName("Harness: SoundAlertManager é Singleton e gera bytes PCM válidos")
    void testSoundAlertManagerSingletonAndPcmBytes() {
        SoundAlertManager manager = SoundAlertManager.getInstance();
        assertThat(manager).isNotNull();

        byte[] pcmBytes = manager.generateAlertToneBytes();
        assertThat(pcmBytes).isNotNull();
        assertThat(pcmBytes.length).isGreaterThan(100);

        // Verifica que não são apenas zeros e estão dentro dos limites de 8-bit assinado
        boolean hasNonZero = false;
        for (byte b : pcmBytes) {
            if (b != 0) {
                hasNonZero = true;
                break;
            }
        }
        assertThat(hasNonZero).isTrue();
    }

    @org.junit.jupiter.api.BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("multicore.test.headless", "true");
    }

    @Test
    @DisplayName("Harness: SoundAlertManager em modo headless não lança excepções")
    void testSoundAlertHeadlessSafe() {
        SoundAlertManager manager = SoundAlertManager.getInstance();
        assertThat(manager.isHeadless()).isTrue();

        // Executar chamadas em headless não deve lançar nenhuma excepção
        manager.playAlertAsync();
        manager.playTestAlertAsync();
    }

    @Test
    @DisplayName("Harness: Preferências de som e volume respeitam limites")
    void testSoundPreferencesAndVolumeBounds() {
        SoundAlertManager manager = SoundAlertManager.getInstance();

        manager.setSoundAlertEnabled(true);
        assertThat(manager.isSoundAlertEnabled()).isTrue();

        manager.setSoundAlertEnabled(false);
        assertThat(manager.isSoundAlertEnabled()).isFalse();

        manager.setVolume(1.5f);
        assertThat(manager.getVolume()).isLessThanOrEqualTo(1.0f);

        manager.setVolume(-0.5f);
        assertThat(manager.getVolume()).isGreaterThanOrEqualTo(0.0f);

        // Restaurar estado padrão
        manager.setVolume(0.85f);
        manager.setSoundAlertEnabled(true);
    }
}
