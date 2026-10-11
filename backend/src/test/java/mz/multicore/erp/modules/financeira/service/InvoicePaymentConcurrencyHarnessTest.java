package mz.multicore.erp.modules.financeira.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness que impede regressão para liquidação concorrente sem lock tenant-scoped. */
class InvoicePaymentConcurrencyHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void invoicePaymentLocksTheDocumentBeforeCheckingAndMovingMoney() throws Exception {
        String repository = read("backend/src/main/java/mz/multicore/erp/modules/comercial/repository/InvoiceRepository.java");
        assertThat(repository).contains(
                "@Lock(LockModeType.PESSIMISTIC_WRITE)",
                "Optional<Invoice> lockByIdAndCompanyId",
                "i.id = :id and i.company.id = :companyId");

        String service = read("backend/src/main/java/mz/multicore/erp/modules/financeira/service/FinanceService.java");
        int lock = service.indexOf("invoiceRepository.lockByIdAndCompanyId");
        int collectableCheck = service.indexOf("invoice.getStatus().isCollectable()", lock);
        int moneyMovement = service.indexOf("registerTransaction(accountId", lock);
        assertThat(lock).isGreaterThanOrEqualTo(0);
        assertThat(collectableCheck).isGreaterThan(lock);
        assertThat(moneyMovement).isGreaterThan(collectableCheck);
    }

    @Test
    void receiptCreationUsesTheSameInvoiceLockBeforeCheckingOutstandingBalance() throws Exception {
        String service = read("backend/src/main/java/mz/multicore/erp/modules/comercial/service/ComercialService.java");
        int method = service.indexOf("public ReceiptDTO createReceipt");
        int lock = service.indexOf("invoiceRepository.lockByIdAndCompanyId", method);
        int outstanding = service.indexOf("invoice.outstandingAmount()", method);
        int receiptSave = service.indexOf("receiptRepository.save(receipt)", method);
        assertThat(method).isGreaterThanOrEqualTo(0);
        assertThat(lock).isGreaterThan(method);
        assertThat(outstanding).isGreaterThan(lock);
        assertThat(receiptSave).isGreaterThan(outstanding);
    }

    @Test
    void receiptCancellationLocksReceiptAndInvoiceBeforeRefundingMoney() throws Exception {
        String receiptRepository = read("backend/src/main/java/mz/multicore/erp/modules/comercial/repository/ReceiptRepository.java");
        assertThat(receiptRepository).contains(
                "@Lock(LockModeType.PESSIMISTIC_WRITE)",
                "Optional<Receipt> lockByIdAndCompanyId");

        String service = read("backend/src/main/java/mz/multicore/erp/modules/comercial/service/ComercialService.java");
        int method = service.indexOf("public void cancelReceipt");
        int receiptLock = service.indexOf("receiptRepository.lockByIdAndCompanyId", method);
        int invoiceLock = service.indexOf("invoiceRepository.lockByIdAndCompanyId", receiptLock);
        int refund = service.indexOf("financeService.registerTransaction", invoiceLock);
        assertThat(receiptLock).isGreaterThan(method);
        assertThat(invoiceLock).isGreaterThan(receiptLock);
        assertThat(refund).isGreaterThan(invoiceLock);
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
