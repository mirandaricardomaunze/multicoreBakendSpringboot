package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.BomItemDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateBomRequest;
import mz.multicore.erp.modules.manufacturing.model.BillOfMaterials;
import mz.multicore.erp.modules.manufacturing.model.BomItem;
import mz.multicore.erp.modules.manufacturing.model.BomType;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gestor transacional de fichas técnicas de produção (Bill of Materials - BOM).
 */
@Service
public class BillOfMaterialsService {

    private final BillOfMaterialsRepository bomRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;

    @Autowired
    public BillOfMaterialsService(
            BillOfMaterialsRepository bomRepository,
            CompanyRepository companyRepository,
            ProductRepository productRepository
    ) {
        this.bomRepository = bomRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public BillOfMaterialsDTO createBom(Long companyId, CreateBomRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        if (bomRepository.existsByCompanyIdAndBomCode(companyId, request.bomCode())) {
            throw new BusinessRuleException("Já existe uma ficha técnica registada com o código '" + request.bomCode() + "'.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        Product finishedProduct = productRepository.findById(request.finishedProductId())
                .orElseThrow(() -> new BusinessRuleException("Produto acabado não encontrado: " + request.finishedProductId()));

        BillOfMaterials bom = new BillOfMaterials();
        bom.setCompany(company);
        bom.setBomCode(request.bomCode().trim().toUpperCase());
        bom.setName(request.name().trim());
        bom.setFinishedProduct(finishedProduct);
        bom.setBaseQuantity(request.baseQuantity());
        bom.setBomType(request.bomType() != null ? request.bomType() : BomType.STANDARD);
        bom.setEstimatedLaborCost(request.estimatedLaborCost() != null ? request.estimatedLaborCost() : BigDecimal.ZERO);
        bom.setEstimatedOverheadCost(request.estimatedOverheadCost() != null ? request.estimatedOverheadCost() : BigDecimal.ZERO);
        bom.setActive(true);

        for (CreateBomRequest.CreateBomItemRequest itemReq : request.items()) {
            Product rawMaterial = productRepository.findById(itemReq.rawMaterialProductId())
                    .orElseThrow(() -> new BusinessRuleException("Matéria-prima não encontrada: " + itemReq.rawMaterialProductId()));

            BomItem item = new BomItem();
            item.setRawMaterialProduct(rawMaterial);
            item.setRequiredQuantity(itemReq.requiredQuantity());
            item.setScrapPercentage(itemReq.scrapPercentage() != null ? itemReq.scrapPercentage() : BigDecimal.ZERO);
            bom.addItem(item);
        }

        BillOfMaterials saved = bomRepository.save(bom);
        return toBomDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<BillOfMaterialsDTO> listBoms(Long companyId) {
        if (companyId == null) return List.of();
        return bomRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId)
                .stream()
                .map(this::toBomDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillOfMaterialsDTO getBom(Long companyId, Long id) {
        BillOfMaterials bom = bomRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Ficha técnica não encontrada."));
        return toBomDTO(bom);
    }

    public BillOfMaterialsDTO toBomDTO(BillOfMaterials b) {
        List<BomItemDTO> itemDTOs = b.getItems().stream()
                .map(i -> {
                    BigDecimal unitCost = i.getRawMaterialProduct().getPurchasePrice() != null
                            ? i.getRawMaterialProduct().getPurchasePrice()
                            : BigDecimal.ZERO;
                    return new BomItemDTO(
                            i.getId(),
                            i.getRawMaterialProduct().getId(),
                            i.getRawMaterialProduct().getSku(),
                            i.getRawMaterialProduct().getName(),
                            i.getRequiredQuantity(),
                            i.getUnitOfMeasure() != null ? i.getUnitOfMeasure() : "UND",
                            i.getScrapPercentage(),
                            unitCost
                    );
                }).toList();

        BigDecimal rawTotal = itemDTOs.stream()
                .map(it -> it.estimatedUnitCost().multiply(it.requiredQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEst = rawTotal.add(b.getEstimatedLaborCost()).add(b.getEstimatedOverheadCost());

        return new BillOfMaterialsDTO(
                b.getId(),
                b.getCompany().getId(),
                b.getBomCode(),
                b.getName(),
                b.getFinishedProduct().getId(),
                b.getFinishedProduct().getSku(),
                b.getFinishedProduct().getName(),
                b.getBaseQuantity(),
                b.getBomType(),
                b.isActive(),
                b.getEstimatedLaborCost(),
                b.getEstimatedOverheadCost(),
                totalEst,
                itemDTOs
        );
    }
}
