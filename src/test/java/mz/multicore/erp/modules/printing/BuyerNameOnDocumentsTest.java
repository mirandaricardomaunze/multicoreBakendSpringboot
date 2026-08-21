package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O nome escrito à mão tem de chegar ao papel.
 *
 * <p>Relatado pelo utilizador: "o nome opcional não aceita no documento de cotação". Aceitava — era
 * validado, gravado e devolvido pela API. O que não fazia era <b>aparecer</b>: os três documentos
 * A4 (fatura, encomenda, cotação) mandavam ao {@link ClientBlockRenderer} apenas o cliente
 * registado, pelo que uma venda a "Consumidor Final" saía com o nome do cliente genérico em vez do
 * comprador que o operador escreveu.
 *
 * <p>Um defeito, três documentos — porque a peça é partilhada. Estes casos lêem o texto do PDF, e
 * não o código que o gera.
 */
class BuyerNameOnDocumentsTest {

    @Test
    void oNomeEscritoAMaoEOQueApareceNoDocumento() throws Exception {
        String text = render(walkInClient(), "Sr. Chirindza");

        assertTrue(text.contains("Sr. Chirindza"),
                "o comprador escrito à mão tem de aparecer: " + text);
        assertFalse(text.contains("Consumidor Final"),
                "não pode sair o cliente genérico no lugar do comprador real: " + text);
    }

    @Test
    void semNomeEscritoOoDocumentoIdentificaOClienteRegistado() throws Exception {
        String text = render(registeredClient(), null);

        assertTrue(text.contains("Mercearia Bom Preco"), text);
        assertTrue(text.contains("400123456"), "o NUIT do cliente registado mantém-se: " + text);
    }

    @Test
    void nomeEmBrancoNaoApagaOClienteRegistado() throws Exception {
        // Espaços não são um nome. Um campo deixado com um espaço não pode apagar quem compra.
        String text = render(registeredClient(), "   ");

        assertTrue(text.contains("Mercearia Bom Preco"), text);
    }

    @Test
    void oNomeEscritoNaoApagaOsDadosFiscaisDoCliente() throws Exception {
        // O nome muda; NUIT e morada continuam a vir do registo, que é o que a AT lê.
        String text = render(registeredClient(), "Recebido por: D. Amélia");

        assertTrue(text.contains("D. Amélia"), text);
        assertTrue(text.contains("400123456"), text);
        assertTrue(text.contains("Av. 25 de Setembro"), text);
    }

    private static String render(Client client, String buyerLabel) throws Exception {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Armazem Central");

        byte[] pdf = PdfDocumentBuilder.buildA4(doc ->
                doc.add(ClientBlockRenderer.build(client, buyerLabel,
                        LocalDateTime.of(2026, 8, 21, 9, 0), warehouse)));

        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    /** O cliente genérico que o sistema usa quando a venda não é a um cliente registado. */
    private static Client walkInClient() {
        Client client = new Client();
        client.setName("Consumidor Final");
        return client;
    }

    private static Client registeredClient() {
        Client client = new Client();
        client.setName("Mercearia Bom Preco");
        client.setTaxId("400123456");
        client.setAddress("Av. 25 de Setembro, Maputo");
        return client;
    }
}
