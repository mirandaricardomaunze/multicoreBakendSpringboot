// Gera o ícone Multicore (traço branco + arco laranja sobre azul-marinho) em todos os tamanhos + app-icon.ico.
// Desenho plano e geométrico: sem gradientes, brilhos ou sombras; cortes horizontais e cores sólidas.
// Uso: javac -d /tmp/ig installer/IconGen.java && java -cp /tmp/ig IconGen <pasta-saida>
// Depois copiar icon-*.png, app-icon.png e app-icon.ico para desktop/src/main/resources/icons/
// e app-icon.ico para installer/. folha.png mostra os tamanhos em fundo claro e escuro.
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

/** Gera o ícone Multicore: traço em ziguezague branco e arco laranja num quadrado arredondado azul-marinho. */
public class IconGen {
    static final Color NAVY = new Color(15, 23, 42), ORANGE = new Color(249, 115, 22);

    static BufferedImage render(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        boolean small = size <= 24;
        double pad = small ? 0 : size * 0.06, box = size - 2 * pad, r = box * (small ? 0.18 : 0.22);
        g.setColor(NAVY);
        g.fill(new RoundRectangle2D.Double(pad, pad, box, box, r * 2, r * 2));
        g.translate(pad, pad); g.scale(box, box);

        // Arco laranja: crescente que nasce fino à esquerda e engrossa até à ponta superior direita.
        Path2D arc = new Path2D.Double();
        arc.moveTo(0.12, 0.84);
        arc.quadTo(0.68, 0.92, 0.90, 0.40);
        arc.quadTo(0.74, small ? 0.66 : 0.72, 0.12, 0.84);
        arc.closePath();
        g.setColor(ORANGE); g.fill(arc);

        // Traço branco: ziguezague de espessura constante, pontas cortadas na horizontal.
        double sw = small ? 0.165 : 0.125, top = 0.21, bottom = 0.66;
        Path2D zig = new Path2D.Double();
        zig.moveTo(0.14, 0.80); zig.lineTo(0.36, 0.31); zig.lineTo(0.55, 0.555); zig.lineTo(0.84, 0.08);
        Area white = new Area(new BasicStroke((float) sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f).createStrokedShape(zig));
        white.intersect(new Area(new Rectangle2D.Double(0, top, 1, bottom - top)));
        // Intervalo uniforme entre o traço e o arco: contorno do próprio traço, na cor do fundo.
        Area gap = new Area(new BasicStroke((float) (small ? 0.07 : 0.075), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10f).createStrokedShape(white));
        gap.add(white);
        g.setColor(NAVY); g.fill(gap);
        g.setColor(Color.WHITE); g.fill(white);
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
