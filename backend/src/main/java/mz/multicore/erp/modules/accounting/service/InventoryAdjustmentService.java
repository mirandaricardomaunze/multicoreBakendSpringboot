package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.InventoryAdjustmentDTO;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.inventory.service.StockValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * Brings the inventory accounts in line with the stock: opening stock entered by hand (ENTRY) is never posted, so
 * Mercadorias (3201) and Produtos acabados (3401) start below the real stock and go negative as sales take their cost
 * out. This posts the difference between the stock at cost and each account's book balance, against Resultados
 * transitados (5901). One adjustment per day; the accountant confirms the counterpart for their case.
 */
@Service
public class InventoryAdjustmentService {

    private static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

    private final StockValuationService stockValuation;
    private final GoodsAccounts goodsAccounts;
    private final JournalService journalService;
    private final PostingHelper postingHelper;
    private final ChartOfAccountsMutationService chartMutation;

    public InventoryAdjustmentService(StockValuationService stockValuation, GoodsAccounts goodsAccounts,
                                      JournalService journalService, PostingHelper postingHelper,
                                      ChartOfAccountsMutationService chartMutation) {
        this.stockValuation = stockValuation;
        this.goodsAccounts = goodsAccounts;
        this.journalService = journalService;
        this.postingHelper = postingHelper;
        this.chartMutation = chartMutation;
    }

    @Transactional
    public InventoryAdjustmentDTO adjust() {
        PermissionGuard.requireManagerOrAdmin("acertar as existências na contabilidade");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        chartMutation.seedDefaultChart(); // adds 5901 to charts created before it existed
        LocalDate today = LocalDate.now();
        if (postingHelper.alreadyPosted(companyId, JournalSource.INVENTORY_ADJUSTMENT, today.toEpochDay())) {
            throw new BusinessRuleException("As existências já foram acertadas hoje. Volte a fazê-lo amanhã, se preciso.");
        }

        BigDecimal goodsStock = BigDecimal.ZERO, finishedStock = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> value : stockValuation.valueByProduct(companyId).entrySet()) {
            if (PgcNirfChart.PRODUTOS_ACABADOS.equals(goodsAccounts.accountFor(companyId, value.getKey()))) {
                finishedStock = finishedStock.add(value.getValue());
            } else {
                goodsStock = goodsStock.add(value.getValue());
            }
        }
        goodsStock = goodsStock.setScale(2, RoundingMode.HALF_UP);
        finishedStock = finishedStock.setScale(2, RoundingMode.HALF_UP);
        BigDecimal goodsBook = bookBalance(companyId, PgcNirfChart.MERCADORIAS, today);
        BigDecimal finishedBook = bookBalance(companyId, PgcNirfChart.PRODUTOS_ACABADOS, today);

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(today);
        entry.setDescription("Acerto de existências ao stock físico");
        entry.setSource(JournalSource.INVENTORY_ADJUSTMENT);
        entry.setSourceDocumentId(today.toEpochDay());
        entry.setSourceDocumentNumber("AE-" + today);
        addDifference(entry, companyId, PgcNirfChart.MERCADORIAS, goodsStock.subtract(goodsBook));
        addDifference(entry, companyId, PgcNirfChart.PRODUTOS_ACABADOS, finishedStock.subtract(finishedBook));
        boolean posted = !entry.getLines().isEmpty();
        if (posted) journalService.save(entry, companyId);
        return new InventoryAdjustmentDTO(goodsStock, goodsBook, finishedStock, finishedBook, posted);
    }

    private void addDifference(JournalEntry entry, Long companyId, String account, BigDecimal difference) {
        if (difference.signum() == 0) return;
        BigDecimal amount = difference.abs();
        boolean up = difference.signum() > 0;
        entry.addLine(up ? JournalLine.debit(postingHelper.account(companyId, account), amount, "Acerto ao stock físico")
                : JournalLine.credit(postingHelper.account(companyId, account), amount, "Acerto ao stock físico"));
        entry.addLine(up ? JournalLine.credit(postingHelper.account(companyId, PgcNirfChart.RESULTADOS_TRANSITADOS), amount, "Acerto de existências")
                : JournalLine.debit(postingHelper.account(companyId, PgcNirfChart.RESULTADOS_TRANSITADOS), amount, "Acerto de existências"));
    }

    private BigDecimal bookBalance(Long companyId, String code, LocalDate asOf) {
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalEntry posted : journalService.findEntriesBetween(companyId, BEGINNING, asOf)) {
            for (JournalLine line : posted.getLines()) {
                if (line.getAccount() != null && code.equals(line.getAccount().getCode())) {
                    balance = balance.add(line.safeDebit()).subtract(line.safeCredit());
                }
            }
        }
        return balance.setScale(2, RoundingMode.HALF_UP);
    }
}
