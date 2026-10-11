package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Avaliador de Risco e Alçadas de Encomendas de Venda (Padrão Corporativo / Management by Exception).
 *
 * <p>Em grandes sistemas ERP (SAP S/4HANA, Microsoft Dynamics 365, Oracle NetSuite), as encomendas
 * <b>não bloqueiam todas cegamente</b>. A operação de armazém só é travada se a encomenda violar
 * um ou mais dos 4 guardiões de controle interno:
 * <ol>
 *   <li><b>Inadimplência / Mora:</b> Cliente com faturas vencidas há mais de 30 dias.</li>
 *   <li><b>Limite Financeiro de Crédito:</b> Dívida actual + encomenda excede o limite contratado.</li>
 *   <li><b>Política Comercial de Descontos:</b> Desconto concedido superior a 10% (alçada do vendedor).</li>
 *   <li><b>Matriz de Alçadas por Montante:</b>
 *     <ul>
 *       <li>Até 50.000,00 MT: Alçada Operacional (Auto-Aprovado / Green Lane se sem exceções).</li>
 *       <li>50.000,01 MT a 250.000,00 MT: Alçada Gerencial (Requer MANAGER).</li>
 *       <li>Superior a 250.000,00 MT: Alçada da Direção (Requer ADMIN).</li>
 *     </ul>
 *   </li>
 * </ol>
 */
@Component
public class OrderRiskEvaluator {

    public static final BigDecimal SELLER_MAX_AMOUNT = new BigDecimal("50000.00");
    public static final BigDecimal MANAGER_MAX_AMOUNT = new BigDecimal("250000.00");

    public static final BigDecimal MAX_DISCOUNT_SELLER = new BigDecimal("10.00");
    public static final BigDecimal MAX_DISCOUNT_MANAGER = new BigDecimal("25.00");

    public static final int MAX_OVERDUE_DAYS_SELLER = 30;
    public static final int MAX_OVERDUE_DAYS_MANAGER = 60;
    public static final BigDecimal CREDIT_OVERRUN_TOLERANCE_MANAGER = new BigDecimal("0.20");

    private final InvoiceRepository invoiceRepository;

