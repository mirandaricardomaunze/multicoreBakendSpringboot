package mz.multicore.erp.modules.comercial.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Todos os estados da encomenda num só sítio, com as perguntas de negócio que se fazem sobre eles.
 *
 * <p>A coluna {@code customer_orders.status} continua a ser texto com exactamente estes nomes:
 * mudar o tipo da coluna obrigava a migrar dados e o desktop, sem ganho para o operador. O que se
 * proíbe é comparar com literais soltos no código — {@code "BILLED"} escrito à mão em cinco sítios
 * foi o que deixou {@code INVOICED} de fora do "em atraso".
 *
 * <p>{@code BILLED} (faturação directa) e {@code INVOICED} (fim do circuito de separação) significam
 * o mesmo para o negócio — {@link #isBilled(String)} trata os dois. Os dois nomes ficam porque já
 * existem em dados gravados e no histórico de eventos.
 */
public enum OrderStatus {
    PENDING_APPROVAL,
    PENDING,
    AWAITING_SEPARATION,
    IN_SEPARATION,
    SEPARATED,
    GUIDE_PENDING,
    GUIDED,
    TRANSFER_PENDING,
    TRANSFERRED,
    BILLED,
    INVOICED,
    CANCELLED;

    private static final Set<OrderStatus> BILLED_STATES = EnumSet.of(BILLED, INVOICED);
    /** Encomenda feita ou desistida: já não há entrega pendente nem nada a alterar. */
    private static final Set<OrderStatus> CLOSED_STATES = EnumSet.of(BILLED, INVOICED, GUIDED, TRANSFERRED, CANCELLED);
    /** Antes de haver guia, transferência ou fatura — cancelar não deixa documento órfão. */
    private static final Set<OrderStatus> CANCELLABLE_STATES =
            EnumSet.of(PENDING_APPROVAL, PENDING, AWAITING_SEPARATION, IN_SEPARATION, SEPARATED);
    private static final Set<OrderStatus> EDITABLE_STATES = EnumSet.of(PENDING_APPROVAL, PENDING, AWAITING_SEPARATION);

    /** Estado gravado convertido; {@code null} se vazio ou desconhecido (nunca rebenta). */
    public static OrderStatus parse(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(s -> s.name().equals(normalized)).findFirst().orElse(null);
    }

    /** O estado gravado é um dos indicados. */
    public static boolean is(String value, OrderStatus... any) {
        OrderStatus status = parse(value);
        return status != null && Arrays.asList(any).contains(status);
    }

    public static boolean isBilled(String value) {
        return BILLED_STATES.contains(parse(value));
    }

    public static boolean isClosed(String value) {
        return CLOSED_STATES.contains(parse(value));
    }

    public static boolean isCancellable(String value) {
        return CANCELLABLE_STATES.contains(parse(value));
    }

    public static boolean isEditable(String value) {
        return EDITABLE_STATES.contains(parse(value));
    }
}
