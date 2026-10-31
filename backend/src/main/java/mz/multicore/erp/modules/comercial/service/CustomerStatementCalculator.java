package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculador financeiro de extrato de conta corrente de clientes com saldo progressivo e apuração de saldos.
 * Ver {@code docs/CUSTOMER_STATEMENT_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Component
public class CustomerStatementCalculator {

    public CustomerStatementDTO calculate(
            Client client,
            LocalDate from,
            LocalDate to,
            List<CustomerStatementMovement> events,
            BigDecimal overdue
    ) {
        // Ordenação cronológica estrita null-safe
        events.sort(Comparator.comparing(CustomerStatementMovement::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CustomerStatementMovement::timestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CustomerStatementMovement::documentNumber, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<CustomerStatementLineDTO> lines = new ArrayList<>();

        for (CustomerStatementMovement mov : events) {
            if (mov.date().isBefore(from)) {
                openingBalance = openingBalance.add(mov.debit()).subtract(mov.credit());
            }
        }

        BigDecimal running = openingBalance;
        for (CustomerStatementMovement mov : events) {
            if (!mov.date().isBefore(from) && !mov.date().isAfter(to)) {
                running = running.add(mov.debit()).subtract(mov.credit());
                totalDebits = totalDebits.add(mov.debit());
                totalCredits = totalCredits.add(mov.credit());

                lines.add(new CustomerStatementLineDTO(
                        mov.date(),
                        mov.type(),
                        mov.documentNumber(),
                        mov.description(),
                        mov.reference(),
                        mov.debit(),
                        mov.credit(),
                        running,
                        mov.businessUnitId(),
                        mov.businessUnitName()
                ));
            }
        }

        BigDecimal closingBalance = openingBalance.add(totalDebits).subtract(totalCredits);

        return new CustomerStatementDTO(
                client.getId(),
                client.getName(),
                client.getTaxId(),
                client.getAddress(),
                client.getEmail(),
                null,
                from,
                to,
                openingBalance,
                totalDebits,
                totalCredits,
                closingBalance,
                overdue,
                lines
        );
    }
}