    public OrderRiskEvaluator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public OrderRiskAssessment evaluate(Order order, Long companyId) {
        if (order == null) {
            return OrderRiskAssessment.greenLane("Encomenda nula.");
        }

        List<String> triggers = new ArrayList<>();
        boolean requiresAdmin = false;
        boolean requiresManager = false;

        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        Client client = order.getClient();

        // ─── 1. POLÍTICA DE DESCONTOS COMERCIAIS ────────────────────────────────
        if (order.getLines() != null) {
            for (OrderLine line : order.getLines()) {
                BigDecimal discount = line.getDiscountPercentage() != null ? line.getDiscountPercentage() : BigDecimal.ZERO;
                if (discount.compareTo(MAX_DISCOUNT_SELLER) > 0) {
                    String prodName = line.getProduct() != null ? line.getProduct().getName() : "Item";
                    if (discount.compareTo(MAX_DISCOUNT_MANAGER) > 0) {
                        requiresAdmin = true;
                        triggers.add(String.format("Desconto Crítico: %s%% no item \"%s\" excede limite de gerência (25%%)",
                                discount.stripTrailingZeros().toPlainString(), prodName));
                    } else {
                        requiresManager = true;
                        triggers.add(String.format("Desconto Comercial: %s%% no item \"%s\" excede alçada base (10%%)",
                                discount.stripTrailingZeros().toPlainString(), prodName));
                    }
                }
            }
        }

        // ─── 2. RISCO DE CRÉDITO E MORA DO CLIENTE ─────────────────────────────
        if (client != null && companyId != null) {
            LocalDate today = LocalDate.now();
            List<Invoice> openInvoices = invoiceRepository.findByCompanyId(companyId).stream()
                    .filter(inv -> inv.getClient() != null && client.getId().equals(inv.getClient().getId()))
                    .filter(inv -> inv.getStatus() != null && inv.getStatus().isCollectable())
                    .filter(inv -> inv.outstandingAmount().signum() > 0)
                    .toList();

            BigDecimal currentDebt = BigDecimal.ZERO;
            int maxDaysOverdue = 0;

            for (Invoice inv : openInvoices) {
                currentDebt = currentDebt.add(inv.outstandingAmount());
                int days = inv.daysOverdue(today);
                if (days > maxDaysOverdue) {
                    maxDaysOverdue = days;
                }
            }

            // Checagem de Mora
            if (maxDaysOverdue > MAX_OVERDUE_DAYS_SELLER) {
                if (maxDaysOverdue > MAX_OVERDUE_DAYS_MANAGER) {
                    requiresAdmin = true;
                    triggers.add(String.format("Inadimplência Crítica: Cliente possui fatura vencida há %d dias (limite: %d dias)",
                            maxDaysOverdue, MAX_OVERDUE_DAYS_SELLER));
                } else {
                    requiresManager = true;
                    triggers.add(String.format("Risco de Mora: Cliente possui fatura vencida há %d dias (limite: %d dias)",
                            maxDaysOverdue, MAX_OVERDUE_DAYS_SELLER));
                }
            }

            // Checagem de Limite de Crédito
            if (client.hasCreditLimit()) {
                BigDecimal limit = client.getCreditLimit();
                BigDecimal totalProjectedDebt = currentDebt.add(total);
                if (totalProjectedDebt.compareTo(limit) > 0) {
                    BigDecimal overrun = totalProjectedDebt.subtract(limit);
                    BigDecimal overrunPercent = limit.signum() > 0
                            ? overrun.divide(limit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : new BigDecimal("100");

                    if (overrunPercent.compareTo(CREDIT_OVERRUN_TOLERANCE_MANAGER.multiply(new BigDecimal("100"))) > 0) {
                        requiresAdmin = true;
                        triggers.add(String.format("Limite de Crédito Excedido em %s%% (> 20%%): Dívida projectada %s MT vs Limite %s MT",
                                overrunPercent.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                                totalProjectedDebt.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                                limit.setScale(2, RoundingMode.HALF_UP).toPlainString()));
                    } else {
                        requiresManager = true;
                        triggers.add(String.format("Limite de Crédito Excedido em %s%%: Dívida projectada %s MT vs Limite %s MT",
                                overrunPercent.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                                totalProjectedDebt.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                                limit.setScale(2, RoundingMode.HALF_UP).toPlainString()));
                    }
                }
            }
        }

        // ─── 3. MATRIZ DE ALÇADA POR VALOR ─────────────────────────────────────
        if (total.compareTo(MANAGER_MAX_AMOUNT) > 0) {
            requiresAdmin = true;
            triggers.add(String.format("Alçada de Valor da Administração: Total %s MT excede limite de gerência (250.000,00 MT)",
                    total.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        } else if (total.compareTo(SELLER_MAX_AMOUNT) > 0) {
            requiresManager = true;
            triggers.add(String.format("Alçada de Valor de Gerência: Total %s MT excede alçada base do vendedor (50.000,00 MT)",
                    total.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }

        // ─── DECISÃO CONSOLIDADA ───────────────────────────────────────────────
        if (triggers.isEmpty()) {
            return OrderRiskAssessment.greenLane(String.format(
                    "Auto-aprovado pela Matriz Comercial (Green Lane: Valor %s MT ≤ 50.000 MT, sem mora, crédito em dia e sem descontos fora de alçada).",
                    total.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }

        String finalRole = requiresAdmin ? "ADMIN" : "MANAGER";
        String clientName = client != null ? client.getName() : "Cliente";
        String orderNum = order.getOrderNumber() != null ? order.getOrderNumber() : "Nova Encomenda";

        String description = String.format("Encomenda %s para %s · Total: %s MT · Retido por: [%s]",
                orderNum, clientName, total.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                String.join("] · [", triggers));

        return OrderRiskAssessment.exception(finalRole, triggers, description);
    }
}
