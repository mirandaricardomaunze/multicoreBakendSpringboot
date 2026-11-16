package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.BiFunction;

/**
 * Chooses the inventory account of a stock movement: 3401 Produtos acabados for what the company makes, 3201
 * Mercadorias for what it buys. Without the lookup or without account 3401 in the chart, everything stays in 3201.
 */
@Component
public class GoodsAccounts {

    private final PostingHelper postingHelper;
    private final FinishedGoodsLookup lookup;

    @Autowired
    public GoodsAccounts(PostingHelper postingHelper, @Autowired(required = false) FinishedGoodsLookup lookup) {
        this.postingHelper = postingHelper;
        this.lookup = lookup;
    }

    /** The account that holds this product: 3401 if it is manufactured, otherwise 3201. */
    public String accountFor(Long companyId, Long productId) {
        return productId != null && finishedAccountExists(companyId) && lookup.isManufactured(companyId, productId)
                ? PgcNirfChart.PRODUTOS_ACABADOS : PgcNirfChart.MERCADORIAS;
    }

    /** Sale: D CMVMC / C 3201 for goods and C 3401 for manufactured products. */
    public void addSaleCost(JournalEntry entry, Long companyId, Long invoiceId, BigDecimal cost) {
        addCost(entry, companyId, invoiceId, cost, false, (c, id) -> lookup.finishedCostOfInvoice(c, id));
    }

    /** Return: the reverse of the sale, back into the account each product came from. */
    public void addReturnCost(JournalEntry entry, Long companyId, Long noteId, BigDecimal cost) {
        addCost(entry, companyId, noteId, cost, true, (c, id) -> lookup.finishedCostOfCreditNote(c, id));
    }

    private void addCost(JournalEntry entry, Long companyId, Long documentId, BigDecimal cost, boolean reverse,
                         BiFunction<Long, Long, BigDecimal> finishedCost) {
        if (cost == null || cost.signum() <= 0) return;
        BigDecimal finished = finishedAccountExists(companyId) ? PostingHelper.safe(finishedCost.apply(companyId, documentId))
                .max(BigDecimal.ZERO).min(cost) : BigDecimal.ZERO;
        BigDecimal goods = cost.subtract(finished);
        line(entry, companyId, PgcNirfChart.CMVMC, cost, !reverse, reverse ? "Estorno do custo da venda" : "Custo das mercadorias vendidas");
        line(entry, companyId, PgcNirfChart.MERCADORIAS, goods, reverse, reverse ? "Reposição das mercadorias devolvidas" : "Saída de existências");
        line(entry, companyId, PgcNirfChart.PRODUTOS_ACABADOS, finished, reverse,
                reverse ? "Reposição dos produtos acabados devolvidos" : "Saída de produtos acabados");
    }

    private void line(JournalEntry entry, Long companyId, String code, BigDecimal amount, boolean debit, String text) {
        if (amount.signum() <= 0) return;
        entry.addLine(debit ? JournalLine.debit(postingHelper.account(companyId, code), amount, text)
                : JournalLine.credit(postingHelper.account(companyId, code), amount, text));
    }

    private boolean finishedAccountExists(Long companyId) {
        return lookup != null && postingHelper.hasAccounts(companyId, PgcNirfChart.PRODUTOS_ACABADOS);
    }
}
