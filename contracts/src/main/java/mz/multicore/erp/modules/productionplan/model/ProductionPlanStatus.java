package mz.multicore.erp.modules.productionplan.model;

/** A day plan is open while the bakery produces; closing it records the leftovers and stops new runs. */
public enum ProductionPlanStatus {
    OPEN,
    CLOSED
}
