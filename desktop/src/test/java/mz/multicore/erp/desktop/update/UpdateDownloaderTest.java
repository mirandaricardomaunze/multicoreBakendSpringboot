package mz.multicore.erp.desktop.update;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AU-03: download limits, cancellation and manifest errors against a real local HTTP server. */
class UpdateDownloaderTest {

    @TempDir Path dir;
    private HttpServer server;
    private final byte[] installer = "x".repeat(200_000).getBytes(StandardCharsets.US_ASCII);

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/downloads/Multicore-instalador.exe", exchange -> {
            exchange.sendResponseHeaders(200, installer.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(installer);
            }
        });
        server.createContext("/downloads/Multicore-instalador.json", exchange -> {
            byte[] body = ("{\"version\":\"1.0.4\",\"sha256\":\"" + "c".repeat(64) + "\",\"size\":200000}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", url("/downloads/Multicore-instalador.json").toString());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    void download_withinLimit_writesFileAndReportsProgress() throws Exception {
        Path target = dir.resolve("updates/Multicore-1.0.4.exe.part");
        AtomicLong last = new AtomicLong();

        new UpdateDownloader().download(url("/downloads/Multicore-instalador.exe"), target, installer.length,
                last::set, () -> false);

        assertThat(Files.size(target)).isEqualTo(installer.length);
        assertThat(last).hasValue(installer.length);
    }

    @Test
    void download_largerThanAnnounced_stopsAndDeletes() {
        Path target = dir.resolve("big.part");

        assertThatThrownBy(() -> new UpdateDownloader().download(url("/downloads/Multicore-instalador.exe"), target,
                1000, n -> { }, () -> false)).hasMessageContaining("maior do que o servidor anunciou");
        assertThat(target).doesNotExist();
    }

    @Test
    void download_cancelled_deletes() {
        Path target = dir.resolve("cancel.part");

        assertThatThrownBy(() -> new UpdateDownloader().download(url("/downloads/Multicore-instalador.exe"), target,
                installer.length, n -> { }, () -> true)).hasMessageContaining("cancelada");
        assertThat(target).doesNotExist();
    }

    @Test
    void fetchManifest_readsPublishedManifest() {
        assertThat(new UpdateDownloader().fetchManifest(url("/downloads/Multicore-instalador.json")).version())
                .isEqualTo("1.0.4");
    }

    @Test
    void fetchManifest_notPublished_saysSo() {
        assertThatThrownBy(() -> new UpdateDownloader().fetchManifest(url("/downloads/nada.json")))
                .hasMessageContaining("ainda não publicou");
    }

    @Test
    void fetchManifest_neverFollowsRedirects() {
        assertThatThrownBy(() -> new UpdateDownloader().fetchManifest(url("/moved")))
                .hasMessageContaining("302");
    }
}
