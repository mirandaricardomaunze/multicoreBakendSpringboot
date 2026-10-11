package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductLabelPrintServiceTest {

    private ComercialService comercialService;
    private CompanyService companyService;
    private ProductLabelPrintService service;

    @BeforeEach
    void setUp() {
        comercialService = mock(ComercialService.class);
        companyService = mock(CompanyService.class);
        service = new ProductLabelPrintService(comercialService, companyService);

        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Fabril, S.A.");
        company.setTaxId("400999888");

        ProductDTO product = new ProductDTO(
                10L, "SKU-PROD-01", "REF-OP-01", "7901234567890", "Farinha Milho Fortificada 1kg",
                BigDecimal.valueOf(45.00), BigDecimal.valueOf(32.00), BigDecimal.valueOf(5),
                BigDecimal.valueOf(40.00), BigDecimal.valueOf(10), 1, "UNIT",
                true, 1L, "ALIMENTOS", 1L, BigDecimal.valueOf(16), "IVA 16%",
                "Alimento fortificado", null, BigDecimal.ONE, BigDecimal.ONE
        );

        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(comercialService.getAllProducts()).thenReturn(List.of(product));
    }

    @Test
    void render_a4_geraPdfValidoComLoteEValidade() {
        byte[] pdf = service.render(1L, List.of(10L), 3, "OP-2026-001", "12/2026", "A4");

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "PDF A4 com lote e validade deve conter bytes válidos gerados");
    }

    @Test
    void render_thermal_geraPdfEmBobinaTermicaComLote() {
        byte[] pdf = service.render(1L, List.of(10L), 2, "LOTE-B22", "06/2027", "THERMAL_80MM");

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "PDF térmico em bobina deve conter bytes válidos gerados");
    }

    @Test
    void render_retrocompativelSemLotePreservaComportamento() {
        byte[] pdf = service.render(1L, List.of(10L), 1);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "Método de 3 argumentos deve manter compatibilidade e gerar PDF válido");
    }
}
