package mz.multicore.erp.modules.butchery.model;

/** A breakdown is posted at once (stock moved); a manager may cancel it while the cuts are still in stock. */
public enum CarcassBreakdownStatus {
    POSTED,
    CANCELLED
}
