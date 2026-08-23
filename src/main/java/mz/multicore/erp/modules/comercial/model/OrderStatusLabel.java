package mz.multicore.erp.modules.comercial.model;

/**
 * Rótulos PT-MZ dos estados da encomenda. <b>Fonte única</b> — o operador (e o cliente, no
 * documento impresso) nunca vê o nome da constante.
 *
 * <p>É a regra que a ENCOMENDA_DUAS_VIAS_SPEC (ED-04) impôs aos ecrãs e que o A4 escapava: o PDF
 * enviado ao cliente terminava com {@code Estado: PENDING_APPROVAL}. A tradução chegou a existir
 * <b>privada e incompleta</b> dentro do {@code CustomerOrderFulfillmentService} — só os estados de
 * separação, ignorando {@code PENDING_APPROVAL}, {@code PENDING}, {@code BILLED},
 * {@code GUIDE_PENDING} e {@code GUIDED}. Agora é uma só, e completa.
 *
 * <p>{@code Order.status} é uma {@code String} (não uma enumeração), por isso a tradução vive aqui
 * e não no tipo. Ver docs/ENCOMENDA_PROFISSIONAL_SPEC.md §P4.
 */
public final class OrderStatusLabel {

    private OrderStatusLabel() {}

    /** Rótulo em PT-MZ. Estado desconhecido devolve-se tal e qual — nunca rebenta um documento. */
    public static String of(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }
        return switch (status.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING_APPROVAL" -> "Pendente de aprovação";
            case "PENDING" -> "Por facturar";
            case "AWAITING_SEPARATION" -> "Aguarda separação";
            case "IN_SEPARATION" -> "Em separação";
            case "SEPARATED" -> "Separado";
            case "GUIDE_PENDING" -> "Guia por aprovar";
            case "GUIDED" -> "Expedido por guia";
            case "TRANSFER_PENDING" -> "Transferência por aprovar";
            case "TRANSFERRED" -> "Transferido para a loja";
            case "BILLED", "INVOICED" -> "Facturado";
            case "CANCELLED" -> "Cancelado";
            default -> status;
        };
    }
}
