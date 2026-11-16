package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma venda foi registada (fatura emitida, incluindo venda de balcão).
 *
 * <p><b>Porquê um evento e não uma chamada directa:</b> o módulo comercial não pode passar a
 * conhecer a contabilidade só para a manter informada — seria mais uma dependência entre
 * domínios, contra a direcção que este projecto tem seguido. O evento leva <b>números</b>, nunca
 * entidades JPA, para que quem o consome não fique preso ao modelo de quem o emite.
 *
 * <p>Os {@code @EventListener} do Spring correm de forma síncrona e na <b>mesma transacção</b>:
 * se a contabilidade falhar, a venda não fica gravada pela metade.
 *
 * @param netAmount     valor sem imposto (proveito)
 * @param taxAmount     IVA liquidado
 * @param totalAmount   total do documento (o que o cliente deve/pagou)
 * @param costOfGoods   custo das mercadorias vendidas, no acto da venda; zero se desconhecido
 * @param amountPaidNow quanto entrou em dinheiro no acto (0 = venda inteiramente a crédito)
 * @param cashPayment   o que foi pago entrou em numerário (caixa) e não por banco
 * @param cashPaidNow   parte de {@code amountPaidNow} em numerário; o resto entrou por banco (cartão, M-Pesa)
 */
public record SaleRegisteredEvent(
        Long companyId,
        Long invoiceId,
        String invoiceNumber,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal costOfGoods,
        BigDecimal amountPaidNow,
        boolean cashPayment,
        BigDecimal cashPaidNow
) {
    /** Um único meio de pagamento: tudo em numerário ou tudo por banco. */
    public SaleRegisteredEvent(Long companyId, Long invoiceId, String invoiceNumber, LocalDate date, BigDecimal netAmount,
                               BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal costOfGoods,
                               BigDecimal amountPaidNow, boolean cashPayment) {
        this(companyId, invoiceId, invoiceNumber, date, netAmount, taxAmount, totalAmount, costOfGoods, amountPaidNow,
                cashPayment, cashPayment ? amountPaidNow : BigDecimal.ZERO);
    }
}
