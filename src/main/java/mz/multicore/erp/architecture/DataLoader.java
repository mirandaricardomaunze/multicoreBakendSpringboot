package mz.multicore.erp.architecture;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.service.CRMService;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.service.HRService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final TreasuryAccountRepository accountRepository;
    private final HRService hrService;
    private final CRMService crmService;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final AppUserRepository appUserRepository;
    private final SupplierRepository supplierRepository;
    private final TaxRateRepository taxRateRepository;
    private final ProductCategoryRepository productCategoryRepository;
    /** Dados de demonstração (empresas/utilizadores/produtos fictícios). Falso em produção. */
    private final boolean seedDemoData;
    /** Senha inicial do superadmin. Vazia (ex.: prod sem env) ⇒ conta não é criada automaticamente. */
    private final String superAdminPassword;

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
            @Value("${app.seed-demo-data:true}") boolean seedDemoData,
            @Value("${app.superadmin.password:superadmin}") String superAdminPassword
    ) {
        this.employeeRepository = employeeRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
        this.hrService = hrService;
        this.crmService = crmService;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.appUserRepository = appUserRepository;
        this.supplierRepository = supplierRepository;
        this.taxRateRepository = taxRateRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.seedDemoData = seedDemoData;
        this.superAdminPassword = superAdminPassword;
    }

    /**
     * Conta superadmin (dono da plataforma), sem empresa. Idempotente por username — corre em
     * qualquer BD, mesmo com dados de demo já semeados. Trocar a senha em produção.
     */
    private void seedSuperAdmin() {
        if (appUserRepository.findByUsername("superadmin").isPresent()) return;
        // Sem senha configurada (ex.: produção sem SUPERADMIN_PASSWORD) NÃO se cria uma conta
        // superadmin com senha default — teria de ser criada deliberadamente.
        if (superAdminPassword == null || superAdminPassword.isBlank()) return;
        AppUser superAdmin = new AppUser();
        superAdmin.setUsername("superadmin");
        superAdmin.setName("Administrador da Plataforma");
        superAdmin.setPassword(superAdminPassword);
        superAdmin.setRole("ADMIN");
        superAdmin.setActive(true);
        superAdmin.setPlatformAdmin(true);
        superAdmin.setCreatedBy("SYSTEM");
        appUserRepository.save(superAdmin);
    }

    private void seedProductCategories() {
        if (productCategoryRepository.count() > 0) return;
        seedCategory("ALIMENT", "Alimentação", "#F59E0B");
        seedCategory("BEBIDAS", "Bebidas", "#3B82F6");
        seedCategory("HIGIENE", "Higiene Pessoal", "#10B981");
        seedCategory("LIMPEZA", "Limpeza Doméstica", "#8B5CF6");
        seedCategory("PADARIA", "Padaria & Pastelaria", "#EC4899");
        seedCategory("MERCEAR", "Mercearia Geral", "#6B7280");
        seedCategory("OUTROS",  "Outros", "#9CA3AF");
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

    private void seedMozambicanTaxRates() {
        if (taxRateRepository.count() > 0) return;
        seedTaxRate("IVA16",   "IVA Normal (16%)",   TaxType.IVA_STANDARD,    "0.16", "Lei do IVA — DL 8/2024");
        seedTaxRate("IVA5",    "IVA Reduzido (5%)",  TaxType.IVA_REDUCED,     "0.05", "Lei do IVA — alimentos, livros, transportes");
        seedTaxRate("IVA0",    "IVA Zero (0%)",      TaxType.IVA_ZERO,        "0.00", "Lei do IVA — exportações");
        seedTaxRate("IVAEXMT", "IVA Isento",         TaxType.IVA_EXEMPT,      "0.00", "Lei do IVA — saúde, educação, finanças");
        seedTaxRate("RF_SERV", "Retenção na Fonte — Serviços (10%)", TaxType.WITHHOLDING, "0.10", "Código do IRPS / IRPC");
        seedTaxRate("RF_REND", "Retenção na Fonte — Rendas (14%)",   TaxType.WITHHOLDING, "0.14", "Código do IRPS");
        seedTaxRate("RF_NRES", "Retenção na Fonte — Não-residentes (20%)", TaxType.WITHHOLDING, "0.20", "Código do IRPC");
        seedTaxRate("IRPC32",  "IRPC — Pessoas Colectivas (32%)",    TaxType.CORPORATE_INCOME, "0.32", "Código do IRPC");
        seedTaxRate("ICE_ALC", "ICE — Bebidas alcoólicas",           TaxType.EXCISE, "0.55", "Pauta Aduaneira / ICE");
        seedTaxRate("ICE_TAB", "ICE — Tabaco",                       TaxType.EXCISE, "0.75", "Pauta Aduaneira / ICE");
    }

    private void seedTaxRate(String code, String name, TaxType type, String rate, String legalBasis) {
        TaxRate t = new TaxRate();
        t.setCode(code);
        t.setName(name);
        t.setType(type);
        t.setRate(new java.math.BigDecimal(rate));
        t.setLegalBasis(legalBasis);
        t.setActive(true);
        t.setCreatedBy("SYSTEM");
        taxRateRepository.save(t);
    }

    private Warehouse whLisboa;
    private Warehouse whMaputo;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) {
        seedMozambicanTaxRates();
        seedProductCategories();
        seedSuperAdmin();
        // Dados de DEMONSTRAÇÃO (empresas/utilizadores/produtos fictícios) NÃO entram em produção
        // (app.seed-demo-data=false no perfil prod). Taxas de IVA e categorias acima são referência.
        if (!seedDemoData) {
            return;
        }
        // Guarda de idempotência: numa BD persistente (PostgreSQL) o seed de demo só corre uma vez.
        // Sem isto, cada arranque tentava recriar empresas/utilizadores e falhava na chave única de username.
        if (companyRepository.count() > 0) {
            return;
        }
        // 0. Seed Companies
        Company ptCompany = new Company();
        ptCompany.setName("Multicore Portugal Lda");
        ptCompany.setTaxId("501982736");
        ptCompany.setEmail("contacto@multicore.pt");
        ptCompany.setAddress("Avenida da Liberdade 100, Lisboa");
        ptCompany = companyRepository.save(ptCompany);

        Company mzCompany = new Company();
        mzCompany.setName("Multicore Moçambique Lda");
        mzCompany.setTaxId("400123456");
        mzCompany.setEmail("contacto@multicore.co.mz");
        mzCompany.setAddress("Avenida 24 de Julho 1500, Maputo");
        mzCompany = companyRepository.save(mzCompany);

        Company seededPtCompany = ptCompany;
        Company seededMzCompany = mzCompany;
        productCategoryRepository.findAll().forEach(category -> {
            category.getCompanies().add(seededPtCompany);
            category.getCompanies().add(seededMzCompany);
            productCategoryRepository.save(category);
        });
        taxRateRepository.findAll().forEach(rate -> {
            rate.getCompanies().add(seededPtCompany);
            rate.getCompanies().add(seededMzCompany);
            taxRateRepository.save(rate);
        });

        // Seed AppUsers matching the employees and matching roles
        AppUser mariaUser = new AppUser();
        mariaUser.setUsername("maria");
        mariaUser.setName("Maria Santos");
        mariaUser.setPassword("password");
        mariaUser.setRole("EMPLOYEE");
        mariaUser.setActive(true);
        appUserRepository.save(mariaUser);

        AppUser joaoUser = new AppUser();
        joaoUser.setUsername("joao");
        joaoUser.setName("João Silva");
        joaoUser.setPassword("password");
        joaoUser.setRole("MANAGER");
        joaoUser.setActive(true);
        appUserRepository.save(joaoUser);

        AppUser anaUser = new AppUser();
        anaUser.setUsername("ana");
        anaUser.setName("Ana Costa");
        anaUser.setPassword("password");
        anaUser.setRole("ADMIN");
        anaUser.setActive(true);
        appUserRepository.save(anaUser);

        mariaUser.grantCompany(ptCompany, "EMPLOYEE");
        joaoUser.grantCompany(ptCompany, "MANAGER");
        anaUser.grantCompany(ptCompany, "ADMIN");
        anaUser.grantCompany(mzCompany, "ADMIN");
        appUserRepository.save(mariaUser);
        appUserRepository.save(joaoUser);
        appUserRepository.save(anaUser);

        // Seed Warehouses
        whLisboa = new Warehouse();
        whLisboa.setName("Armazém Lisboa Central");
        whLisboa.setLocation("Lisboa");
        whLisboa.setCompany(ptCompany);
        whLisboa = warehouseRepository.save(whLisboa);

        Warehouse whPorto = new Warehouse();
        whPorto.setName("Armazém Porto Norte");
        whPorto.setLocation("Porto");
        whPorto.setCompany(ptCompany);
        warehouseRepository.save(whPorto);

        whMaputo = new Warehouse();
        whMaputo.setName("Armazém Maputo Porto");
        whMaputo.setLocation("Maputo");
        whMaputo.setCompany(mzCompany);
        whMaputo = warehouseRepository.save(whMaputo);

        Warehouse whBeira = new Warehouse();
        whBeira.setName("Armazém Beira Centro");
        whBeira.setLocation("Beira");
        whBeira.setCompany(mzCompany);
        warehouseRepository.save(whBeira);

        // Seed Suppliers
        Supplier supPT = new Supplier();
        supPT.setName("Distribuidor Software Portugal Lda");
        supPT.setTaxId("509182736");
        supPT.setEmail("contacto@distribuidores.pt");
        supPT.setAddress("Zona Industrial, Porto");
        supPT.setCompany(ptCompany);
        supplierRepository.save(supPT);

        Supplier supMZ = new Supplier();
        supMZ.setName("Importadora Tecnológica Maputo");
        supMZ.setTaxId("400987654");
        supMZ.setEmail("contacto@importadortec.co.mz");
        supMZ.setAddress("Avenida de Angola, Maputo");
        supMZ.setCompany(mzCompany);
        supplierRepository.save(supMZ);

        // 1. Seed Employees
        Employee maria = new Employee();
        maria.setCompany(ptCompany);
        maria.setEmployeeNumber("PT-001");
        maria.setName("Maria Santos");
        maria.setEmail("maria.santos@multicore.pt");
        maria.setDepartment("Recursos Humanos");
        maria.setBaseSalary(new BigDecimal("1250.00"));
        maria.setRole("EMPLOYEE");
        maria.setHireDate(java.time.LocalDate.of(2022, 1, 10));
        maria.setCreatedBy("SYSTEM");
        employeeRepository.save(maria);

        Employee joao = new Employee();
        joao.setCompany(ptCompany);
        joao.setEmployeeNumber("PT-002");
        joao.setName("João Silva");
        joao.setEmail("joao.silva@multicore.pt");
        joao.setDepartment("Comercial");
        joao.setBaseSalary(new BigDecimal("2100.00"));
        joao.setRole("MANAGER");
        joao.setHireDate(java.time.LocalDate.of(2021, 5, 3));
        joao.setCreatedBy("SYSTEM");
        employeeRepository.save(joao);

        Employee ana = new Employee();
        ana.setCompany(mzCompany);
        ana.setEmployeeNumber("MZ-001");
        ana.setName("Ana Costa");
        ana.setEmail("ana.costa@multicore.pt");
        ana.setDepartment("Administração");
        ana.setBaseSalary(new BigDecimal("3800.00"));
        ana.setRole("ADMIN");
        ana.setHireDate(java.time.LocalDate.of(2020, 2, 17));
        ana.setCreatedBy("SYSTEM");
        employeeRepository.save(ana);

        // 2. Seed Clients
        Client techSol = new Client();
        techSol.setName("Tech Solutions Lda");
        techSol.setTaxId("501234567");
        techSol.setEmail("financeiro@techsolutions.pt");
        techSol.setAddress("Avenida da Boavista 1200, Porto");
        techSol.getCompanies().add(ptCompany);
        clientRepository.save(techSol);

        Client padaria = new Client();
        padaria.setName("Padaria Central do Minho");
        padaria.setTaxId("509876543");
        padaria.setEmail("geral@padariaminho.pt");
        padaria.setAddress("Rua Principal 45, Braga");
        padaria.getCompanies().add(ptCompany);
        clientRepository.save(padaria);

        // 3. Seed Products
        Product erpLic = new Product();
        erpLic.setSku("MER-ARROZ-5KG");
        erpLic.setReference("MRC-0001");
        erpLic.setBarcode("5601000000017");
        erpLic.setName("Arroz Agulha 5kg");
        erpLic.setUnitPrice(new BigDecimal("550.00"));
        erpLic.setPurchasePrice(new BigDecimal("430.00"));
        erpLic.setMinStock(new BigDecimal("20.000"));
        erpLic.setDescription("Saco de arroz agulha de 5kg");
        shareProduct(erpLic, ptCompany, mzCompany);
        productRepository.save(erpLic);

        Product support = new Product();
        support.setSku("MER-ACUCAR-1KG");
        support.setReference("MRC-0002");
        support.setBarcode("5601000000024");
        support.setName("Acucar Branco 1kg");
        support.setUnitPrice(new BigDecimal("95.00"));
        support.setPurchasePrice(new BigDecimal("72.00"));
        support.setMinStock(new BigDecimal("40.000"));
        support.setDescription("Pacote de acucar branco refinado de 1kg");
        shareProduct(support, ptCompany, mzCompany);
        productRepository.save(support);

        Product techServ = new Product();
        techServ.setSku("MER-OLEO-1L");
        techServ.setReference("MRC-0003");
        techServ.setBarcode("5601000000031");
        techServ.setName("Oleo Alimentar 1L");
        techServ.setUnitPrice(new BigDecimal("165.00"));
        techServ.setPurchasePrice(new BigDecimal("130.00"));
        techServ.setMinStock(new BigDecimal("30.000"));
        techServ.setDescription("Garrafa de oleo alimentar de 1 litro");
        shareProduct(techServ, ptCompany, mzCompany);
        productRepository.save(techServ);

        Product partsProduct = new Product();
        partsProduct.setSku("MER-FARINHA-1KG");
        partsProduct.setReference("MRC-0004");
        partsProduct.setBarcode("5601000000048");
        partsProduct.setName("Farinha de Trigo 1kg");
        partsProduct.setUnitPrice(new BigDecimal("80.00"));
        partsProduct.setPurchasePrice(new BigDecimal("58.00"));
        partsProduct.setMinStock(new BigDecimal("35.000"));
        partsProduct.setDescription("Pacote de farinha de trigo de 1kg");
        shareProduct(partsProduct, ptCompany, mzCompany);
        productRepository.save(partsProduct);

        Product feijao = new Product();
        feijao.setSku("MER-FEIJAO-1KG");
        feijao.setReference("MRC-0005");
        feijao.setBarcode("5601000000055");
        feijao.setName("Feijao Manteiga 1kg");
        feijao.setUnitPrice(new BigDecimal("180.00"));
        feijao.setPurchasePrice(new BigDecimal("140.00"));
        feijao.setMinStock(new BigDecimal("25.000"));
        feijao.setDescription("Pacote de feijao manteiga de 1kg");
        shareProduct(feijao, ptCompany, mzCompany);
        productRepository.save(feijao);

        Product massa = new Product();
        massa.setSku("MER-MASSA-500G");
        massa.setReference("MRC-0006");
        massa.setBarcode("5601000000062");
        massa.setName("Massa Esparguete 500g");
        massa.setUnitPrice(new BigDecimal("70.00"));
        massa.setPurchasePrice(new BigDecimal("48.00"));
        massa.setMinStock(new BigDecimal("50.000"));
        massa.setDescription("Pacote de massa esparguete de 500g");
        shareProduct(massa, ptCompany, mzCompany);
        productRepository.save(massa);

        // IVA dinâmico — taxas variadas por produto (realidade MZ: cesta básica isenta).
        TaxRate ivaExempt = taxRateRepository.findByCode("IVAEXMT").orElse(null);
        TaxRate ivaReduced = taxRateRepository.findByCode("IVA5").orElse(null);
        TaxRate ivaNormal = taxRateRepository.findByCode("IVA16").orElse(null);
        erpLic.setTaxRate(ivaExempt);        productRepository.save(erpLic);        // Arroz — isento
        support.setTaxRate(ivaExempt);       productRepository.save(support);       // Açúcar — isento
        partsProduct.setTaxRate(ivaExempt);  productRepository.save(partsProduct);  // Farinha — isento
        feijao.setTaxRate(ivaExempt);        productRepository.save(feijao);        // Feijão — isento
        massa.setTaxRate(ivaReduced);        productRepository.save(massa);         // Massa — IVA 5%
        techServ.setTaxRate(ivaNormal);      productRepository.save(techServ);      // Óleo — IVA 16%


        // 3.1 Seed Stocks
        Stock stLisboa1 = new Stock();
        stLisboa1.setProduct(erpLic);
        stLisboa1.setWarehouse(whLisboa);
        stLisboa1.setQuantity(new BigDecimal("85.000"));
        stockRepository.save(stLisboa1);

        Stock stLisboa2 = new Stock();
        stLisboa2.setProduct(support);
        stLisboa2.setWarehouse(whLisboa);
        stLisboa2.setQuantity(new BigDecimal("120.000"));
        stockRepository.save(stLisboa2);

        Stock stLisboa3 = new Stock();
        stLisboa3.setProduct(techServ);
        stLisboa3.setWarehouse(whLisboa);
        stLisboa3.setQuantity(new BigDecimal("45.000"));
        stockRepository.save(stLisboa3);

        Stock stMaputo1 = new Stock();
        stMaputo1.setProduct(partsProduct);
        stMaputo1.setWarehouse(whMaputo);
        stMaputo1.setQuantity(new BigDecimal("70.000"));
        stockRepository.save(stMaputo1);

        Stock stMaputo2 = new Stock();
        stMaputo2.setProduct(support);
        stMaputo2.setWarehouse(whMaputo);
        stMaputo2.setQuantity(new BigDecimal("95.000"));
        stockRepository.save(stMaputo2);

        Stock stMaputo3 = new Stock();
        stMaputo3.setProduct(feijao);
        stMaputo3.setWarehouse(whMaputo);
        stMaputo3.setQuantity(new BigDecimal("30.000"));
        stockRepository.save(stMaputo3);

        Stock stMaputo4 = new Stock();
        stMaputo4.setProduct(massa);
        stMaputo4.setWarehouse(whMaputo);
        stMaputo4.setQuantity(new BigDecimal("160.000"));
        stockRepository.save(stMaputo4);

        // 4. Seed Treasury Accounts
        TreasuryAccount cgd = new TreasuryAccount();
        cgd.setName("Caixa Geral de Depósitos - Conta à Ordem");
        cgd.setAccountNumber("PT50 0035 0123 4567 8901 23");
        cgd.setBalance(new BigDecimal("18500.00"));
        cgd.setCompany(ptCompany);
        accountRepository.save(cgd);

        TreasuryAccount bcp = new TreasuryAccount();
        bcp.setName("Millennium BCP - Investimentos");
        bcp.setAccountNumber("PT50 0033 0987 6543 2101 24");
        bcp.setBalance(new BigDecimal("45000.00"));
        bcp.setCompany(mzCompany);
        accountRepository.save(bcp);

        // 5 e 6. Tickets/folhas de obra e despesas passam pelos Services, que resolvem a empresa pelo
        // CurrentUserContext. O povoamento não é um pedido HTTP, logo não há contexto — declara-se aqui,
        // ligado à empresa a que estes dados pertencem. Antes dependia do contexto assumir a empresa 1
        // por omissão, o que só calhava certo porque a ptCompany é a primeira a ser gravada.
        CurrentUserContext.runAsSystem(ptCompany.getId(), () -> seedTicketsAndExpenses(padaria, techSol, maria, joao));
    }

    private void seedTicketsAndExpenses(Client padaria, Client techSol, Employee maria, Employee joao) {
        // 5. Seed Support Tickets & WorkSheets
        SupportTicketDTO ticket1 = crmService.createTicket(new CreateTicketRequest(
                padaria.getId(),
                "Instalação do terminal de faturas falhou",
                "Ao tentar iniciar a aplicação de vendas, o terminal de faturas indica erro de ligação com a impressora térmica."
        ));

        // Register a Job Sheet for the ticket
        crmService.createWorkSheet(new CreateWorkSheetRequest(
                ticket1.id(),
                "João Silva",
                new BigDecimal("2.5"), // 2.5 hours worked
                "Deslocação ao local, reconfiguração das portas COM no Windows e reinstalação dos drivers da impressora térmica. Teste efetuado com sucesso.",
                "Cabo RS232 Substituído",
                new BigDecimal("15.00") // 15 MT parts cost
        ));

        SupportTicketDTO ticket2 = crmService.createTicket(new CreateTicketRequest(
                techSol.getId(),
                "Migração de Base de Dados para Nuvem",
                "Pretende-se migrar as tabelas de faturação históricas para a nuvem Multicore."
        ));

        // 6. Seed Expense Claims (triggers approvals)
        // Under 50.00: Auto-approved
        hrService.submitExpense(new CreateExpenseClaimRequest(
                maria.getId(),
                new BigDecimal("35.50"),
                "MEALS",
                "Almoço com cliente de Braga"
        ));

        // Between 50 and 500: Requires MANAGER approval
        hrService.submitExpense(new CreateExpenseClaimRequest(
                maria.getId(),
                new BigDecimal("180.00"),
                "TRAVEL",
                "Deslocação em trabalho a Braga (combustível e portagens)"
        ));

        // Above 500: Requires ADMIN approval
        hrService.submitExpense(new CreateExpenseClaimRequest(
                joao.getId(),
                new BigDecimal("750.00"),
                "LODGING",
                "Estadia de 4 dias no Hotel Ritz para conferência anual de parceiros"
        ));
    }

    private void shareProduct(Product product, Company... companies) {
        product.getCompanies().addAll(java.util.List.of(companies));
    }
}
