package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Issues the commercial invoice for one billing period of a contract. The contract's fixed amount is the price
 * before VAT and the invoice adds the standard VAT rate. Each contract bills through its own non-stock service
 * product, so the invoice line names the contract and its price always follows the contract's current amount.
 */
@Component
public class RecurringContractInvoicer {

    static final String SKU_PREFIX = "AVENCA-";
    private static final int MAX_NAME = 255;

    private final ComercialService comercialService;
    private final InvoiceLookup invoiceLookup;
    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;
    private final CompanyLookup companyLookup;
    private final WarehouseLookup warehouseLookup;

    public RecurringContractInvoicer(ComercialService comercialService, InvoiceLookup invoiceLookup,
                                     ProductLookup productLookup, ProductCommandService productCommandService,
                                     CompanyLookup companyLookup, WarehouseLookup warehouseLookup) {
        this.comercialService = comercialService;
        this.invoiceLookup = invoiceLookup;
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
        this.companyLookup = companyLookup;
        this.warehouseLookup = warehouseLookup;
    }

    public Invoice invoicePeriod(RecurringContract contract) {
        if (contract.getFixedAmount() == null || contract.getFixedAmount().signum() <= 0) {
            throw new BusinessRuleException("O contrato " + contract.getContractNumber() + " não tem valor a faturar.");
        }
        Long companyId = contract.getCompany().getId();
        Product product = serviceProduct(contract, companyId);
        // The line is a non-stock service, but every invoice is issued against a warehouse of the company.
        Warehouse warehouse = warehouseLookup.listForCompany(companyId).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Registe um armazém antes de faturar contratos."));
        CreateInvoiceLineRequest line = new CreateInvoiceLineRequest(
                product.getId(), BigDecimal.ONE, TaxRates.STANDARD_VAT, BigDecimal.ZERO, null, null);
        InvoiceDTO issued = comercialService.createInvoice(new CreateInvoiceRequest(
                contract.getClient().getId(), companyId, warehouse.getId(), List.of(line)));
        return invoiceLookup.require(issued.id());
    }

    private Product serviceProduct(RecurringContract contract, Long companyId) {
        // SKUs are unique across companies, so the company is part of the code.
        String sku = SKU_PREFIX + companyId + "-" + contract.getContractNumber();
        Product product = productLookup.findBySkuForCompany(sku, companyId).orElseGet(() -> {
            Product created = new Product();
            created.setSku(sku);
            created.getCompanies().add(companyLookup.reference(companyId));
            return created;
        });
        String name = "Avença " + contract.getContractNumber() + " - " + contract.getDescription();
        product.setName(name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name);
        product.setUnitPrice(contract.getFixedAmount());
        product.setStockTracked(false);
        return productCommandService.saveProduct(product);
    }
}
