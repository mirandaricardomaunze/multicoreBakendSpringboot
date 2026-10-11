package mz.multicore.erp.gui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador canónico da identidade visual do Multicore ERP:
 * Monograma "M" Corporativo 3D com facetas de vidro e gradiente Azul Safira / Ciano.
 * Gera o conjunto multi-resolução completo (16, 24, 32, 48, 64, 128, 256) e compila o app-icon.ico.
 */
public class IconGenerator {

    public static void main(String[] args) throws Exception {
        System.out.println("A gerar o novo icone corporativo Monograma M 3D...");

        BufferedImage base256 = renderMaster256();

        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        List<BufferedImage> images = new ArrayList<>();

        Path resourcesDir = Path.of("desktop", "src", "main", "resources", "icons");
        Files.createDirectories(resourcesDir);

        for (int size : sizes) {
            BufferedImage scaled = scale(base256, size);
            images.add(scaled);
            Path pngPath = resourcesDir.resolve("icon-" + size + ".png");
            ImageIO.write(scaled, "PNG", pngPath.toFile());
            System.out.println("Gerado: " + pngPath);
        }

        // app-icon.png principal (256x256)
        Path appIconPng = resourcesDir.resolve("app-icon.png");
        ImageIO.write(base256, "PNG", appIconPng.toFile());
        System.out.println("Gerado: " + appIconPng);

        // Compilar o arquivo ICO multi-resolução
        byte[] icoBytes = buildIco(images);
        Path appIconIco = resourcesDir.resolve("app-icon.ico");
        Files.write(appIconIco, icoBytes);
        System.out.println("Gerado: " + appIconIco + " (" + icoBytes.length + " bytes)");

        // Copiar para installer/app-icon.ico
        Path installerIco = Path.of("installer", "app-icon.ico");
        Files.createDirectories(installerIco.getParent());
        Files.write(installerIco, icoBytes);
        System.out.println("Copiado para instalador: " + installerIco);

        System.out.println("Icone gerado com sucesso em todas as resolucoes!");
    }

