package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras puras da recepção conferida: impressão digital SHA-256 para idempotência, indexação das
 * linhas declaradas e validação de quantidades/motivo de divergência.
 */
public final class StockTransferReceiptValidator {

    private StockTransferReceiptValidator() {
    }

    public static String receiptFingerprint(String notes, List<ReceiveStockTransferRequest.ReceiveLine> receiptLines) {
        StringBuilder canonical = new StringBuilder();
        appendFingerprintPart(canonical, notes);
        if (receiptLines != null) {
            receiptLines.stream()
                    .sorted(Comparator.comparing(
                            ReceiveStockTransferRequest.ReceiveLine::lineId,
                            Comparator.nullsFirst(Long::compareTo)))
                    .forEach(line -> {
                        appendFingerprintPart(canonical, line.lineId());
                        appendFingerprintPart(canonical, line.receivedQuantity() == null ? null
                                : line.receivedQuantity().stripTrailingZeros().toPlainString());
                        appendFingerprintPart(canonical, line.discrepancyReason());
                    });
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }

    private static void appendFingerprintPart(StringBuilder target, Object value) {
        String text = value == null ? "<null>" : value.toString().trim();
        target.append(text.length()).append(':').append(text).append('|');
    }

    public static Map<Long, ReceiveStockTransferRequest.ReceiveLine> indexReceiptLines(
            StockTransfer transfer, List<ReceiveStockTransferRequest.ReceiveLine> receiptLines) {
        Map<Long, ReceiveStockTransferRequest.ReceiveLine> declared = new HashMap<>();
        if (receiptLines == null || receiptLines.isEmpty()) {
            return declared;
        }
        Set<Long> lineIds = new HashSet<>();
        for (StockTransferLine line : transfer.getLines()) {
            if (line.getId() != null) lineIds.add(line.getId());
        }
        for (ReceiveStockTransferRequest.ReceiveLine r : receiptLines) {
            if (r == null || r.lineId() == null || !lineIds.contains(r.lineId())) {
                throw new BusinessRuleException("A linha indicada na recepção não pertence à guia "
                        + transfer.getTransferNumber() + ".");
            }
            if (declared.put(r.lineId(), r) != null) {
                throw new BusinessRuleException("A mesma linha foi indicada mais de uma vez na recepção.");
            }
        }
        return declared;
    }

    public static void validateReceivedQuantity(StockTransferLine line, BigDecimal sent, BigDecimal received,
                                                ReceiveStockTransferRequest.ReceiveLine req) {
        String product = line.getProduct() != null ? line.getProduct().getName() : "artigo";
        if (received == null || received.signum() < 0) {
            throw new BusinessRuleException("A quantidade recebida de '" + product + "' não pode ser negativa.");
        }
        if (received.compareTo(sent) > 0) {
            throw new BusinessRuleException("A quantidade recebida de '" + product + "' ("
                    + DisplayNumbers.qty(received) + ") excede a enviada ("
                    + DisplayNumbers.qty(sent) + "). Excedentes registam-se por entrada de stock.");
        }
        if (received.compareTo(sent) < 0
                && (req == null || req.discrepancyReason() == null || req.discrepancyReason().isBlank())) {
            throw new BusinessRuleException("Indique o motivo da divergência em '" + product
                    + "': a diferença será devolvida ao armazém de origem.");
        }
    }
}
