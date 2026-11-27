package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderLineRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * "Same content" of an order request (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4.2): SHA-256 of every body field, trimmed
 * (blank = absent), quantities without trailing zeros, lines in the order sent. Each value is length-prefixed so no
 * text can forge a field boundary.
 */
final class PublicOrderFingerprint {

    private PublicOrderFingerprint() {
    }

    static String of(CreatePublicOrderRequest request) {
        List<String> fields = new ArrayList<>(List.of(
                text(request.externalReference()), text(request.nuit()), text(request.customerName()),
                text(request.contactName()), text(request.contactPhone()), text(request.warehouse()),
                text(request.notes())));
        for (PublicOrderLineRequest line : request.lines()) {
            fields.add(text(line.sku()));
            fields.add(quantity(line.quantity()));
        }
        StringBuilder canonical = new StringBuilder();
        fields.forEach(value -> canonical.append(value.length()).append(':').append(value).append(';'));
        return sha256(canonical.toString());
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String quantity(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String sha256(String canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }
}