    public static BufferedImage renderMaster256() {
        int w = 256;
        int h = 256;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Anti-aliasing máximo
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // 1. Sombra externa do Squircle
        RoundRectangle2D shadow = new RoundRectangle2D.Float(14, 18, 228, 228, 60, 60);
        g.setColor(new Color(5, 10, 25, 120));
        g.fill(shadow);

        // 2. Fundo Squircle Corporativo Profundo (Navy Safira escuro e rico)
        RoundRectangle2D squircle = new RoundRectangle2D.Float(14, 14, 228, 228, 56, 56);
        GradientPaint bgGradient = new GradientPaint(
                14, 14, new Color(15, 23, 42),       // Slate-900
                242, 242, new Color(10, 15, 30)     // Deep Navy
        );
        g.setPaint(bgGradient);
        g.fill(squircle);

        // Brilho suave sutil de fundo no quadrante superior
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(128, 70), 140f,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(30, 64, 175, 75), new Color(14, 165, 233, 20), new Color(0, 0, 0, 0)}
        );
        g.setPaint(glow);
        g.fill(squircle);

        // Borda suave do Squircle com gradiente sutil
        GradientPaint borderGrad = new GradientPaint(
                14, 14, new Color(56, 189, 248, 90),   // Sky-400 translúcido
                242, 242, new Color(99, 102, 241, 40)  // Indigo translúcido
        );
        g.setPaint(borderGrad);
        g.setStroke(new BasicStroke(1.8f));
        g.draw(squircle);

        // 3. Desenho do Monograma "M" Corporativo 3D
        // Geometria precisa do "M"
        // Coordenadas base:
        // Pilar esquerdo: x: 50 a 82, y: 72 a 188
        // Vértice central inferior: x: 128, y: 154
        // Pilar direito: x: 174 a 206, y: 72 a 188

        // A) Pilar Esquerdo (Vertical)
        Path2D.Double leftPillar = new Path2D.Double();
        leftPillar.moveTo(52, 186);
        leftPillar.lineTo(52, 78);
        leftPillar.curveTo(52, 73, 55, 70, 60, 70); // canto superior arredondado
        leftPillar.lineTo(82, 70);
        leftPillar.lineTo(82, 186);
        leftPillar.curveTo(82, 190, 78, 193, 74, 193);
        leftPillar.lineTo(60, 193);
        leftPillar.curveTo(55, 193, 52, 190, 52, 186);
        leftPillar.closePath();

        GradientPaint leftGrad = new GradientPaint(
                52, 70, new Color(14, 165, 233),   // Ciano radiante
                82, 193, new Color(37, 99, 235)   // Azul Safira
        );
        g.setPaint(leftGrad);
        g.fill(leftPillar);

        // Chanfro 3D / Realce de volume no pilar esquerdo
        Path2D.Double leftBevel = new Path2D.Double();
        leftBevel.moveTo(52, 78);
        leftBevel.curveTo(52, 73, 55, 70, 60, 70);
        leftBevel.lineTo(72, 70);
        leftBevel.lineTo(64, 193);
        leftBevel.lineTo(60, 193);
        leftBevel.curveTo(55, 193, 52, 190, 52, 186);
        leftBevel.closePath();
        g.setPaint(new GradientPaint(52, 70, new Color(255, 255, 255, 140), 64, 193, new Color(255, 255, 255, 10)));
        g.fill(leftBevel);

        // B) Pilar Direito (Vertical)
        Path2D.Double rightPillar = new Path2D.Double();
        rightPillar.moveTo(174, 70);
        rightPillar.lineTo(196, 70);
        rightPillar.curveTo(201, 70, 204, 73, 204, 78);
        rightPillar.lineTo(204, 186);
        rightPillar.curveTo(204, 190, 201, 193, 196, 193);
        rightPillar.lineTo(182, 193);
        rightPillar.curveTo(178, 193, 174, 190, 174, 186);
        rightPillar.closePath();

        GradientPaint rightGrad = new GradientPaint(
                174, 70, new Color(79, 70, 229),   // Indigo
                204, 193, new Color(30, 58, 138)  // Safira Escuro
        );
        g.setPaint(rightGrad);
        g.fill(rightPillar);

        // Chanfro 3D no pilar direito
        Path2D.Double rightBevel = new Path2D.Double();
        rightBevel.moveTo(194, 70);
        rightBevel.lineTo(196, 70);
        rightBevel.curveTo(201, 70, 204, 73, 204, 78);
        rightBevel.lineTo(204, 186);
        rightBevel.curveTo(204, 190, 201, 193, 196, 193);
        rightBevel.lineTo(186, 193);
        rightBevel.lineTo(194, 70);
        rightBevel.closePath();
        g.setPaint(new GradientPaint(186, 70, new Color(165, 180, 252, 90), 204, 193, new Color(255, 255, 255, 20)));
        g.fill(rightBevel);

        // C) Diagonal Esquerda (Descendente: de (74, 70) a (128, 158))
        Path2D.Double leftDiagonal = new Path2D.Double();
        leftDiagonal.moveTo(74, 70);
        leftDiagonal.lineTo(104, 70);
        leftDiagonal.lineTo(138, 150);
        leftDiagonal.lineTo(128, 162);
        leftDiagonal.lineTo(118, 150);
        leftDiagonal.closePath();

        GradientPaint diagLeftGrad = new GradientPaint(
                74, 70, new Color(56, 189, 248),    // Ciano brilhante
                128, 162, new Color(37, 99, 235)   // Azul Safira
        );
        g.setPaint(diagLeftGrad);
        g.fill(leftDiagonal);

        // D) Diagonal Direita (Ascendente: de (128, 162) a (182, 70))
        Path2D.Double rightDiagonal = new Path2D.Double();
        rightDiagonal.moveTo(128, 162);
        rightDiagonal.lineTo(138, 150);
        rightDiagonal.lineTo(182, 70);
        rightDiagonal.lineTo(152, 70);
        rightDiagonal.lineTo(118, 150);
        rightDiagonal.closePath();

        GradientPaint diagRightGrad = new GradientPaint(
                128, 162, new Color(37, 99, 235),  // Azul Safira
                182, 70, new Color(99, 102, 241)   // Indigo radiante
        );
        g.setPaint(diagRightGrad);
        g.fill(rightDiagonal);

        // E) Faceta Superior de Reflexo de Vidro (Aresta de topo das diagonais)
        Path2D.Double topGlassHighlight = new Path2D.Double();
        topGlassHighlight.moveTo(74, 70);
        topGlassHighlight.lineTo(104, 70);
        topGlassHighlight.lineTo(128, 134);
        topGlassHighlight.lineTo(152, 70);
        topGlassHighlight.lineTo(182, 70);
        topGlassHighlight.lineTo(138, 150);
        topGlassHighlight.lineTo(128, 162);
        topGlassHighlight.closePath();
        // Sombra de profundidade e oclusão na junção interior
        GradientPaint innerShadow = new GradientPaint(
                128, 70, new Color(255, 255, 255, 110),
                128, 162, new Color(15, 23, 42, 160)
        );
        g.setPaint(innerShadow);
        g.fill(topGlassHighlight);

        // F) Núcleo Tecnológico Central (Prisma / Quadrante de alta tecnologia no coração do "M")
        // Um diamante/losango tecnológico perfeitamente alinhado abaixo do vértice central
        Path2D.Double coreDiamond = new Path2D.Double();
        float cx = 128f;
        float cy = 186f;
        float rad = 14f;
        coreDiamond.moveTo(cx, cy - rad);
        coreDiamond.lineTo(cx + rad, cy);
        coreDiamond.lineTo(cx, cy + rad);
        coreDiamond.lineTo(cx - rad, cy);
        coreDiamond.closePath();

        // Brilho do Núcleo
        RadialGradientPaint coreGlow = new RadialGradientPaint(
                new Point2D.Float(cx, cy), 32f,
                new float[]{0.0f, 0.45f, 1.0f},
                new Color[]{new Color(56, 189, 248, 220), new Color(37, 99, 235, 100), new Color(0, 0, 0, 0)}
        );
        g.setPaint(coreGlow);
        g.fill(new Ellipse2D.Float(cx - 32, cy - 32, 64, 64));

        GradientPaint diamondGrad = new GradientPaint(
                cx, cy - rad, new Color(224, 242, 254), // Ice blue
                cx, cy + rad, new Color(14, 165, 233)   // Sky cyan
        );
        g.setPaint(diamondGrad);
        g.fill(coreDiamond);

        // Ponto central do núcleo (Core dot)
        g.setColor(new Color(15, 23, 42));
        g.fill(new Ellipse2D.Float(cx - 3f, cy - 3f, 6f, 6f));

        // Trilhas tecnológicas sutis conectando os pilares ao núcleo
        g.setColor(new Color(56, 189, 248, 110));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Line2D.Float(82, 186, cx - rad, cy));
        g.draw(new Line2D.Float(174, 186, cx + rad, cy));

        // Micro-dots de conexão
        g.setColor(new Color(56, 189, 248, 200));
        g.fill(new Ellipse2D.Float(82 - 2f, 186 - 2f, 4f, 4f));
        g.fill(new Ellipse2D.Float(174 - 2f, 186 - 2f, 4f, 4f));

        g.dispose();
        return img;
    }

    private static BufferedImage scale(BufferedImage src, int size) {
        if (src.getWidth() == size && src.getHeight() == size) {
            return src;
        }
        BufferedImage dest = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.drawImage(src, 0, 0, size, size, null);
        g2.dispose();
        return dest;
    }

    /**
     * Compila um arquivo .ico do Windows contendo múltiplos PNGs comprimidos.
     */
    public static byte[] buildIco(List<BufferedImage> images) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int count = images.size();

        // 1. ICO Header (6 bytes, little-endian)
        dos.writeShort(Short.reverseBytes((short) 0));      // Reserved
        dos.writeShort(Short.reverseBytes((short) 1));      // Type (1 = ICO)
        dos.writeShort(Short.reverseBytes((short) count));  // Image count

        // Preparar os dados PNG de cada imagem
        List<byte[]> pngBytesList = new ArrayList<>();
        for (BufferedImage img : images) {
            ByteArrayOutputStream pngOs = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", pngOs);
            pngBytesList.add(pngOs.toByteArray());
        }

        // 2. Directory Entries (16 bytes per image)
        int offset = 6 + (16 * count);
        for (int i = 0; i < count; i++) {
            BufferedImage img = images.get(i);
            byte[] pngData = pngBytesList.get(i);

            int width = img.getWidth() >= 256 ? 0 : img.getWidth();
            int height = img.getHeight() >= 256 ? 0 : img.getHeight();

            dos.writeByte(width);                                     // Width
            dos.writeByte(height);                                    // Height
            dos.writeByte(0);                                         // Color count (0 = >8bpp)
            dos.writeByte(0);                                         // Reserved
            dos.writeShort(Short.reverseBytes((short) 1));            // Color planes
            dos.writeShort(Short.reverseBytes((short) 32));           // Bits per pixel
            dos.writeInt(Integer.reverseBytes(pngData.length));       // Data size
            dos.writeInt(Integer.reverseBytes(offset));               // Data offset

            offset += pngData.length;
        }

        // 3. Image Data (PNG blocks)
        for (byte[] pngData : pngBytesList) {
            dos.write(pngData);
        }

        dos.flush();
        return baos.toByteArray();
    }
}
