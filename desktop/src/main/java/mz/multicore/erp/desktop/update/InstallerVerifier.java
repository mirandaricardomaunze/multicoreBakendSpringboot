package mz.multicore.erp.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Checks a downloaded installer against the manifest (§4.3): size, SHA-256 and, once a signer is configured,
 * a valid Authenticode signature from that signer. A file that fails any check is deleted.
 */
public final class InstallerVerifier {

    /** Authenticode status and certificate subject of a file, as Windows reports them. */
    public record Signature(String status, String subject) {
    }

    @FunctionalInterface
    public interface SignatureReader {
        Signature read(Path file);
    }

    private final String expectedSigner;
    private final SignatureReader signatureReader;

    public InstallerVerifier(String expectedSigner, SignatureReader signatureReader) {
        this.expectedSigner = expectedSigner == null ? "" : expectedSigner.trim();
        this.signatureReader = signatureReader;
    }

    /** Production verifier: signer from {@code update/trust.properties}, signature read by PowerShell. */
    public static InstallerVerifier standard() {
        return new InstallerVerifier(UpdateTrust.expectedSigner(), InstallerVerifier::readWithPowerShell);
    }

    public boolean requiresSignature() {
        return !expectedSigner.isEmpty();
    }

    public void verify(Path file, UpdateManifest manifest) {
        try {
            long size = Files.size(file);
            if (size != manifest.size()) {
                throw new UpdateException("O instalador descarregado tem " + size + " bytes e o servidor anunciou "
                        + manifest.size() + ". A descarga foi rejeitada.");
            }
            if (!sha256(file).equals(manifest.sha256())) {
                throw new UpdateException("A impressão digital (SHA-256) do instalador não corresponde à publicada "
                        + "no servidor. A descarga foi rejeitada.");
            }
            if (requiresSignature()) verifySignature(file);
        } catch (UpdateException | IOException error) {
            deleteQuietly(file);
            if (error instanceof UpdateException update) throw update;
            throw new UpdateException("Não foi possível ler o instalador descarregado.", error);
        }
    }

    private void verifySignature(Path file) {
        Signature signature = signatureReader.read(file);
        if (signature == null || !"Valid".equalsIgnoreCase(signature.status())) {
            throw new UpdateException("O instalador não tem uma assinatura digital válida ("
                    + (signature == null ? "sem resposta" : signature.status()) + "). A descarga foi rejeitada.");
        }
        String subject = signature.subject() == null ? "" : signature.subject();
        if (!subject.toLowerCase(Locale.ROOT).contains(expectedSigner.toLowerCase(Locale.ROOT))) {
            throw new UpdateException("O instalador está assinado por \"" + subject + "\" e não por \""
                    + expectedSigner + "\". A descarga foi rejeitada.");
        }
    }

    public static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = in.read(buffer)) > 0; ) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static Signature readWithPowerShell(Path file) {
        // The path travels as an argument, never inside the command text.
        List<String> command = List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                "$s = Get-AuthenticodeSignature -LiteralPath $args[0]; "
                        + "Write-Output $s.Status; Write-Output $s.SignerCertificate.Subject",
                file.toAbsolutePath().toString());
        try {
            Process process = UpdateInstallerLauncher.processFor(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String[] lines = output.strip().split("\\R", 2);
            return new Signature(lines[0].strip(), lines.length > 1 ? lines[1].strip() : "");
        } catch (IOException error) {
            return null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A leftover file is re-verified before any use; it can never be installed unchecked.
        }
    }
}
