package mz.multicore.erp.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** Fetches the manifest and the installer from the fixed {@code /downloads/} addresses of the server (§3, §4.3). */
public final class UpdateDownloader {

    private final HttpClient http;

    public UpdateDownloader() {
        // No redirects: the addresses are fixed, and following one would let the server point elsewhere.
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    UpdateDownloader(HttpClient http) {
        this.http = http;
    }

    public UpdateManifest fetchManifest(URI manifestUrl) {
        try {
            HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(manifestUrl)
                    .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) {
                throw new UpdateException("O servidor ainda não publicou nenhuma actualização do programa.");
            }
            if (response.statusCode() != 200) {
                throw new UpdateException("O servidor respondeu " + response.statusCode()
                        + " ao pedir a actualização. Tente mais tarde.");
            }
            byte[] body = response.body();
            if (body.length > 4096) throw new UpdateException("O manifesto da actualização não é legível.");
            return UpdateManifest.parse(new String(body, StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new UpdateException("Sem ligação ao servidor para verificar a actualização.", error);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new UpdateException("Verificação da actualização interrompida.");
        }
    }

    /**
     * Downloads to {@code target}, never past {@code maxBytes}. A partial or cancelled download is deleted.
     *
     * @param progress receives the bytes received so far
     * @param cancelled polled between blocks; {@code true} stops and deletes
     */
    public void download(URI installerUrl, Path target, long maxBytes, LongConsumer progress,
                         BooleanSupplier cancelled) {
        boolean complete = false;
        try {
            Files.createDirectories(target.getParent());
            HttpResponse<InputStream> response = http.send(HttpRequest.newBuilder(installerUrl).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                if (response.statusCode() != 200) {
                    throw new UpdateException("O servidor respondeu " + response.statusCode()
                            + " ao descarregar o instalador.");
                }
                try (OutputStream out = Files.newOutputStream(target)) {
                    byte[] buffer = new byte[64 * 1024];
                    long total = 0;
                    for (int read; (read = in.read(buffer)) > 0; ) {
                        if (cancelled.getAsBoolean()) throw new UpdateException("Descarga cancelada.");
                        total += read;
                        if (total > maxBytes) {
                            throw new UpdateException("O instalador é maior do que o servidor anunciou. "
                                    + "A descarga foi rejeitada.");
                        }
                        out.write(buffer, 0, read);
                        progress.accept(total);
                    }
                }
            }
            complete = true;
        } catch (IOException error) {
            throw new UpdateException("A descarga do instalador falhou. Verifique a ligação e tente outra vez.", error);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new UpdateException("Descarga interrompida.");
        } finally {
            if (!complete) InstallerVerifier.deleteQuietly(target);
        }
    }
}
