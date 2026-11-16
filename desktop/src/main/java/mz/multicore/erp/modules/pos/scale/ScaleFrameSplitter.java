package mz.multicore.erp.modules.pos.scale;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits the byte stream of a serial scale into frames. Scales end a frame with CR, LF or ETX (0x03) and may start
 * it with STX (0x02); both are removed. Bytes of an incomplete frame wait for the next read. A runaway buffer
 * (no terminator at all) is dropped, so a misconfigured port never grows memory.
 */
public final class ScaleFrameSplitter {

    private static final int MAX_FRAME = 64;
    private final StringBuilder pending = new StringBuilder();

    public List<String> feed(byte[] data, int length) {
        List<String> frames = new ArrayList<>();
        String text = new String(data, 0, Math.max(0, length), StandardCharsets.US_ASCII);
        for (char ch : text.toCharArray()) {
            if (ch == '\r' || ch == '\n' || ch == 0x03) {
                String frame = pending.toString().trim();
                pending.setLength(0);
                if (!frame.isEmpty()) frames.add(frame);
            } else if (ch != 0x02) {
                pending.append(ch);
                if (pending.length() > MAX_FRAME) pending.setLength(0);
            }
        }
        return frames;
    }
}
