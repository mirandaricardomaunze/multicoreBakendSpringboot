package mz.multicore.erp.desktop.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * What the server publishes next to the installer ({@code Multicore-instalador.json}): the version it carries,
 * its SHA-256 and its size. The download is accepted only when the file matches all three.
 */
public record UpdateManifest(String version, String sha256, long size) {

    static final long MAX_SIZE = 1L << 30;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+){1,3}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public UpdateManifest {
        if (version == null || !VERSION.matcher(version.trim()).matches()) {
            throw new UpdateException("O manifesto da actualização não indica uma versão válida.");
        }
        if (sha256 == null || !SHA256.matcher(sha256.trim().toLowerCase(Locale.ROOT)).matches()) {
            throw new UpdateException("O manifesto da actualização não traz um SHA-256 válido.");
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new UpdateException("O manifesto da actualização indica um tamanho inválido.");
        }
        version = version.trim();
        sha256 = sha256.trim().toLowerCase(Locale.ROOT);
    }

    public static UpdateManifest parse(String json) {
        JsonNode node;
        try {
            node = MAPPER.readTree(json == null ? "" : json);
        } catch (Exception error) {
            throw new UpdateException("O manifesto da actualização não é legível.");
        }
        if (node == null || !node.isObject()) {
            throw new UpdateException("O manifesto da actualização não é legível.");
        }
        JsonNode size = node.get("size");
        return new UpdateManifest(text(node, "version"), text(node, "sha256"),
                size != null && size.canConvertToLong() ? size.asLong() : -1);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
