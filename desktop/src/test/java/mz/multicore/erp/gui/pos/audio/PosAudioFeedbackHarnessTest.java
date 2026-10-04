package mz.multicore.erp.gui.pos.audio;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Motor de Feedback Sonoro no POS (PAUD-01 a PAUD-08)")
class PosAudioFeedbackHarnessTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("multicore.test.headless", "true");
    }

    @Test
    @DisplayName("PAUD-01: Inicialização do PosAudioFeedbackEngine por omissão")
    void testEngineInitialization() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        assertNotNull(engine);
        assertTrue(engine.isEnabled());
        assertTrue(engine.getVolume() > 0.0f);
    }

    @Test
    @DisplayName("PAUD-02: Execução segura de evento SUCCESS em modo headless")
    void testPlayAsyncSuccessHeadless() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        assertDoesNotThrow(() -> engine.playAsync(PosAudioFeedbackEngine.SoundEvent.SUCCESS));
    }

    @Test
    @DisplayName("PAUD-03: Execução não-bloqueante de WARNING e ERROR")
    void testPlayAsyncWarningAndErrorHeadless() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        assertDoesNotThrow(() -> {
            engine.playAsync(PosAudioFeedbackEngine.SoundEvent.WARNING);
            engine.playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
            engine.playAsync(PosAudioFeedbackEngine.SoundEvent.SCALE_STABLE);
        });
    }

    @Test
    @DisplayName("PAUD-04: Comutação do estado de áudio (Enabled / Disabled)")
    void testSetEnabledState() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        boolean original = engine.isEnabled();
        try {
            engine.setEnabled(false);
            assertFalse(engine.isEnabled());
            assertDoesNotThrow(() -> engine.playAsync(PosAudioFeedbackEngine.SoundEvent.SUCCESS));

            engine.setEnabled(true);
            assertTrue(engine.isEnabled());
        } finally {
            engine.setEnabled(original);
        }
    }

    @Test
    @DisplayName("PAUD-05: Ajuste e limitação de volume")
    void testVolumeClamping() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        float original = engine.getVolume();
        try {
            engine.setVolume(0.5f);
            assertEquals(0.5f, engine.getVolume(), 0.001f);

            engine.setVolume(1.5f);
            assertEquals(1.0f, engine.getVolume(), 0.001f);

            engine.setVolume(-0.5f);
            assertEquals(0.0f, engine.getVolume(), 0.001f);
        } finally {
            engine.setVolume(original);
        }
    }

    @Test
    @DisplayName("PAUD-06: Síntese de amostras PCM em memória (generateToneBytes)")
    void testGenerateToneBytes() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        byte[] bytes = engine.generateToneBytes(800, 65);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("PAUD-07: Resiliência contra parâmetros nulos")
    void testNullEventResilience() {
        PosAudioFeedbackEngine engine = PosAudioFeedbackEngine.getInstance();
        assertDoesNotThrow(() -> engine.playAsync(null));
    }
}
