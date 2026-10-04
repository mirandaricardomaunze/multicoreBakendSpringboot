package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import mz.multicore.erp.modules.comercial.model.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockTransferPrintServiceTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void render_imprimeMotoristaEMatricula() throws Exception {
        StockTransfer transfer = sampleTransfer();
        transfer.setDriverName("Carlos Sitoe");
        transfer.setVehiclePlate("AFE-123-MC");

        StockTransferService service = mock(StockTransferService.class);
        when(service.loadForPrint(1L)).thenReturn(transfer);

        StockTransferPrintService printService = new StockTransferPrintService(service);
        byte[] pdf = printService.render(1L);

        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        String text = extract(pdf);

        assertTrue(text.contains("TRF-2026/001"), text);
        assertTrue(text.contains("Carlos Sitoe"), "Devia conter o nome do motorista: " + text);
        assertTrue(text.contains("AFE-123-MC"), "Devia conter a matrícula do veículo: " + text);
    }

    @Test
    void render_semMotoristaEMatricula_imprimeLinhasSublinhadas() throws Exception {
        StockTransfer transfer = sampleTransfer();
        transfer.setDriverName(null);
        transfer.setVehiclePlate(null);
        transfer.setResponsible(null);
        transfer.setVehicle(null);

        StockTransferService service = mock(StockTransferService.class);
        when(service.loadForPrint(2L)).thenReturn(transfer);

        StockTransferPrintService printService = new StockTransferPrintService(service);
        byte[] pdf = printService.render(2L);

        String text = extract(pdf);

        assertTrue(text.contains("Motorista: _____"), "Devia conter linha para preenchimento de motorista: " + text);
        assertTrue(text.contains("Matrícula: _____"), "Devia conter linha para preenchimento de matrícula: " + text);
    }

    @Test
    void render_imprimeTabelaComColunasObrigatorias() throws Exception {
        StockTransfer transfer = sampleTransfer();
        Product product = transfer.getLines().get(0).getProduct();
        product.setReference("REF-TRF-01");
        product.setBarcode("5601234567890");
        product.setPackagesPerBox(12);
        product.setUnitsPerPackage(6);
        product.setUnitPrice(new BigDecimal("100.00"));
        transfer.getLines().get(0).setQuantity(new BigDecimal("18"));

        StockTransferService service = mock(StockTransferService.class);
        when(service.loadForPrint(3L)).thenReturn(transfer);

        StockTransferPrintService printService = new StockTransferPrintService(service);
        byte[] pdf = printService.render(3L);

        String text = extract(pdf);

        // Cabeçalhos das 9 colunas obrigatórias
        assertTrue(text.contains("Referência"), "Devia conter cabeçalho Referência: " + text);
        assertTrue(text.contains("Cód. Barras"), "Devia conter cabeçalho Cód. Barras: " + text);
        assertTrue(text.contains("Produto"), "Devia conter cabeçalho Produto: " + text);
        assertTrue(text.contains("Qtd"), "Devia conter cabeçalho Qtd: " + text);
        assertTrue(text.contains("Embalagem"), "Devia conter cabeçalho Embalagem: " + text);
        assertTrue(text.contains("Caixa"), "Devia conter cabeçalho Caixa: " + text);
        assertTrue(text.contains("% da Caixa"), "Devia conter cabeçalho % da Caixa: " + text);
        assertTrue(text.contains("Valor Unit."), "Devia conter cabeçalho Valor Unit.: " + text);
        assertTrue(text.contains("IVA"), "Devia conter cabeçalho IVA: " + text);

        // Valores da linha
        assertTrue(text.contains("REF-TRF-01"), "Devia conter a referência: " + text);
        assertTrue(text.contains("5601234567890"), "Devia conter o código de barras: " + text);
        assertTrue(text.contains("Produto Teste"), "Devia conter o nome do produto: " + text);
        assertTrue(text.contains("18"), "Devia conter a quantidade: " + text);
        assertTrue(text.contains("3"), "Devia conter as embalagens (18 / 6 = 3): " + text);
        assertTrue(text.contains("0.25"), "Devia conter as caixas (18 / 72 = 0.25): " + text);
        assertTrue(text.contains("25%"), "Devia conter a percentagem da caixa (25%): " + text);
        assertTrue(text.contains("100,00"), "Devia conter o valor unitário: " + text);
        assertTrue(text.contains("16%"), "Devia conter a taxa de IVA: " + text);

        // Bloco de totais
        assertTrue(text.contains("Total Mercadoria (Líquido)"), "Devia conter total líquido: " + text);
        assertTrue(text.contains("Total IVA"), "Devia conter total IVA: " + text);
        assertTrue(text.contains("Total Geral"), "Devia conter total geral: " + text);
    }

    private static StockTransfer sampleTransfer() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Teste");

        Warehouse origin = new Warehouse();
        origin.setId(10L);
        origin.setName("Armazém Central");

        Warehouse dest = new Warehouse();
        dest.setId(20L);
        dest.setName("Loja Baixa");

        Product product = new Product();
        product.setId(100L);
        product.setSku("PRD-1");
        product.setReference("REF-1");
        product.setBarcode("560000000001");
        product.setName("Produto Teste");
        product.setUnitPrice(new BigDecimal("100.00"));
        product.setPackagesPerBox(1);
        product.setUnitsPerPackage(1);

        StockTransfer transfer = new StockTransfer();
        transfer.setId(1L);
        transfer.setTransferNumber("TRF-2026/001");
        transfer.setTransferDate(LocalDateTime.of(2026, 9, 26, 10, 0));
        transfer.setCompany(company);
        transfer.setOriginWarehouse(origin);
        transfer.setDestinationWarehouse(dest);
        transfer.setStatus(TransferStatus.PENDING_APPROVAL);

        StockTransferLine line = new StockTransferLine();
        line.setTransfer(transfer);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("10"));
        transfer.getLines().add(line);

        return transfer;
    }

    private static String extract(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
