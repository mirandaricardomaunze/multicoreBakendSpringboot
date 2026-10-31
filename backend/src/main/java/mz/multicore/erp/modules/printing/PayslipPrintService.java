package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.service.HRService;
import mz.multicore.erp.modules.hr.service.PayrollDeductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Fachada de emissão de recibos de salário. */
@Service
public class PayslipPrintService {

    public static final List<String> PAYSLIP_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Trabalhador)",
            "2.ª VIA — DUPLICADO (Recursos Humanos / Arquivo)"
    );

    private final HRService hrService;
    private final PayslipDocumentRenderer documentRenderer;

    @Autowired
    public PayslipPrintService(HRService hrService, PayslipDocumentRenderer documentRenderer) {
        this.hrService = hrService;
        this.documentRenderer = documentRenderer;
    }

    /** Preserva a construção usada pelas integrações e testes anteriores à decomposição. */
    public PayslipPrintService(HRService hrService, PayrollDeductionService deductionService) {
        this(hrService, new PayslipDocumentRenderer(new PayslipCompensationTableRenderer(deductionService)));
    }

    @Transactional(readOnly = true)
    public byte[] render(Long payslipId) {
        Payslip payslip = hrService.loadPayslipForPrint(payslipId);
        return documentRenderer.render(
                payslip,
                payslip.getEmployee().getCompany(),
                PAYSLIP_COPIES
        );
    }
}
