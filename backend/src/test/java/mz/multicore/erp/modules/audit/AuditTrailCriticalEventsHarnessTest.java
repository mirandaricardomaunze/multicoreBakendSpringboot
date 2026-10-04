package mz.multicore.erp.modules.audit;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.ManagerPinRateLimiter;
import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.ApproveWasteRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Harness canónico para SPEC-AUD-001 (HARNESS-AUD-001):
 * Trilha de Auditoria Centralizada e Imutável de Eventos Críticos.
 */
class AuditTrailCriticalEventsHarnessTest {

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("AUD-01: Registo de quebra de stock gera auditoria com produto, armazém e custo")
    void testAud01_StockWasteRegisterAudit() {
        StockWasteRepository wasteRepo = mock(StockWasteRepository.class);
        mz.multicore.erp.modules.comercial.repository.ProductRepository prodRepo = mock(mz.multicore.erp.modules.comercial.repository.ProductRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductBatchRepository batchRepo = mock(ProductBatchRepository.class);
        InventoryService invService = mock(InventoryService.class);
        mz.multicore.erp.modules.comercial.repository.InvoiceRepository invRepo = mock(mz.multicore.erp.modules.comercial.repository.InvoiceRepository.class);
        CompanyRepository compRepo = mock(CompanyRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        Company company = new Company();
        company.setId(COMPANY_ID);
        when(compRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        mz.multicore.erp.modules.inventory.model.Warehouse wh = new mz.multicore.erp.modules.inventory.model.Warehouse();
        wh.setId(10L);
        wh.setName("Armazém Central");
        when(warehouseRepo.findById(10L)).thenReturn(Optional.of(wh));

        mz.multicore.erp.modules.comercial.model.Product prod = new mz.multicore.erp.modules.comercial.model.Product();
        prod.setId(20L);
        prod.setSku("PRD-AUD-01");
        prod.setName("Produto Auditoria");
        prod.setPurchasePrice(new BigDecimal("150.00"));
        when(prodRepo.findById(20L)).thenReturn(Optional.of(prod));

        when(wasteRepo.save(any(StockWaste.class))).thenAnswer(invocation -> {
            StockWaste w = invocation.getArgument(0);
            w.setId(99L);
            return w;
        });

        StockWasteService wasteService = new StockWasteService(
                wasteRepo, prodRepo, warehouseRepo, batchRepo, invService, invRepo, compRepo, auditLogService
        );

        CreateStockWasteRequest req = new CreateStockWasteRequest(
                COMPANY_ID, 10L, 20L, null, new BigDecimal("5"), WasteReason.DAMAGED, "Quebra no transporte"
        );

        wasteService.registerWaste(req);

        verify(auditLogService, times(1)).logEvent(
                eq("admin"),
                eq(COMPANY_ID),
                eq("STOCK_WASTE_REGISTER"),
                contains("PRD-AUD-01")
        );
    }

    @Test
    @DisplayName("AUD-02: Aprovação e Rejeição de quebras de stock geram auditoria formal")
    void testAud02_StockWasteApproveAndRejectAudit() {
        StockWasteRepository wasteRepo = mock(StockWasteRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        StockWaste waste = new StockWaste();
        waste.setId(55L);
        waste.setStatus(WasteStatus.PENDING_APPROVAL);
        Company c = new Company();
        c.setId(COMPANY_ID);
        waste.setCompany(c);
        mz.multicore.erp.modules.comercial.model.Product p = new mz.multicore.erp.modules.comercial.model.Product();
        p.setId(12L);
        p.setSku("SKU-WASTE-55");
        waste.setProduct(p);
        waste.setQuantity(new BigDecimal("10"));
        waste.setTotalCost(new BigDecimal("5000.00"));
        waste.setReason(WasteReason.EXPIRED);

        when(wasteRepo.findById(55L)).thenReturn(Optional.of(waste));
        when(wasteRepo.save(any(StockWaste.class))).thenAnswer(i -> i.getArgument(0));

        StockWasteService wasteService = new StockWasteService(
                wasteRepo, mock(mz.multicore.erp.modules.comercial.repository.ProductRepository.class),
                mock(WarehouseRepository.class), mock(ProductBatchRepository.class),
                mock(InventoryService.class), mock(mz.multicore.erp.modules.comercial.repository.InvoiceRepository.class),
                mock(CompanyRepository.class), auditLogService
        );

        // Aprovação
        wasteService.approveWaste(55L, new ApproveWasteRequest(true, "Aprovado em conferência"));
        verify(auditLogService, times(1)).logEvent(
                eq("admin"),
                eq(COMPANY_ID),
                eq("STOCK_WASTE_APPROVE"),
                contains("Quebra de stock #55")
        );

        // Rejeição
        waste.setStatus(WasteStatus.PENDING_APPROVAL);
        wasteService.approveWaste(55L, new ApproveWasteRequest(false, "Quantidade inconsistente"));
        verify(auditLogService, times(1)).logEvent(
                eq("admin"),
                eq(COMPANY_ID),
                eq("STOCK_WASTE_REJECT"),
                contains("Quebra de stock #55")
        );
    }

    @Test
    @DisplayName("AUD-03: Alteração de limite de crédito de cliente gera registo CLIENT_CREDIT_LIMIT_CHANGE")
    void testAud03_ClientCreditLimitChangeAudit() {
        ClientRepository clientRepo = mock(ClientRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        Client client = new Client();
        client.setId(101L);
        client.setName("Cliente VIP Lda");
        client.setTaxId("123456789");
        client.setCreditLimit(new BigDecimal("10000.00"));
        client.setPaymentTermsDays(30);

        when(clientRepo.findByIdAndCompaniesId(101L, COMPANY_ID)).thenReturn(Optional.of(client));
        when(clientRepo.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        ComercialService comercialService = new ComercialService(
                clientRepo,
                mock(mz.multicore.erp.modules.comercial.repository.ProductRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.InvoiceRepository.class),
                null,
                mock(CompanyRepository.class),
                mock(WarehouseRepository.class),
                null,
                mock(mz.multicore.erp.modules.comercial.repository.ReceiptRepository.class),
                null,
                mock(mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.OrderRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.OrderLineRepository.class),
                mock(mz.multicore.erp.modules.comercial.service.WalkInClientProvider.class),
                mock(mz.multicore.erp.modules.numbering.service.DocumentNumberService.class),
                auditLogService,
                mock(mz.multicore.erp.modules.fiscal.repository.TaxRateRepository.class),
                mock(mz.multicore.erp.modules.comercial.service.ReceivablesService.class),
                mock(org.springframework.context.ApplicationEventPublisher.class)
        );

        SaveClientRequest updateReq = new SaveClientRequest(
                "Cliente VIP Lda",
                "123456789",
                "vip@cliente.mz",
                "Maputo",
                30,
                new BigDecimal("25000.00")
        );

        comercialService.updateClient(101L, updateReq);

        verify(auditLogService, times(1)).logCurrent(
                eq("CLIENT_CREDIT_LIMIT_CHANGE"),
                contains("alterado de 10000.00 para 25000.00 MZN")
        );
    }

    @Test
    @DisplayName("AUD-04: Mutações de segurança e privilégios de utilizadores são auditadas")
    void testAud04_UserSecurityAndPrivilegesAudit() {
        AppUserRepository userRepo = mock(AppUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        CompanyRepository compRepo = mock(CompanyRepository.class);
        AppUserCompanyAccessRepository accessRepo = mock(AppUserCompanyAccessRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        when(encoder.encode(anyString())).thenReturn("hashed-secret");

        Company company = new Company();
        company.setId(COMPANY_ID);
        when(compRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        AppUser user = new AppUser();
        user.setId(77L);
        user.setUsername("operador1");
        user.setName("Operador Teste");
        user.setActive(true);

        AppUserCompanyAccess access = new AppUserCompanyAccess();
        access.setCompany(company);
        access.setRole("CASHIER");
        user.getCompanyAccesses().add(access);

        when(userRepo.findByUsername("operador1")).thenReturn(Optional.of(user));
        when(userRepo.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUserService userService = new AppUserService(
                userRepo, encoder, compRepo, accessRepo,
                new ManagerPinRateLimiter(3, 5), auditLogService
        );

        // 1. Alteração de Role
        userService.updateCompanyRole("operador1", "MANAGER");
        verify(auditLogService, times(1)).logCurrent(
                eq("USER_ROLE_CHANGE"),
                contains("operador1")
        );

        // 2. Definição de PIN de Gestor
        userService.setManagerPin("operador1", "1234");
        verify(auditLogService, times(1)).logCurrent(
                eq("USER_PIN_SET"),
                contains("operador1")
        );

        // 3. Reset de Password
        userService.resetPassword("operador1", "NovaSenha2026");
        verify(auditLogService, times(1)).logCurrent(
                eq("USER_PASSWORD_RESET"),
                contains("operador1")
        );

        // 4. Toggle de Status
        userService.toggleUserStatus("operador1", false);
        verify(auditLogService, times(1)).logCurrent(
                eq("USER_STATUS_CHANGE"),
                contains("INATIVO")
        );
    }

    @Test
    @DisplayName("AUD-05: Anulação de documentos fiscais e comerciais grava auditoria")
    void testAud05_DocumentCancellationAudit() {
        ClientRepository clientRepo = mock(ClientRepository.class);
        mz.multicore.erp.modules.comercial.repository.InvoiceRepository invRepo = mock(mz.multicore.erp.modules.comercial.repository.InvoiceRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        mz.multicore.erp.modules.comercial.model.Invoice inv = new mz.multicore.erp.modules.comercial.model.Invoice();
        inv.setId(200L);
        inv.setInvoiceNumber("FT-2026/001");
        inv.setStatus(mz.multicore.erp.modules.comercial.model.InvoiceStatus.APPROVED);
        Company company = new Company();
        company.setId(COMPANY_ID);
        inv.setCompany(company);

        when(invRepo.findById(200L)).thenReturn(Optional.of(inv));
        when(invRepo.save(any(mz.multicore.erp.modules.comercial.model.Invoice.class))).thenAnswer(i -> i.getArgument(0));

        ComercialService comercialService = new ComercialService(
                clientRepo,
                mock(mz.multicore.erp.modules.comercial.repository.ProductRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository.class),
                invRepo,
                null,
                mock(CompanyRepository.class),
                mock(WarehouseRepository.class),
                null,
                mock(mz.multicore.erp.modules.comercial.repository.ReceiptRepository.class),
                null,
                mock(mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.OrderRepository.class),
                mock(mz.multicore.erp.modules.comercial.repository.OrderLineRepository.class),
                mock(mz.multicore.erp.modules.comercial.service.WalkInClientProvider.class),
                mock(mz.multicore.erp.modules.numbering.service.DocumentNumberService.class),
                auditLogService,
                mock(mz.multicore.erp.modules.fiscal.repository.TaxRateRepository.class),
                mock(mz.multicore.erp.modules.comercial.service.ReceivablesService.class),
                mock(org.springframework.context.ApplicationEventPublisher.class)
        );

        comercialService.cancelInvoice(200L, "Erro no NUIT do cliente");

        verify(auditLogService, times(1)).logCurrent(
                eq("INVOICE_CANCEL"),
                contains("FT-2026/001")
        );
    }

    @Test
    @DisplayName("AUD-06: Persistência com IP do cliente e consulta escopada por tenant")
    void testAud06_AuditLogsScopedByTenantAndIp() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditLogService service = new AuditLogService(repo);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // 1. Gravar evento com IP
        service.logEvent("gestor1", COMPANY_ID, "SECURITY_AUDIT", "Teste de IP", "192.168.1.100");
        verify(repo).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("gestor1", saved.getUsername());
        assertEquals(COMPANY_ID, saved.getCompanyId());
        assertEquals("SECURITY_AUDIT", saved.getAction());
        assertEquals("Teste de IP", saved.getDetails());
        assertEquals("192.168.1.100", saved.getIpAddress());
        assertNotNull(saved.getEventTime());

        // 2. Consulta por tenant (empresa)
        AuditLog entry = new AuditLog();
        entry.setId(1L);
        entry.setCompanyId(COMPANY_ID);
        when(repo.findByCompanyIdOrderByEventTimeDesc(COMPANY_ID)).thenReturn(List.of(entry));

        List<AuditLog> results = service.getLogsByCompany(COMPANY_ID);
        assertEquals(1, results.size());
        assertEquals(COMPANY_ID, results.get(0).getCompanyId());
        verify(repo, times(1)).findByCompanyIdOrderByEventTimeDesc(COMPANY_ID);
    }
}
