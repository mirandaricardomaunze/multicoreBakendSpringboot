package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de fornecedores das compras para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * As compras directas e as encomendas a fornecedor, pouco lidas fora das compras, ficam aqui por serem do
 * agregado mais próximo. Escritas alheias passam por {@link SupplierCommandService}. As entidades devolvidas
 * servem só para relações JPA dentro do backend e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class SupplierLookup {

    static final String NOT_FOUND = "Fornecedor não encontrado.";

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierLookup(SupplierRepository supplierRepository,
                          PurchaseRepository purchaseRepository,
                          PurchaseOrderRepository purchaseOrderRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    /** Fornecedor pelo id; {@code BusinessRuleException("Fornecedor não encontrado.")} se não existir. */
    public Supplier require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Supplier> find(Long id) {
        return id == null ? Optional.empty() : supplierRepository.findById(id);
    }

    /** Todos os fornecedores da empresa (activos e inactivos). */
    public List<Supplier> listForCompany(Long companyId) {
        return supplierRepository.findByCompanyId(companyId);
    }

    /** Fornecedores activos da empresa. */
    public List<Supplier> listActiveForCompany(Long companyId) {
        return supplierRepository.findByCompanyIdAndActiveTrue(companyId);
    }

    /** Compras directas da empresa. */
    public List<Purchase> listPurchasesForCompany(Long companyId) {
        return purchaseRepository.findByCompanyId(companyId);
    }

    /** Compras directas de todas as empresas (visão de plataforma, sem empresa activa). */
    public List<Purchase> listAllPurchases() {
        return purchaseRepository.findAll();
    }

    /** Encomendas a fornecedor da empresa, das mais recentes para as mais antigas. */
    public List<PurchaseOrder> listPurchaseOrdersForCompany(Long companyId) {
        return purchaseOrderRepository.findByCompanyIdOrderByOrderDateDesc(companyId);
    }
}
