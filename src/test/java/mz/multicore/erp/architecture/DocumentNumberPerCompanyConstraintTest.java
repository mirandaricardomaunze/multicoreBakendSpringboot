package mz.multicore.erp.architecture;

import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do bug multi-tenant de numeração, cobrindo TODOS os tipos de documento de uma vez.
 *
 * <p>O número de cada documento é gerado <b>por empresa</b> (ver {@code DocumentNumberService}). A
 * unicidade na BD tem, portanto, de ser {@code UNIQUE(company_id, número)} — e <b>não</b>
 * {@code UNIQUE(número)} global, que fazia duas empresas colidir (fix nas migrações V31/V32).
 *
 * <p>Este teste verifica por reflexão a <b>invariante</b> em cada entidade: (1) o {@code @Table} declara
 * uma {@code @UniqueConstraint} sobre {@code (company_id, coluna_do_número)}; (2) a coluna do número
 * <b>não</b> tem {@code unique = true} (que reintroduziria a unicidade global). Guarda contra reverter
 * acidentalmente qualquer uma. A <i>aplicação</i> em runtime (H2) está coberta, por amostragem, pelos
 * testes de comportamento {@code InvoiceNumberUniquenessPerCompanyTest} e
 * {@code PayslipNumberUniquenessPerCompanyTest}.
 */
class DocumentNumberPerCompanyConstraintTest {

    static Stream<Arguments> documentEntities() {
        return Stream.of(
                Arguments.of(Invoice.class, "invoiceNumber", "invoice_number"),
                Arguments.of(Order.class, "orderNumber", "order_number"),
                Arguments.of(CreditNote.class, "noteNumber", "note_number"),
                Arguments.of(DebitNote.class, "noteNumber", "note_number"),
                Arguments.of(Receipt.class, "receiptNumber", "receipt_number"),
                Arguments.of(Purchase.class, "purchaseNumber", "purchase_number"),
                Arguments.of(PurchaseOrder.class, "orderNumber", "order_number"),
                Arguments.of(StockTransfer.class, "transferNumber", "transfer_number"),
                Arguments.of(Payslip.class, "payslipNumber", "payslip_number")
        );
    }

    @ParameterizedTest(name = "{0} numera por empresa (nao global)")
    @MethodSource("documentEntities")
    void numberIsUniquePerCompanyNotGlobal(Class<?> entity, String numberField, String numberColumn) throws Exception {
        Table table = entity.getAnnotation(Table.class);
        assertNotNull(table, entity.getSimpleName() + " deveria ter @Table");

        boolean hasCompositeUnique = Arrays.stream(table.uniqueConstraints()).anyMatch(uc -> {
            List<String> cols = Arrays.stream(uc.columnNames()).map(String::toLowerCase).toList();
            return cols.contains("company_id") && cols.contains(numberColumn.toLowerCase());
        });
        assertTrue(hasCompositeUnique, entity.getSimpleName()
                + ": falta @UniqueConstraint(company_id, " + numberColumn + ") — numeração é por empresa.");

        Field field = entity.getDeclaredField(numberField);
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column, entity.getSimpleName() + "." + numberField + " deveria ter @Column");
        assertFalse(column.unique(), entity.getSimpleName() + "." + numberField
                + ": unique=true reintroduz a unicidade GLOBAL — duas empresas colidiriam no mesmo número.");
    }
}
