package mz.multicore.erp.modules.consignment.model;

/** A delivery stays open at the reseller until it is settled (returns counted and the sold part invoiced). */
public enum ConsignmentStatus {
    OPEN,
    SETTLED
}
