package mz.multicore.erp.modules.pos.scale;

import com.fazecast.jSerialComm.SerialPort;
import mz.multicore.erp.desktop.config.DesktopLocalSettings.ScaleSettings;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The serial scale of this workstation, shared by the POS and the butchery bench. Without a configured port it
 * stays in simulation (the previous behaviour). With a port, a daemon thread opens it, asks for the weight when
 * the scale works on request ("W" or ENQ) and turns every frame into a reading. If the link stops answering for
 * three seconds the reading becomes "disconnected", and the thread keeps trying to reopen the port.
 */
public final class ScaleConnection {

    /** One serial port found on this computer. */
    public record PortInfo(String name, String description) {
        public String label() {
            return description == null || description.isBlank() || description.equals(name) ? name : name + "  ·  " + description;
        }
    }

    private static final ScaleConnection SHARED = new ScaleConnection();
    private static final long SILENCE_MS = 3000;

    private final SerialScaleReader reader = new SerialScaleReader(true);
    private volatile boolean running;
    private Thread worker;

    private ScaleConnection() {
    }

    public static ScaleConnection shared() {
        return SHARED;
    }

    public SerialScaleReader reader() {
        return reader;
    }

    private static volatile boolean configured;

    /** The shared reader, connected once with the settings saved on this workstation. */
    public static SerialScaleReader connectedReader() {
        if (!configured) {
            synchronized (ScaleConnection.class) {
                if (!configured) {
                    configured = true;
                    try {
                        SHARED.apply(mz.multicore.erp.desktop.config.DesktopLocalSettings.scaleSettings());
                    } catch (RuntimeException unreadable) {
                        SHARED.reader.setSimulationMode(true);
                    }
                }
            }
        }
        return SHARED.reader;
    }

    /** Applies new settings: stops the current link and, when enabled with a port, starts the real one. */
    public synchronized void apply(ScaleSettings settings) {
        stop();
        if (settings == null || !settings.enabled() || !settings.hasPort()) {
            reader.setSimulationMode(true);
            return;
        }
        reader.setSimulationMode(false);
        reader.markDisconnected();
        running = true;
        worker = new Thread(() -> loop(settings), "multicore-scale-" + settings.port());
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        running = false;
        if (worker != null) worker.interrupt();
        worker = null;
    }

    public boolean isLive() {
        return running;
    }

    public static List<PortInfo> availablePorts() {
        List<PortInfo> ports = new ArrayList<>();
        try {
            for (SerialPort port : SerialPort.getCommPorts()) {
                ports.add(new PortInfo(port.getSystemPortName(), port.getPortDescription()));
            }
        } catch (Throwable unavailable) {
            // Native library not loadable on this machine: no ports to offer, simulation stays available.
        }
        return ports;
    }

    private void loop(ScaleSettings settings) {
        while (running) {
            SerialPort port = null;
            try {
                port = SerialPort.getCommPort(settings.port());
                port.setComPortParameters(settings.baud(), 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 300, 300);
                if (!port.openPort()) {
                    reader.markDisconnected();
                    pause(2000);
                    continue;
                }
                read(port, settings);
            } catch (Throwable error) {
                reader.markDisconnected();
                pause(2000);
            } finally {
                if (port != null && port.isOpen()) port.closePort();
            }
        }
    }

    private void read(SerialPort port, ScaleSettings settings) {
        ScaleFrameSplitter splitter = new ScaleFrameSplitter();
        byte[] buffer = new byte[256];
        byte[] request = requestFor(settings.mode());
        long lastFrame = System.currentTimeMillis();
        boolean silent = false;
        while (running && port.isOpen()) {
            if (request != null) port.writeBytes(request, request.length);
            int read = port.readBytes(buffer, buffer.length);
            if (read < 0) return;
            for (String frame : splitter.feed(buffer, read)) {
                reader.parseRawFrame(frame);
                lastFrame = System.currentTimeMillis();
                silent = false;
            }
            if (!silent && System.currentTimeMillis() - lastFrame > SILENCE_MS) {
                reader.markDisconnected();
                silent = true;
            }
            if (request != null) pause(250);
        }
    }

    static byte[] requestFor(String mode) {
        if ("POLL_W".equals(mode)) return "W\r".getBytes(StandardCharsets.US_ASCII);
        if ("POLL_ENQ".equals(mode)) return new byte[]{0x05};
        return null;
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
