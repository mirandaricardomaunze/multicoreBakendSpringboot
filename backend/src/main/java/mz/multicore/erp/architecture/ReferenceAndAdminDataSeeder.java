package mz.multicore.erp.architecture;

import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReferenceAndAdminDataSeeder {

    private final CompanyRepository companyRepository;
    private final AppUserRepository appUserRepository;
    private final TaxRateRepository taxRateRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final String superAdminPassword;
    private final PasswordEncoder passwordEncoder;

    public ReferenceAndAdminDataSeeder(
            CompanyRepository companyRepository,
            AppUserRepository appUserRepository,
            TaxRateRepository taxRateRepository,
            ProductCategoryRepository productCategoryRepository,
            @Value("${app.superadmin.password:}") String superAdminPassword,
            PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.appUserRepository = appUserRepository;
        this.taxRateRepository = taxRateRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.superAdminPassword = superAdminPassword;
        this.passwordEncoder = passwordEncoder;
    }

    public void seedReferenceAndSuperAdmin() {
        seedMozambicanTaxRates();
        seedProductCategories();
        seedSuperAdmin();
    }

    public void seedSuperAdmin() {
        if (appUserRepository.findByUsername("superadmin").isPresent()) return;
        if (superAdminPassword == null || superAdminPassword.isBlank()) return;
        AppUser superAdmin = new AppUser();
        superAdmin.setUsername("superadmin");
        superAdmin.setName("Administrador da Plataforma");
        superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
        superAdmin.setRole("ADMIN");
        superAdmin.setActive(true);
        superAdmin.setPlatformAdmin(true);
        superAdmin.setCreatedBy("SYSTEM");
        appUserRepository.save(superAdmin);
    }

    public void seedDefaultAdmin(Company... companies) {
        AppUser admin = appUserRepository.findByUsername("admin").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("admin");
            u.setName("Administrador Geral");
            u.setPassword(passwordEncoder.encode("admin"));
            u.setRole("ADMIN");
            u.setActive(true);
            u.setCreatedBy("SYSTEM");
            return appUserRepository.save(u);
        });
        if (companies != null && companies.length > 0) {
            for (Company c : companies) {
                if (c != null && admin.findCompanyAccess(c.getId()).isEmpty()) {
                    admin.grantCompany(c, "ADMIN");
                }
            }
        } else {
            companyRepository.findAll().forEach(c -> {
                if (admin.findCompanyAccess(c.getId()).isEmpty()) {
                    admin.grantCompany(c, "ADMIN");
                }
            });
        }
        appUserRepository.save(admin);
    }

    public void seedProductCategories() {
        if (productCategoryRepository.count() > 0) return;
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
        productCategoryRepository.save(c);
    }

    public void seedMozambicanTaxRates() {
        if (taxRateRepository.count() > 0) return;
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
        taxRateRepository.save(t);
    }
}
