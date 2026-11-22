package mz.multicore.erp.modules.company.service;

/**
 * Port answered by each module that owns documents or movements (purchases, invoices, stock): has the company
 * recorded any? The company module asks through this port instead of reading other modules' repositories
 * (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
public interface CompanyMovementProbe {

    boolean hasMovements(Long companyId);
}
