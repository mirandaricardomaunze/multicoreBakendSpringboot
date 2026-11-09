package mz.multicore.erp.modules.returnables.model;

/** OUT: left with the client. IN: came back. CHARGED: not returned and invoiced, which closes that quantity. */
public enum ReturnableMovementType {
    OUT,
    IN,
    CHARGED
}
