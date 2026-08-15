package com.phcpro.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um cliente pagou (recibo emitido ou fatura liquidada em tesouraria).
 *
 * <p>Só o <b>recebimento</b>: a venda já foi lançada quando a fatura foi emitida. Aqui o que
 * muda é a natureza do saldo — sai de Clientes e entra em Caixa/Banco. Lançar aqui outra vez o
 * proveito seria contar a mesma venda duas vezes.
 *
 * @param cashPayment entrou em numerário (caixa) e não por banco
 */
public record PaymentReceivedEvent(
        Long companyId,
        Long receiptId,
        String receiptNumber,
        LocalDate date,
        BigDecimal amount,
        boolean cashPayment
) {}
