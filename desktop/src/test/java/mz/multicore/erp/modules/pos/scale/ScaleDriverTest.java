package mz.multicore.erp.modules.pos.scale;

import mz.multicore.erp.desktop.config.DesktopLocalSettings;
import mz.multicore.erp.desktop.config.DesktopLocalSettings.ScaleSettings;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Real serial scale: frames as scales send them, overload, request commands and per-workstation settings. */
class ScaleDriverTest {

    @Test
    void framesAreSplitOnCrLfAndEtxAndWaitForTheirEnd() {
        ScaleFrameSplitter splitter = new ScaleFrameSplitter();
        byte[] first = "\u000200001.4".getBytes(StandardCharsets.US_ASCII);
        assertThat(splitter.feed(first, first.length)).isEmpty();
        byte[] rest = "50kg\u0003ST,GS,+002.100kg\r\n".getBytes(StandardCharsets.US_ASCII);
        assertThat(splitter.feed(rest, rest.length)).containsExactly("00001.450kg", "ST,GS,+002.100kg");
    }

    @Test
    void readerUnderstandsCommonFramesAndOverload() {
        SerialScaleReader reader = new SerialScaleReader(false);
        assertThat(reader.parseRawFrame("ST,GS,+002.100kg").weightKg()).isEqualByComparingTo("2.100");
        assertThat(reader.parseRawFrame("US,GS,+002.180kg").status()).isEqualTo(ScaleStatus.UNSTABLE);
        assertThat(reader.parseRawFrame("00001.450kg").status()).isEqualTo(ScaleStatus.STABLE);
        assertThat(reader.parseRawFrame("ST,OL,------kg").status()).isEqualTo(ScaleStatus.OVERLOAD);
        reader.markDisconnected();
        assertThat(reader.getCurrentReading().status()).isEqualTo(ScaleStatus.DISCONNECTED);
    }

    @Test
    void pollModesSendTheCommandOfTheManual() {
        assertThat(ScaleConnection.requestFor("POLL_W")).containsExactly('W', '\r');
        assertThat(ScaleConnection.requestFor("POLL_ENQ")).containsExactly(0x05);
        assertThat(ScaleConnection.requestFor("CONTINUOUS")).isNull();
    }

    @Test
    void settingsAreSavedPerWorkstationKeepingOtherKeys(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("desktop.properties");
        Files.writeString(file, "desktop.api.base-url=https://erp.exemplo.co.mz\n");
        invoke("writeScaleSettings", file, new ScaleSettings(true, "COM4", 4800, "POLL_W"));
        ScaleSettings read = (ScaleSettings) invoke("readScaleSettings", file);
        assertThat(read).isEqualTo(new ScaleSettings(true, "COM4", 4800, "POLL_W"));
        assertThat(Files.readString(file)).contains("erp.exemplo.co.mz");
        assertThat(((ScaleSettings) invoke("readScaleSettings", dir.resolve("missing.properties"))).hasPort()).isFalse();
    }

    @Test
    void withoutPortTheConnectionStaysInSimulation() {
        ScaleConnection.shared().apply(new ScaleSettings(true, "", 9600, "CONTINUOUS"));
        assertThat(ScaleConnection.shared().reader().isSimulationMode()).isTrue();
        assertThat(ScaleConnection.shared().isLive()).isFalse();
        assertThat(ScaleConnection.availablePorts()).isInstanceOf(List.class);
    }

    private static Object invoke(String name, Object... args) throws Exception {
        for (Method m : DesktopLocalSettings.class.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                return m.invoke(null, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    @Test
    void lastFrameIsKeptAndShownWithControlCharacters() {
        SerialScaleReader reader = new SerialScaleReader(false);
        long before = reader.getFrameCount();
        reader.parseRawFrame((char) 0x02 + "00001.450kg" + (char) 0x0D);
        assertThat(reader.getFrameCount()).isEqualTo(before + 1);
        assertThat(SerialScaleReader.visible(reader.getLastRawFrame())).isEqualTo("<STX>00001.450kg<CR>");
        assertThat(SerialScaleReader.visible(null)).isEqualTo("—");
    }
}
