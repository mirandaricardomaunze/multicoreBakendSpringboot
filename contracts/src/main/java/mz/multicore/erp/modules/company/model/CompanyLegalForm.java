package mz.multicore.erp.modules.company.model;

/** Legal form of a company as registered at the Mozambican legal entities registry. */
public enum CompanyLegalForm {
    SOLE_TRADER("Empresário em nome individual", null),
    SINGLE_MEMBER_LIMITED("Sociedade unipessoal por quotas", "Lda"),
    PRIVATE_LIMITED("Sociedade por quotas", "Lda"),
    PUBLIC_LIMITED("Sociedade anónima", "SA"),
    COOPERATIVE("Cooperativa", null),
    ASSOCIATION("Associação", null),
    OTHER("Outra", null);

    private final String label;
    private final String abbreviation;

    CompanyLegalForm(String label, String abbreviation) {
        this.label = label;
        this.abbreviation = abbreviation;
    }

    public String label() {
        return label;
    }

    /** Short suffix used after the company name ("Lda", "SA"), or {@code null} when there is none. */
    public String abbreviation() {
        return abbreviation;
    }
}
