package mz.multicore.erp.gui.components;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/** Traduz o código de uma acção de auditoria para texto que o utilizador entende. */
public final class AuditActionLabels {

    private static final Map<String, String> SUBJECTS = new LinkedHashMap<>();
    private static final Map<String, String> WORDS = Map.ofEntries(
            Map.entry("CREATE", "criação"), Map.entry("UPDATE", "alteração"), Map.entry("DELETE", "eliminação"),
            Map.entry("CANCEL", "anulação"), Map.entry("APPROVE", "aprovação"), Map.entry("REJECT", "rejeição"),
            Map.entry("RECEIVE", "recepção"), Map.entry("PARTIAL", "parcial"), Map.entry("ISSUE", "emissão"),
            Map.entry("CLOSE", "fecho"), Map.entry("OPEN", "abertura"), Map.entry("SESSION", "sessão"),
            Map.entry("PAYMENT", "pagamento"), Map.entry("PAYABLE", "dívida criada"), Map.entry("CONVERT", "conversão"),
            Map.entry("ACCEPT", "aceitação"), Map.entry("BILL", "faturação"), Map.entry("DISCREPANCY", "divergência"),
            Map.entry("SUBMIT", "submissão"), Map.entry("DRAFT", "rascunho"), Map.entry("ADJUSTMENT", "ajuste"),
            Map.entry("STATUS", "estado"), Map.entry("LOGIN", "entrada"), Map.entry("LOGOUT", "saída"));

    static {
        SUBJECTS.put("PURCHASE_ORDER", "Encomenda a fornecedor");
        SUBJECTS.put("STOCK_TRANSFER", "Transferência de stock");
        SUBJECTS.put("DELIVERY_GUIDE", "Guia de remessa");
        SUBJECTS.put("CREDIT_NOTE", "Nota de crédito");
        SUBJECTS.put("DEBIT_NOTE", "Nota de débito");
        SUBJECTS.put("PAYROLL_DEDUCTION", "Desconto salarial");
        SUBJECTS.put("SUPPLIER_PAYMENT", "Pagamento a fornecedor");
        SUBJECTS.put("QUOTATION", "Cotação");
        SUBJECTS.put("INVOICE", "Fatura");
        SUBJECTS.put("RECEIPT", "Recibo");
        SUBJECTS.put("ORDER", "Encomenda");
        SUBJECTS.put("POS", "POS");
        SUBJECTS.put("STOCK", "Stock");
        SUBJECTS.put("EXPENSE", "Despesa");
        SUBJECTS.put("TERMINATION", "Cessação");
        SUBJECTS.put("EMPLOYEE", "Colaborador");
        SUBJECTS.put("CONTRACT", "Contrato");
        SUBJECTS.put("SUPPLIER", "Fornecedor");
        SUBJECTS.put("PRODUCT", "Artigo");
        SUBJECTS.put("USER", "Utilizador");
    }

    private AuditActionLabels() {}

    public static String label(String code) {
        if (code == null || code.isBlank()) return "—";
        for (Map.Entry<String, String> subject : SUBJECTS.entrySet()) {
            if (code.equals(subject.getKey())) return subject.getValue();
            if (code.startsWith(subject.getKey() + "_")) {
                return subject.getValue() + ": " + words(code.substring(subject.getKey().length() + 1));
            }
        }
        String text = words(code);
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private static String words(String code) {
        StringJoiner joined = new StringJoiner(" ");
        for (String word : code.split("_")) {
            joined.add(WORDS.getOrDefault(word, word.toLowerCase(Locale.ROOT)));
        }
        return joined.toString();
    }
}
