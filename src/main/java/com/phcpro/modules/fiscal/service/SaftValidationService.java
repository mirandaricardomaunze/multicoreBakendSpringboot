package com.phcpro.modules.fiscal.service;

import com.phcpro.modules.fiscal.dto.FiscalSalesExportDTO;
import com.phcpro.modules.fiscal.dto.SaftValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Valida a exportação SAF-T de vendas contra a <b>XSD oficial</b> (caminho em {@code fiscal.saft.xsd-path}).
 * O export segue a <i>estrutura</i> SAF-T mas só é <b>certificável</b> depois de validado contra a XSD da
 * AT-MZ — este serviço fecha esse passo assim que a XSD é fornecida. Ver {@code docs/FISCAL_SAFT_VALIDACAO_SPEC.md}.
 */
@Service
public class SaftValidationService {

    private final FiscalSalesExportService fiscalSalesExportService;
    private final String xsdPath;

    public SaftValidationService(FiscalSalesExportService fiscalSalesExportService,
                                 @Value("${fiscal.saft.xsd-path:}") String xsdPath) {
        this.fiscalSalesExportService = fiscalSalesExportService;
        this.xsdPath = xsdPath;
    }

    /** Gera a exportação do período e valida-a contra a XSD configurada. */
    @Transactional(readOnly = true)
    public SaftValidationResult validateSalesExport(Long companyId, LocalDate from, LocalDate to) {
        FiscalSalesExportDTO export = fiscalSalesExportService.exportSales(companyId, from, to);
        return validateXml(export.xml());
    }

    /** Valida um XML SAF-T já gerado contra a XSD configurada. */
    public SaftValidationResult validateXml(String xml) {
        if (xsdPath == null || xsdPath.isBlank()) {
            return new SaftValidationResult(false, false, List.of(),
                    "XSD não configurada. Defina 'fiscal.saft.xsd-path' com a XSD oficial da AT-MZ para validar/certificar.");
        }
        File xsd = new File(xsdPath.trim());
        if (!xsd.isFile()) {
            return new SaftValidationResult(false, false, List.of(),
                    "XSD não encontrada em: " + xsdPath);
        }
        try {
            List<String> errors = SaftXsdValidator.validate(xml, xsd);
            boolean valid = errors.isEmpty();
            return new SaftValidationResult(true, valid, errors,
                    valid ? "SAF-T válido face à XSD." : errors.size() + " erro(s) de validação.");
        } catch (Exception e) {
            return new SaftValidationResult(true, false, List.of(String.valueOf(e.getMessage())),
                    "Falha ao validar contra a XSD: " + e.getMessage());
        }
    }
}
