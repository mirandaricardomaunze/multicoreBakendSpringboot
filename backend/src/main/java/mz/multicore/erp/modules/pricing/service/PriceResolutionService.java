package mz.multicore.erp.modules.pricing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.pricing.dto.ResolvedPriceDTO;
import mz.multicore.erp.modules.pricing.repository.ClientPriceListRepository;
import mz.multicore.erp.modules.pricing.repository.PriceListItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Single source of the selling price of a document line (SA-C03). Resolution order:
 * <ol>
 *   <li>the client's price list, when active and valid on the date, using the highest tier reached;</li>
 *   <li>otherwise the existing rule, {@code Product.effectiveUnitPrice} (wholesale or retail).</li>
 * </ol>
 * Without assigned lists the result is exactly the one produced before this capability existed.
 */
@Service
public class PriceResolutionService {

    public static final String SOURCE_PRICE_LIST = "PRICE_LIST";
    public static final String SOURCE_WHOLESALE = "WHOLESALE";
    public static final String SOURCE_RETAIL = "RETAIL";

    /** List selected for a document; resolved once and reused for every line. */
    public record PriceSelection(Long priceListId, String priceListName) {
        public static final PriceSelection NONE = new PriceSelection(null, null);

        public boolean hasList() {
            return priceListId != null;
        }
    }

    public record ResolvedPrice(BigDecimal unitPrice, String source, String priceListName) {}

    private final ClientPriceListRepository assignmentRepository;
    private final PriceListItemRepository itemRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    public PriceResolutionService(ClientPriceListRepository assignmentRepository,
                                  PriceListItemRepository itemRepository,
                                  ClientRepository clientRepository,
                                  ProductRepository productRepository) {
        this.assignmentRepository = assignmentRepository;
        this.itemRepository = itemRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
    }

    /**
     * Unit price to store on a line. A {@code null} resolver (legacy constructors and tests that build the
     * services by hand) keeps the previous rule.
     */
    public static BigDecimal unitPrice(PriceResolutionService resolver, PriceSelection selection,
                                       Product product, BigDecimal quantity) {
        if (resolver == null) return product.effectiveUnitPrice(quantity);
        return resolver.resolve(selection, product, quantity).unitPrice();
    }

    /** Selection for a document. Null-safe: without a resolver or a client it returns {@link PriceSelection#NONE}. */
    public static PriceSelection selectionFor(PriceResolutionService resolver, Long companyId, Client client) {
        if (resolver == null || client == null || client.getId() == null || companyId == null) return PriceSelection.NONE;
        return resolver.selectionFor(companyId, client.getId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public PriceSelection selectionFor(Long companyId, Long clientId, LocalDate date) {
        if (companyId == null || clientId == null) return PriceSelection.NONE;
        return assignmentRepository.findAssignment(companyId, clientId)
                .map(a -> a.getPriceList())
                .filter(list -> PriceListRules.isApplicable(list.isActive(), list.getValidFrom(), list.getValidTo(), date))
                .map(list -> new PriceSelection(list.getId(), list.getName()))
                .orElse(PriceSelection.NONE);
    }

    @Transactional(readOnly = true)
    public ResolvedPrice resolve(PriceSelection selection, Product product, BigDecimal quantity) {
        if (selection != null && selection.hasList()) {
            var tiers = itemRepository.findByPriceListIdAndProductId(selection.priceListId(), product.getId()).stream()
                    .map(i -> new PriceListRules.Tier(i.getMinQuantity(), i.getPrice()))
                    .toList();
            var tier = PriceListRules.pickTier(tiers, quantity);
            if (tier.isPresent()) {
                return new ResolvedPrice(tier.get().price(), SOURCE_PRICE_LIST, selection.priceListName());
            }
        }
        BigDecimal legacy = product.effectiveUnitPrice(quantity);
        boolean wholesale = legacy != null && product.getUnitPrice() != null
                && product.getWholesalePrice() != null && legacy.compareTo(product.getUnitPrice()) != 0;
        return new ResolvedPrice(legacy, wholesale ? SOURCE_WHOLESALE : SOURCE_RETAIL, null);
    }

    /** Desktop preview: the same price the document will store. */
    @Transactional(readOnly = true)
    public ResolvedPriceDTO resolveForApi(Long companyId, Long clientId, Long productId, BigDecimal quantity) {
        CurrentUserContext.requireCompany(companyId);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("A quantidade tem de ser maior que zero.");
        }
        Product product = productRepository.findByIdAndCompaniesId(productId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado nesta empresa."));
        PriceSelection selection = PriceSelection.NONE;
        if (clientId != null) {
            clientRepository.findByIdAndCompaniesId(clientId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado nesta empresa."));
            selection = selectionFor(companyId, clientId, LocalDate.now());
        }
        ResolvedPrice price = resolve(selection, product, quantity);
        return new ResolvedPriceDTO(product.getId(), clientId, quantity, price.unitPrice(), price.source(),
                price.priceListName());
    }
}
