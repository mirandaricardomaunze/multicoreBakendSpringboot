package mz.multicore.erp.architecture;

import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.crm.service.CRMService;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.service.HRService;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {

    private final boolean seedDemoData;
    private final ReferenceAndAdminDataSeeder referenceSeeder;
    private final DemoEnterpriseDataSeeder demoSeeder;

    @Autowired
    public DataLoader(
            @Value("${app.seed-demo-data:true}") boolean seedDemoData,
            ReferenceAndAdminDataSeeder referenceSeeder,
            DemoEnterpriseDataSeeder demoSeeder) {
        this.seedDemoData = seedDemoData;
        this.referenceSeeder = referenceSeeder;
        this.demoSeeder = demoSeeder;
    }

    public DataLoader(
            EmployeeRepository employeeRepository,
            ClientRepository clientRepository,
            ProductRepository productRepository,
            TreasuryAccountRepository accountRepository,
            HRService hrService,
            CRMService crmService,
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository,
            StockRepository stockRepository,
            AppUserRepository appUserRepository,
            SupplierRepository supplierRepository,
            TaxRateRepository taxRateRepository,
            ProductCategoryRepository productCategoryRepository,
            boolean seedDemoData,
            String superAdminPassword,
            PasswordEncoder passwordEncoder) {
        this.seedDemoData = seedDemoData;
        this.referenceSeeder = new ReferenceAndAdminDataSeeder(
                companyRepository, appUserRepository, taxRateRepository,
                productCategoryRepository, superAdminPassword, passwordEncoder);
        this.demoSeeder = new DemoEnterpriseDataSeeder(
                employeeRepository, clientRepository, productRepository, accountRepository,
                hrService, crmService, companyRepository, warehouseRepository, stockRepository,
                appUserRepository, supplierRepository, taxRateRepository, productCategoryRepository,
                passwordEncoder, this.referenceSeeder);
    }

    @Override
    @Transactional
    public void run(String... args) {
        referenceSeeder.seedReferenceAndSuperAdmin();
        if (!seedDemoData) {
            return;
        }
        demoSeeder.seedDemoEnvironment();
    }
}
