package mz.multicore.erp.architecture;

import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;
import mz.multicore.erp.modules.fiscal.service.TaxRateCommandService;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserCommandService;
import mz.multicore.erp.modules.users.service.AppUserLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReferenceAndAdminDataSeeder {

    private final CompanyLookup companyLookup;
    private final AppUserLookup userLookup;
    private final AppUserCommandService userCommands;
    private final TaxRateLookup taxRateLookup;
    private final TaxRateCommandService taxRateCommands;
    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;
    private final String superAdminPassword;
    private final PasswordEncoder passwordEncoder;

    public ReferenceAndAdminDataSeeder(
            CompanyLookup companyLookup,
            AppUserLookup userLookup,
            AppUserCommandService userCommands,
            TaxRateLookup taxRateLookup,
            TaxRateCommandService taxRateCommands,
            ProductLookup productLookup,
            ProductCommandService productCommandService,
            @Value("${app.superadmin.password:}") String superAdminPassword,
            PasswordEncoder passwordEncoder) {
        this.companyLookup = companyLookup;
        this.userLookup = userLookup;
        this.userCommands = userCommands;
        this.taxRateLookup = taxRateLookup;
        this.taxRateCommands = taxRateCommands;
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
        this.superAdminPassword = superAdminPassword;
        this.passwordEncoder = passwordEncoder;
    }

    public void seedReferenceAndSuperAdmin() {
        seedMozambicanTaxRates();
        seedProductCategories();
        seedSuperAdmin();
    }

    public void seedSuperAdmin() {
        if (userLookup.findByUsername("superadmin").isPresent()) return;
        if (superAdminPassword == null || superAdminPassword.isBlank()) return;
        AppUser superAdmin = new AppUser();
        superAdmin.setUsername("superadmin");
        superAdmin.setName("Administrador da Plataforma");
        superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
        superAdmin.setRole("ADMIN");
        superAdmin.setActive(true);
        superAdmin.setPlatformAdmin(true);
        superAdmin.setCreatedBy("SYSTEM");
        userCommands.saveUser(superAdmin);
    }

    public void seedDefaultAdmin(Company... companies) {
        AppUser admin = userLookup.findByUsername("admin").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("admin");
            u.setName("Administrador Geral");
            u.setPassword(passwordEncoder.encode("admin"));
            u.setRole("ADMIN");
            u.setActive(true);
            u.setCreatedBy("SYSTEM");
            return userCommands.saveUser(u);
        });
        if (companies != null && companies.length > 0) {
            for (Company c : companies) {
                if (c != null && admin.findCompanyAccess(c.getId()).isEmpty()) {
                    admin.grantCompany(c, "ADMIN");
                }
            }
        } else {
            companyLookup.findAll().forEach(c -> {
                if (admin.findCompanyAccess(c.getId()).isEmpty()) {
                    admin.grantCompany(c, "ADMIN");
                }
            });
        }
        userCommands.saveUser(admin);
    }

    public void seedProductCategories() {
        if (productLookup.countCategories() > 0) return;
        seedCategory("ALIMENT", "Alimentação", "#F59E0B");
        seedCategory("BEBIDAS", "Bebidas", "#3B82F6");
        seedCategory("HIGIENE", "Higiene Pessoal", "#10B981");
        seedCategory("LIMPEZA", "Limpeza Doméstica", "#8B5CF6");
        seedCategory("PADARIA", "Padaria & Pastelaria", "#EC4899");
        seedCategory("MERCEAR", "Mercearia Geral", "#6B7280");
        seedCategory("OUTROS", "Outros", "#9CA3AF");
    }

    private void seedCategory(String code, String name, String colorHex) {
        ProductCategory c = new ProductCategory();
        c.setCode(code);
        c.setName(name);
        c.setColorHex(colorHex);
        c.setActive(true);
        c.setCreatedBy("SYSTEM");
        productCommandService.saveCategory(c);
    }

    public void seedMozambicanTaxRates() {
        if (taxRateLookup.countAll() > 0) return;
        seedTaxRate("IVA16", "IVA Normal (16%)", TaxType.IVA_STANDARD, "0.16", "Lei do IVA — DL 8/2024");
        seedTaxRate("IVA5", "IVA Reduzido (5%)", TaxType.IVA_REDUCED, "0.05", "Lei do IVA — alimentos, livros, transportes");
        seedTaxRate("IVA0", "IVA Zero (0%)", TaxType.IVA_ZERO, "0.00", "Lei do IVA — exportações");
        seedTaxRate("IVAEXMT", "IVA Isento", TaxType.IVA_EXEMPT, "0.00", "Lei do IVA — saúde, educação, finanças");
        seedTaxRate("RF_SERV", "Retenção na Fonte — Serviços (10%)", TaxType.WITHHOLDING, "0.10", "Código do IRPS / IRPC");
        seedTaxRate("RF_REND", "Retenção na Fonte — Rendas (14%)", TaxType.WITHHOLDING, "0.14", "Código do IRPS");
        seedTaxRate("RF_NRES", "Retenção na Fonte — Não-residentes (20%)", TaxType.WITHHOLDING, "0.20", "Código do IRPC");
        seedTaxRate("IRPC32", "IRPC — Pessoas Colectivas (32%)", TaxType.CORPORATE_INCOME, "0.32", "Código do IRPC");
        seedTaxRate("ICE_ALC", "ICE — Bebidas alcoólicas", TaxType.EXCISE, "0.55", "Pauta Aduaneira / ICE");
        seedTaxRate("ICE_TAB", "ICE — Tabaco", TaxType.EXCISE, "0.75", "Pauta Aduaneira / ICE");
    }

    private void seedTaxRate(String code, String name, TaxType type, String rate, String legalBasis) {
        TaxRate t = new TaxRate();
        t.setCode(code);
        t.setName(name);
        t.setType(type);
        t.setRate(new BigDecimal(rate));
        t.setLegalBasis(legalBasis);
        t.setActive(true);
        t.setCreatedBy("SYSTEM");
        taxRateCommands.saveTaxRate(t);
    }
}
