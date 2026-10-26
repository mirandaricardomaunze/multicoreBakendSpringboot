package mz.multicore.erp.desktop.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de ComercialApiClient (Fase 93).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegação canónica para os 6 clientes especializados.
 */
class ComercialApiClientDecompositionHarnessTest {

    private static String readSource(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + relativePath, e);
        }
    }

    private static long countLines(String relativePath) {
        try {
            return Files.lines(Path.of(relativePath)).count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + relativePath, e);
        }
    }

    @Test
    @DisplayName("CACD-01: Limites físicos estritos de linhas de código")
    void cacd01_limitesFisicosLinhasCodigo() {
        long facadeLines = countLines("src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java");
        long clientLines = countLines("src/main/java/mz/multicore/erp/desktop/client/CommercialClientApiClient.java");
        long productLines = countLines("src/main/java/mz/multicore/erp/desktop/client/CommercialProductApiClient.java");
        long orderLines = countLines("src/main/java/mz/multicore/erp/desktop/client/CommercialOrderApiClient.java");
        long quotationLines = countLines("src/main/java/mz/multicore/erp/desktop/client/CommercialQuotationApiClient.java");
        long guideLines = countLines("src/main/java/mz/multicore/erp/desktop/client/CommercialDeliveryGuideApiClient.java");
        long invoiceLines = countLines("src/main/java/mz/multicore/erp/desktop/client/CommercialInvoiceApiClient.java");

        assertThat(facadeLines)
                .as("ComercialApiClient.java deve ter <= 225 linhas (reduzido de 622 linhas)")
                .isLessThanOrEqualTo(225);

        assertThat(clientLines)
                .as("CommercialClientApiClient.java deve ter <= 60 linhas")
                .isLessThanOrEqualTo(60);

        assertThat(productLines)
                .as("CommercialProductApiClient.java deve ter <= 140 linhas")
                .isLessThanOrEqualTo(140);

        assertThat(orderLines)
                .as("CommercialOrderApiClient.java deve ter <= 150 linhas")
                .isLessThanOrEqualTo(150);

        assertThat(quotationLines)
                .as("CommercialQuotationApiClient.java deve ter <= 105 linhas")
                .isLessThanOrEqualTo(105);

        assertThat(guideLines)
                .as("CommercialDeliveryGuideApiClient.java deve ter <= 75 linhas")
                .isLessThanOrEqualTo(75);

        assertThat(invoiceLines)
                .as("CommercialInvoiceApiClient.java deve ter <= 185 linhas")
                .isLessThanOrEqualTo(185);
    }

    @Test
    @DisplayName("CACD-02: Zero diálogos legados JOptionPane")
    void cacd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/CommercialClientApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/CommercialProductApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/CommercialOrderApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/CommercialQuotationApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/CommercialDeliveryGuideApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/CommercialInvoiceApiClient.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("CACD-03: Delegação canónica para clientes especializados")
    void cacd03_delegacaoCanonicaParaClientesEspecializados() {
        String facade = readSource("src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java");

        assertThat(facade)
                .contains("clientApiClient.getClients()")
                .contains("productApiClient.getAllProducts()")
                .contains("orderApiClient.getOrdersByCompany(")
                .contains("quotationApiClient.getQuotationsByCompany(")
                .contains("deliveryGuideApiClient.getDeliveryGuidesByCompany(")
                .contains("invoiceApiClient.getAllInvoices()");
    }

    @Test
    @DisplayName("CACD-04: Contratos e métodos públicos preservados")
    void cacd04_contratosPreservados() {
        String facade = readSource("src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java");

        assertThat(facade)
                .contains("getClients()")
                .contains("getAllProducts()")
                .contains("getProductsPage(")
                .contains("getOrdersByCompany(")
                .contains("createQuotation(")
                .contains("createDeliveryGuide(")
                .contains("createInvoice(")
                .contains("getReceivablesAging()")
                .contains("record SaveClientRequest")
                .contains("record RejectGuideRequest");
    }
}
