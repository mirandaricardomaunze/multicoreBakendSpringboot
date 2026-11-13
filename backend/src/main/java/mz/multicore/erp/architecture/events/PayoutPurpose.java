package mz.multicore.erp.architecture.events;

/** What an automatic payout pays for, so accounting debits the right account without knowing the paying module. */
public enum PayoutPurpose {
    /** Remuneration outside the payslip: 13th month, holiday allowance, final settlement. */
    STAFF_COST,
    /** Money lent or advanced to an employee, recovered later through payslip deductions. */
    STAFF_ADVANCE,
    /** Expenses an employee paid for the company and is reimbursed. */
    STAFF_EXPENSE,
    /** Services bought from outside (e.g. occupational health exams). */
    EXTERNAL_SERVICE
}
