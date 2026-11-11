package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.hr.model.PayrollIrpsBracket;
import mz.multicore.erp.modules.hr.model.PayrollTaxConfig;
import mz.multicore.erp.modules.hr.repository.PayrollTaxConfigRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Order(2)
public class PayrollTaxConfigSeeder implements CommandLineRunner {
    private final CompanyLookup companyLookup;
    private final PayrollTaxConfigRepository configRepository;

    public PayrollTaxConfigSeeder(CompanyLookup companyLookup, PayrollTaxConfigRepository configRepository) {
        this.companyLookup = companyLookup;
        this.configRepository = configRepository;
    }

    @Override
    public void run(String... args) {
        companyLookup.findAll().forEach(company -> {
            if (configRepository.existsByCompanyId(company.getId())) return;
            PayrollTaxConfig config = new PayrollTaxConfig();
            config.setCompany(company);
            config.setName("MZ IRPS/INSS - Configuração Inicial");
            config.setEffectiveFrom(LocalDate.of(2026, 1, 1));
            config.setEmployeeInssRate(new BigDecimal("0.0300"));
            config.setEmployerInssRate(new BigDecimal("0.0400"));
            config.setLegalBasis("Configuração inicial baseada em escalões progressivos IRPS e INSS 3%/4%; validar com AT/INSS antes de produção.");
            addBracket(config, "0", "3500", "0.10", "0");
            addBracket(config, "3500.01", "14000", "0.15", "175");
            addBracket(config, "14000.01", "42000", "0.20", "875");
            addBracket(config, "42000.01", "126000", "0.25", "2975");
            addBracket(config, "126000.01", null, "0.32", "11795");
            configRepository.save(config);
        });
    }

    private void addBracket(PayrollTaxConfig config, String lower, String upper, String rate, String deduction) {
        PayrollIrpsBracket bracket = new PayrollIrpsBracket();
        bracket.setConfig(config);
        bracket.setLowerBound(new BigDecimal(lower));
        bracket.setUpperBound(upper == null ? null : new BigDecimal(upper));
        bracket.setRate(new BigDecimal(rate));
        bracket.setFixedDeduction(new BigDecimal(deduction));
        bracket.setDependentDeduction(BigDecimal.ZERO);
        config.getBrackets().add(bracket);
    }
}
