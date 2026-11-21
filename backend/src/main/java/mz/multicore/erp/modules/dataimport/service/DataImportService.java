package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.comercial.dto.CreateProductCategoryRequest;
import mz.multicore.erp.modules.comercial.dto.CreateProductRequest;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.ProductCategoryService;
import mz.multicore.erp.modules.dataimport.dto.ImportCommitResultDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.purchases.service.PurchaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Pré-visualizar e confirmar a importação de cadastros (IMPORTACAO_DADOS_SPEC §3.1). Pré-visualizar só
 * classifica. Confirmar volta a classificar o ficheiro do zero (não confia na pré-visualização), recusa com
 * qualquer {@code ERRO} e grava as linhas {@code NOVO}/{@code ASSOCIADO} numa só transacção, sempre pelos
 * serviços donos de cada agregado (§3.3): clientes e artigos pelo comercial, fornecedores pelas compras.
 */
@Service
public class DataImportService {

    private final ClientImportService clientImport;
    private final SupplierImportService supplierImport;
    private final ProductImportService productImport;
    private final ComercialService comercialService;
    private final ProductCategoryService categoryService;
    private final PurchaseService purchaseService;

    public DataImportService(ClientImportService clientImport, SupplierImportService supplierImport,
                             ProductImportService productImport, ComercialService comercialService,
                             ProductCategoryService categoryService, PurchaseService purchaseService) {
        this.clientImport = clientImport;
        this.supplierImport = supplierImport;
        this.productImport = productImport;
        this.comercialService = comercialService;
        this.categoryService = categoryService;
        this.purchaseService = purchaseService;
    }

    /** §3.1 passo 2: classifica cada linha, sem gravar nada. */
    @Transactional(readOnly = true)
    public ImportPreviewDTO preview(String entityPath, byte[] content) {
        return switch (ImportEntity.fromPath(entityPath)) {
            case CLIENTS -> clientImport.preview(content);
            case SUPPLIERS -> supplierImport.preview(content);
            case PRODUCTS -> productImport.preview(content);
        };
    }

    /** §3.1 passo 3: tudo ou nada; uma falha a meio da escrita desfaz o que já tinha sido gravado. */
    @Transactional
    public ImportCommitResultDTO commit(String entityPath, byte[] content) {
        ImportPreviewDTO written = switch (ImportEntity.fromPath(entityPath)) {
            case CLIENTS -> commitClients(content);
            case SUPPLIERS -> commitSuppliers(content);
            case PRODUCTS -> commitProducts(content);
        };
        return ImportCommitResultDTO.of(written);
    }

    /** {@code ASSOCIADO}: o registo manual liga o cliente partilhado à empresa activa sem alterar os dados. */
    private ImportPreviewDTO commitClients(byte[] content) {
        var plan = clientImport.plan(content).requireNoErrors();
        plan.toWrite().forEach(comercialService::createClient);
        return plan.preview();
    }

    private ImportPreviewDTO commitSuppliers(byte[] content) {
        var plan = supplierImport.plan(content).requireNoErrors();
        plan.toWrite().forEach(purchaseService::createSupplier);
        return plan.preview();
    }

    /** As categorias novas são criadas primeiro (§4.3) e os artigos que as usam recebem o seu id. */
    private ImportPreviewDTO commitProducts(byte[] content) {
        var plan = productImport.plan(content).requireNoErrors();
        Map<String, Long> createdCategories = new HashMap<>();
        for (String name : plan.categoriesToCreate()) {
            Long id = categoryService.create(
                    new CreateProductCategoryRequest(ImportCategoryPlan.codeFor(name), name, null)).id();
            createdCategories.put(ImportText.comparisonKey(name), id);
        }
        for (ProductImportService.ProductLine product : plan.toWrite()) {
            CreateProductRequest request = product.request().categoryId() != null
                    ? product.request()
                    : product.withCategory(createdCategories.get(ImportText.comparisonKey(product.category())));
            comercialService.createProduct(request);
        }
        return plan.preview();
    }
}
