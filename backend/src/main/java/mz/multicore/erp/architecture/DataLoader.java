package mz.multicore.erp.architecture;

import mz.multicore.erp.modules.comercial.service.ClientCommandService;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.crm.service.CRMService;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.service.HRService;
import mz.multicore.erp.modules.inventory.service.StockMovementService;
import mz.multicore.erp.modules.inventory.service.WarehouseCommandService;
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
            @Value("${app.seed-demo-data:false}") boolean seedDemoData,
            ReferenceAndAdminDataSeeder referenceSeeder,
            DemoEnterpriseDataSeeder demoSeeder) {
        this.seedDemoData = seedDemoData;
        this.referenceSeeder = referenceSeeder;
        this.demoSeeder = demoSeeder;
    }

    public DataLoader(
            EmployeeRepository employeeRepository,
            ClientCommandService clientCommandService,
            ProductLookup productLookup,
            ProductCommandService productCommandService,
            TreasuryAccountRepository accountRepository,
            HRService hrService,
            CRMService crmService,
            CompanyLookup companyLookup,
            CompanyService companyService,
            WarehouseCommandService warehouseCommandService,
            StockMovementService stockMovementService,
            AppUserRepository appUserRepository,
            SupplierRepository supplierRepository,
            TaxRateRepository taxRateRepository,
            boolean seedDemoData,
            String superAdminPassword,
            PasswordEncoder passwordEncoder) {
        this.seedDemoData = seedDemoData;
        this.referenceSeeder = new ReferenceAndAdminDataSeeder(
                companyLookup, appUserRepository, taxRateRepository,
                productLookup, productCommandService, superAdminPassword, passwordEncoder);
        this.demoSeeder = new DemoEnterpriseDataSeeder(
                employeeRepository, clientCommandService, productLookup, productCommandService, accountRepository,
                hrService, crmService, companyLookup, companyService, warehouseCommandService, stockMovementService,
                appUserRepository, supplierRepository, taxRateRepository,
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
