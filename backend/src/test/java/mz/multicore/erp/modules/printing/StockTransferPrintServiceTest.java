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

        assertTrue(text.contains("TRANSPORTADOR"), "Devia conter o campo do transportador: " + text);
        assertTrue(text.contains("MATRÍCULA"), "Devia conter o campo da matrícula: " + text);
        assertTrue(text.contains("____________________"), "Devia conter linhas para preenchimento manual: " + text);
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

        // Cabeçalhos logísticos do modelo profissional
        assertTrue(text.contains("Referência"), "Devia conter cabeçalho Referência: " + text);
        assertTrue(text.contains("Código de barras"), "Devia conter cabeçalho Código de barras: " + text);
        assertTrue(text.contains("Designação"), "Devia conter cabeçalho Designação: " + text);
        assertTrue(text.contains("Quantidade"), "Devia conter cabeçalho Quantidade: " + text);
        assertTrue(text.contains("Emb."), "Devia conter cabeçalho Emb.: " + text);
        assertTrue(text.contains("Caixas"), "Devia conter cabeçalho Caixas: " + text);
        assertTrue(text.contains("Lote"), "Devia conter cabeçalho Lote: " + text);
        assertTrue(text.contains("Validade"), "Devia conter cabeçalho Validade: " + text);
        assertTrue(text.contains("Peso total") && text.contains("(kg)"),
                "Devia conter cabeçalho Peso total (kg): " + text);
        assertTrue(text.contains("Preço unit."), "Devia conter cabeçalho Preço unitário: " + text);
        assertTrue(text.contains("Total"), "Devia conter cabeçalho Total: " + text);

        // Valores da linha
        assertTrue(text.contains("REF-TRF-01"), "Devia conter a referência: " + text);
        assertTrue(text.contains("5601234567890"), "Devia conter o código de barras: " + text);
        assertTrue(text.contains("Produto Teste"), "Devia conter o nome do produto: " + text);
        assertTrue(text.contains("18"), "Devia conter a quantidade: " + text);
        assertTrue(text.contains("3"), "Devia conter as embalagens (18 / 6 = 3): " + text);
        assertTrue(text.contains("0.25"), "Devia conter as caixas (18 / 72 = 0.25): " + text);
        assertTrue(text.contains("Quantidade total"), "Devia conter a quantidade total: " + text);
        assertTrue(text.contains("PESO TOTAL (kg)"), "Devia conter o resumo de peso: " + text);
        assertTrue(text.contains("100,00"), "Devia conter o preço unitário: " + text);
        assertTrue(text.contains("1.800,00"), "Devia conter o total da linha: " + text);
        assertTrue(text.contains("VALOR TOTAL"), "Devia conter o valor total da guia: " + text);
    }

    @Test
    void render_imprimeTresViasNominais() throws Exception {
        StockTransfer transfer = sampleTransfer();
        StockTransferService service = mock(StockTransferService.class);
        when(service.loadForPrint(4L)).thenReturn(transfer);

        StockTransferPrintService printService = new StockTransferPrintService(service);
        byte[] pdf = printService.render(4L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(3, reader.getNumberOfPages(), "A guia de transferência deve ter 3 vias (3 páginas)");
            assertTrue(reader.getPageSizeWithRotation(1).getWidth() > reader.getPageSizeWithRotation(1).getHeight(),
                    "A guia profissional deve ser emitida em A4 horizontal");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("1.ª VIA — ORIGINAL"), "Devia conter 1.ª Via Original: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO"), "Devia conter 2.ª Via Duplicado: " + text);
        assertTrue(text.contains("3.ª VIA — TRIPLICADO"), "Devia conter 3.ª Via Triplicado: " + text);
    }

    @Test
    void renderPickingSheet_emiteGuiaDeSeparacaoInterna() throws Exception {
        StockTransfer transfer = sampleTransfer();
        transfer.setStatus(TransferStatus.IN_SEPARATION);
        StockTransferService service = mock(StockTransferService.class);
        when(service.loadForPickingPrint(5L)).thenReturn(transfer);

        StockTransferPrintService printService = new StockTransferPrintService(service);
        byte[] pdf = printService.renderPickingSheet(5L);

        String text = extract(pdf);
        assertTrue(text.toUpperCase().contains("GUIA DE SEPARA"), "Devia conter título Guia de Separação: " + text);
        assertTrue(text.contains("DOCUMENTO INTERNO DE ARMAZÉM"), "Devia conter tarja interna: " + text);
        assertTrue(text.contains("Separado por (Operador de Armazém)"), "Devia conter assinatura de separador: " + text);
        assertTrue(text.contains("Conferido por (Encarregado / Gestor)"), "Devia conter assinatura de conferente: " + text);
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
        transfer.setStatus(TransferStatus.APPROVED);

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
