package mz.multicore.erp.modules.documents.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Conta bancária do modelo de documento (até 3 por configuração, MODELOS_DOCUMENTO_SPEC §3.1). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DocumentBankAccount {

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "holder", nullable = false, length = 150)
    private String holder;

    /** NIB ou IBAN tal como deve ser impresso. */
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    public DocumentBankAccount(String bankName, String holder, String accountNumber) {
        this.bankName = bankName;
        this.holder = holder;
        this.accountNumber = accountNumber;
    }
}
