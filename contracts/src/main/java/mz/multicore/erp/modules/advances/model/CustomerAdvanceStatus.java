package mz.multicore.erp.modules.advances.model;

/** Life cycle of a customer advance. */
public enum CustomerAdvanceStatus {
    /** Received and invoiced; still available for the final invoice. */
    OPEN,
    /** Regularised by a credit note and offset on the final invoice. */
    APPLIED
}
