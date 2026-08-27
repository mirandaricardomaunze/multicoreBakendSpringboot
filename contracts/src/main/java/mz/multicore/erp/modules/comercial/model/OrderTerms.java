package mz.multicore.erp.modules.comercial.model;

/**
 * Origem e condições comerciais de uma encomenda — de onde veio o compromisso e o que foi acordado.
 *
 * <p>Viaja como <b>um</b> valor porque as cinco coisas vêm sempre da mesma decisão comercial e
 * nunca fazem sentido isoladas; {@code placeOrder} já recebia seis argumentos e passaria a onze.
 *
 * <p>{@code deliveryDays} são <b>dias a contar da confirmação</b>, não uma data: a cotação promete
 * "entrega em 7 dias após confirmação" sem saber quando o cliente vai confirmar. A data nasce na
 * conversão ({@link Order#assignExpectedDelivery}). Ver docs/ENCOMENDA_PROFISSIONAL_SPEC.md §P3.
 */
public record OrderTerms(
        Long quotationId,
        String quotationNumber,
        String paymentTerms,
        String deliveryTerms,
        Integer deliveryDays
) {

    private static final OrderTerms NONE = new OrderTerms(null, null, null, null, null);

    /** Encomenda sem acordo prévio — o caso de quem a cria à mão a partir do catálogo. */
    public static OrderTerms none() {
        return NONE;
    }

    /** Nunca devolve nulo: quem não declara condições fica com {@link #none()}. */
    public static OrderTerms orNone(OrderTerms terms) {
        return terms == null ? NONE : terms;
    }
}
