package mz.multicore.erp.architecture;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ClientCommandService;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.service.CRMService;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.service.EmployeeCommandService;
import mz.multicore.erp.modules.hr.service.HRService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockMovementService;
import mz.multicore.erp.modules.inventory.service.WarehouseCommandService;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.service.SupplierCommandService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserCommandService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DemoEnterpriseDataSeeder {

    private final EmployeeCommandService employeeCommandService;
    private final ClientCommandService clientCommandService;
    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;
    private final TreasuryAccountRepository accountRepository;
    private final HRService hrService;
    private final CRMService crmService;
    private final CompanyLookup companyLookup;
    private final CompanyService companyService;
    private final WarehouseCommandService warehouseCommandService;
    private final StockMovementService stockMovementService;
    private final AppUserCommandService userCommands;
    private final SupplierCommandService supplierCommandService;
    private final TaxRateRepository taxRateRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferenceAndAdminDataSeeder referenceSeeder;

    public DemoEnterpriseDataSeeder(
            EmployeeCommandService employeeCommandService, ClientCommandService clientCommandService,
            ProductLookup productLookup, ProductCommandService productCommandService,
            TreasuryAccountRepository accountRepository,
            HRService hrService, CRMService crmService,
            CompanyLookup companyLookup, CompanyService companyService,
            WarehouseCommandService warehouseCommandService, StockMovementService stockMovementService,
            AppUserCommandService userCommands, SupplierCommandService supplierCommandService,
            TaxRateRepository taxRateRepository,
            PasswordEncoder passwordEncoder, ReferenceAndAdminDataSeeder referenceSeeder) {
        this.employeeCommandService = employeeCommandService;
        this.clientCommandService = clientCommandService;
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
        this.accountRepository = accountRepository;
        this.hrService = hrService;
        this.crmService = crmService;
        this.companyLookup = companyLookup;
        this.companyService = companyService;
        this.warehouseCommandService = warehouseCommandService;
        this.stockMovementService = stockMovementService;
        this.userCommands = userCommands;
        this.supplierCommandService = supplierCommandService;
        this.taxRateRepository = taxRateRepository;
        this.passwordEncoder = passwordEncoder;
        this.referenceSeeder = referenceSeeder;
    }

    public void seedDemoEnvironment() {
        if (companyLookup.count() > 0) {
            referenceSeeder.seedDefaultAdmin();
            return;
        }
        Company ptCompany = createCompany("Multicore Portugal Lda", "501982736", "contacto@multicore.pt", "Avenida da Liberdade 100, Lisboa");
        Company mzCompany = createCompany("Multicore Moçambique Lda", "400123456", "contacto@multicore.co.mz", "Avenida 24 de Julho 1500, Maputo");
        referenceSeeder.seedDefaultAdmin(ptCompany, mzCompany);

        productLookup.findAllCategories().forEach(category -> {
            category.getCompanies().add(ptCompany);
            category.getCompanies().add(mzCompany);
            productCommandService.saveCategory(category);
        });
        taxRateRepository.findAll().forEach(rate -> {
            rate.getCompanies().add(ptCompany);
            rate.getCompanies().add(mzCompany);
            taxRateRepository.save(rate);
        });

        AppUser mariaUser = createAppUser("maria", "Maria Santos", "EMPLOYEE");
        AppUser joaoUser = createAppUser("joao", "João Silva", "MANAGER");
        AppUser anaUser = createAppUser("ana", "Ana Costa", "ADMIN");
        mariaUser.grantCompany(ptCompany, "EMPLOYEE");
        joaoUser.grantCompany(ptCompany, "MANAGER");
        anaUser.grantCompany(ptCompany, "ADMIN");
        anaUser.grantCompany(mzCompany, "ADMIN");
        userCommands.saveUsers(List.of(mariaUser, joaoUser, anaUser));

        Warehouse whLisboa = createWarehouse("Armazém Lisboa Central", "Lisboa", ptCompany);
        createWarehouse("Armazém Porto Norte", "Porto", ptCompany);
        Warehouse whMaputo = createWarehouse("Armazém Maputo Porto", "Maputo", mzCompany);
        createWarehouse("Armazém Beira Centro", "Beira", mzCompany);

        createSupplier("Distribuidor Software Portugal Lda", "509182736", "contacto@distribuidores.pt", "Zona Industrial, Porto", ptCompany);
        createSupplier("Importadora Tecnológica Maputo", "400987654", "contacto@importadortec.co.mz", "Avenida de Angola, Maputo", mzCompany);

        Employee maria = createEmployee(ptCompany, "PT-001", "Maria Santos", "maria.santos@multicore.pt",
                "Recursos Humanos", "1250.00", "EMPLOYEE", LocalDate.of(2022, 1, 10), mariaUser);
        Employee joao = createEmployee(ptCompany, "PT-002", "João Silva", "joao.silva@multicore.pt",
                "Comercial", "2100.00", "MANAGER", LocalDate.of(2021, 5, 3), joaoUser);
        createEmployee(mzCompany, "MZ-001", "Ana Costa", "ana.costa@multicore.pt",
                "Administração", "3800.00", "ADMIN", LocalDate.of(2020, 2, 17), null);

        Client techSol = createClient("Tech Solutions Lda", "501234567", "financeiro@techsolutions.pt", "Avenida da Boavista 1200, Porto", ptCompany);
        Client padaria = createClient("Padaria Central do Minho", "509876543", "geral@padariaminho.pt", "Rua Principal 45, Braga", ptCompany);

        TaxRate ivaExempt = taxRateRepository.findByCode("IVAEXMT").orElse(null);
        TaxRate ivaReduced = taxRateRepository.findByCode("IVA5").orElse(null);
        TaxRate ivaNormal = taxRateRepository.findByCode("IVA16").orElse(null);

        Product erpLic = createProduct("MER-ARROZ-5KG", "MRC-0001", "5601000000017", "Arroz Agulha 5kg", "550.00", "430.00", "20.000", "Saco de arroz agulha de 5kg", ivaExempt, ptCompany, mzCompany);
        Product support = createProduct("MER-ACUCAR-1KG", "MRC-0002", "5601000000024", "Açúcar Branco 1kg", "95.00", "72.00", "40.000", "Pacote de açúcar branco refinado de 1kg", ivaExempt, ptCompany, mzCompany);
        Product techServ = createProduct("MER-OLEO-1L", "MRC-0003", "5601000000031", "Óleo Alimentar 1L", "165.00", "130.00", "30.000", "Garrafa de óleo alimentar de 1 litro", ivaNormal, ptCompany, mzCompany);
        Product partsProduct = createProduct("MER-FARINHA-1KG", "MRC-0004", "5601000000048", "Farinha de Trigo 1kg", "80.00", "58.00", "35.000", "Pacote de farinha de trigo de 1kg", ivaExempt, ptCompany, mzCompany);
        Product feijao = createProduct("MER-FEIJAO-1KG", "MRC-0005", "5601000000055", "Feijão Manteiga 1kg", "180.00", "140.00", "25.000", "Pacote de feijão manteiga de 1kg", ivaExempt, ptCompany, mzCompany);
        Product massa = createProduct("MER-MASSA-500G", "MRC-0006", "5601000000062", "Massa Esparguete 500g", "70.00", "48.00", "50.000", "Pacote de massa esparguete de 500g", ivaReduced, ptCompany, mzCompany);

        seedWarehouseStocks(whLisboa, erpLic, support, "120.000", techServ, partsProduct, feijao, massa);
        seedWarehouseStocks(whMaputo, erpLic, support, "95.000", techServ, partsProduct, feijao, massa);

        createTreasuryAccount("Caixa Geral de Depósitos - Conta à Ordem", "PT50 0035 0123 4567 8901 23", "18500.00", ptCompany);
        createTreasuryAccount("Millennium BCP - Investimentos", "PT50 0033 0987 6543 2101 24", "45000.00", mzCompany);

        CurrentUserContext.runAsSystem(ptCompany.getId(), () -> seedTicketsAndExpenses(padaria, techSol, maria, joao));
    }

    private Company createCompany(String name, String taxId, String email, String address) {
        Company c = new Company();
        c.setName(name);
        c.setTaxId(taxId);
        c.setEmail(email);
        c.setAddress(address);
        return companyService.saveCompany(c);
    }

    private AppUser createAppUser(String username, String name, String role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setName(name);
        u.setPassword(passwordEncoder.encode("password"));
        u.setRole(role);
        u.setActive(true);
        return userCommands.saveUser(u);
    }

    private Warehouse createWarehouse(String name, String location, Company company) {
        Warehouse w = new Warehouse();
        w.setName(name);
        w.setLocation(location);
        w.setCompany(company);
        return warehouseCommandService.saveWarehouse(w);
    }

    private void createSupplier(String name, String taxId, String email, String address, Company company) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setTaxId(taxId);
        s.setEmail(email);
        s.setAddress(address);
        s.setCompany(company);
        supplierCommandService.saveSupplier(s);
    }

    private Employee createEmployee(Company company, String number, String name, String email,
                                    String department, String salary, String role, LocalDate hireDate, AppUser appUser) {
        Employee e = new Employee();
        e.setCompany(company);
        e.setEmployeeNumber(number);
        e.setName(name);
        e.setEmail(email);
        e.setDepartment(department);
        e.setBaseSalary(new BigDecimal(salary));
        e.setRole(role);
        e.setHireDate(hireDate);
        e.setCreatedBy("SYSTEM");
        e.setAppUser(appUser);
        return employeeCommandService.saveEmployee(e);
    }

    private Client createClient(String name, String taxId, String email, String address, Company company) {
        Client c = new Client();
        c.setName(name);
        c.setTaxId(taxId);
        c.setEmail(email);
        c.setAddress(address);
        c.getCompanies().add(company);
        return clientCommandService.saveClient(c);
    }

    private Product createProduct(String sku, String reference, String barcode, String name,
                                  String unitPrice, String purchasePrice, String minStock, String desc,
                                  TaxRate taxRate, Company... companies) {
        Product p = new Product();
        p.setSku(sku);
        p.setReference(reference);
        p.setBarcode(barcode);
        p.setName(name);
        p.setUnitPrice(new BigDecimal(unitPrice));
        p.setPurchasePrice(new BigDecimal(purchasePrice));
        p.setMinStock(new BigDecimal(minStock));
        p.setDescription(desc);
        p.setTaxRate(taxRate);
        p.getCompanies().addAll(List.of(companies));
        return productCommandService.saveProduct(p);
    }

    private void seedWarehouseStocks(Warehouse wh, Product erpLic, Product support, String supportQty,
                                     Product techServ, Product partsProduct, Product feijao, Product massa) {
        seedStock(erpLic, wh, "85.000");
        seedStock(support, wh, supportQty);
        seedStock(techServ, wh, "45.000");
        seedStock(partsProduct, wh, "70.000");
        seedStock(feijao, wh, "30.000");
        seedStock(massa, wh, "160.000");
    }

    /** Saldo inicial pelo ledger do inventário: grava movimento, lote e saldo juntos. */
    private void seedStock(Product product, Warehouse warehouse, String quantity) {
        CurrentUserContext.runAsSystem(warehouse.getCompany().getId(), () ->
                stockMovementService.registerMovement(product, warehouse, new BigDecimal(quantity),
                        "ENTRY", null, null, "Stock inicial (demonstração)"));
    }

    private void createTreasuryAccount(String name, String accountNumber, String balance, Company company) {
        TreasuryAccount a = new TreasuryAccount();
        a.setName(name);
        a.setAccountNumber(accountNumber);
        a.setAccountType(TreasuryAccountType.BANK);
        a.setBalance(new BigDecimal(balance));
        a.setCompany(company);
        accountRepository.save(a);
    }

    private void seedTicketsAndExpenses(Client padaria, Client techSol, Employee maria, Employee joao) {
        SupportTicketDTO ticket1 = crmService.createTicket(new CreateTicketRequest(
                padaria.getId(), "Instalação do terminal de faturas falhou",
                "Ao tentar iniciar a aplicação de vendas, o terminal de faturas indica erro de ligação com a impressora térmica."));
        crmService.createWorkSheet(new CreateWorkSheetRequest(
                ticket1.id(), "João Silva", new BigDecimal("2.5"),
                "Deslocação ao local, reconfiguração das portas COM no Windows e reinstalação dos drivers da impressora térmica. Teste efetuado com sucesso.",
                "Cabo RS232 Substituído", new BigDecimal("15.00")));
        crmService.createTicket(new CreateTicketRequest(
                techSol.getId(), "Migração de Base de Dados para Nuvem",
                "Pretende-se migrar as tabelas de faturação históricas para a nuvem Multicore."));

        submitExpenseAs("maria", "EMPLOYEE", new CreateExpenseClaimRequest(maria.getId(), new BigDecimal("35.50"), "MEALS", "Almoço com cliente de Braga"));
        submitExpenseAs("maria", "EMPLOYEE", new CreateExpenseClaimRequest(maria.getId(), new BigDecimal("180.00"), "TRAVEL", "Deslocação em trabalho a Braga (combustível e portagens)"));
        submitExpenseAs("joao", "MANAGER", new CreateExpenseClaimRequest(joao.getId(), new BigDecimal("750.00"), "LODGING", "Estadia de 4 dias no Hotel Ritz para conferência anual de parceiros"));
    }

    private void submitExpenseAs(String username, String role, CreateExpenseClaimRequest request) {
        CurrentUserContext.UserSession seeder = CurrentUserContext.findCurrentUser();
        CurrentUserContext.setCurrentUser(username, role);
        try {
            hrService.submitExpense(request);
        } finally {
            if (seeder != null) CurrentUserContext.setCurrentUser(seeder.username(), seeder.role());
        }
    }
}
