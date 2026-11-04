// Gera o ícone Multicore (M branco sobre azul->violeta) em todos os tamanhos + app-icon.ico.
// Uso: javac -d /tmp/ig installer/IconGen.java && java -cp /tmp/ig IconGen <pasta-saida>
// Depois copiar icon-*.png, app-icon.png e app-icon.ico para desktop/src/main/resources/icons/
// e app-icon.ico para installer/. folha.png mostra os tamanhos em fundo claro e escuro.
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

/** Gera o ícone Multicore: M geométrico branco sobre quadrado arredondado azul→violeta. */
public class IconGen {
    static final Color BLUE = new Color(37, 99, 235), VIOLET = new Color(124, 58, 237);

    static BufferedImage render(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        boolean small = size <= 24;
        double pad = small ? 0 : size * 0.06, box = size - 2 * pad, r = box * (small ? 0.18 : 0.22);
        Shape bg = new RoundRectangle2D.Double(pad, pad, box, box, r * 2, r * 2);
        g.setPaint(new GradientPaint((float) pad, (float) pad, BLUE, (float) (pad + box), (float) (pad + box), VIOLET));
        g.fill(bg);
        // M: duas hastes e um V central, traço único, pontas rectas.
        double sw = box * (small ? 0.165 : 0.125);
        double left = pad + box * (small ? 0.19 : 0.215) + sw / 2, right = pad + box - (left - pad);
        double top = pad + box * (small ? 0.22 : 0.25) + sw / 2, bottom = pad + box * (small ? 0.81 : 0.765);
        double mid = top + (bottom - top) * (small ? 0.80 : 0.74);
        Path2D m = new Path2D.Double();
        m.moveTo(left, bottom); m.lineTo(left, top); m.lineTo(pad + box / 2, mid); m.lineTo(right, top); m.lineTo(right, bottom);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float) sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(m);
        g.dispose();
        return img;
    }

    public static void main(String[] a) throws Exception {
        Path out = Path.of(a[0]); Files.createDirectories(out);
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        ByteArrayOutputStream[] pngs = new ByteArrayOutputStream[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            BufferedImage img = render(sizes[i]);
            ImageIO.write(img, "png", out.resolve("icon-" + sizes[i] + ".png").toFile());
            pngs[i] = new ByteArrayOutputStream(); ImageIO.write(img, "png", pngs[i]);
        }
        ImageIO.write(render(512), "png", out.resolve("app-icon.png").toFile());
        try (DataOutputStream ico = new DataOutputStream(new FileOutputStream(out.resolve("app-icon.ico").toFile()))) {
            ico.write(le16(0)); ico.write(le16(1)); ico.write(le16(sizes.length));
            int offset = 6 + 16 * sizes.length;
            for (int i = 0; i < sizes.length; i++) {
                int s = sizes[i] >= 256 ? 0 : sizes[i];
                ico.write(s); ico.write(s); ico.write(0); ico.write(0);
                ico.write(le16(1)); ico.write(le16(32)); ico.write(le32(pngs[i].size())); ico.write(le32(offset));
                offset += pngs[i].size();
            }
            for (ByteArrayOutputStream p : pngs) p.writeTo(ico);
        }
        // Folha de comparação: fundo claro e escuro, tamanhos reais e ampliados.
        BufferedImage sheet = new BufferedImage(900, 420, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(243, 244, 246)); g.fillRect(0, 0, 900, 210);
        g.setColor(new Color(31, 41, 55)); g.fillRect(0, 210, 900, 210);
        for (int band = 0; band < 2; band++) {
            int x = 20, y = band * 210 + 20;
            g.drawImage(render(256).getScaledInstance(170, 170, Image.SCALE_SMOOTH), x, y, null); x += 190;
            for (int s : new int[]{64, 48, 32, 24, 16}) { g.drawImage(render(s), x, y + 170 - s, null); x += s + 24; }
            for (int s : new int[]{32, 16}) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(render(s), x, y + 10, s == 16 ? 160 : 160, 160, null); x += 180;
            }
        }
        ImageIO.write(sheet, "png", out.resolve("folha.png").toFile());
    }
    static byte[] le16(int v) { return new byte[]{(byte) v, (byte) (v >> 8)}; }
    static byte[] le32(int v) { return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)}; }
}
