package com.phcpro.modules.printing;

import com.phcpro.architecture.exception.BusinessRuleException;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Saves PDF bytes under user-home / phc-pdfs and opens them with the OS PDF viewer. */
public final class PdfFileSaver {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path OUTPUT_DIR = Paths.get(System.getProperty("user.home"), "phc-pdfs");

    private PdfFileSaver() {}

    public static Path saveAndOpen(byte[] bytes, String baseName) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            String safe = baseName.replaceAll("[^A-Za-z0-9_.-]", "_");
            Path file = OUTPUT_DIR.resolve(safe + "-" + LocalDateTime.now().format(STAMP) + ".pdf");
            Files.write(file, bytes);
            tryOpen(file);
            return file;
        } catch (IOException e) {
            throw new BusinessRuleException("Não foi possível gravar o PDF: " + e.getMessage());
        }
    }

    private static void tryOpen(Path file) {
        if (!Desktop.isDesktopSupported()) return;
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(file.toFile());
            }
        } catch (IOException ignored) {
            // OS unable to open viewer — file is still saved.
        }
    }
}
