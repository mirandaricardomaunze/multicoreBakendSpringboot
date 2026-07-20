package com.phcpro.modules.comercial;

import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regressão do bug multi-tenant da numeração de documentos.
 *
 * <p>O número de documento é gerado <b>por empresa</b> (cada NUIT tem a sua série gapless). Antes do
 * fix, a coluna {@code invoices.invoice_number} tinha uma restrição {@code UNIQUE} <b>global</b>, pelo
 * que duas empresas que chegassem ao mesmo número (ex.: ambas {@code FT-2026/1}) colidiam com erro de
 * integridade — <b>mesmo sem concorrência</b>. O fix trocou-a por
 * {@code UNIQUE(company_id, invoice_number)} (anotação na entidade + migração {@code V31}).
 *
 * <p>Teste de camada JPA (H2, leve): a unicidade é uma regra <b>da base de dados</b>, logo não é
 * exercitável com repositórios <i>mock</i> — precisa de persistência real.
 */
@DataJpaTest
class InvoiceNumberUniquenessPerCompanyTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private InvoiceRepository invoiceRepository;

    @Test
    void mesmoNumeroPermitidoEmEmpresasDiferentes() {
        Company a = company("Empresa A", "100000001");
        Company b = company("Empresa B", "100000002");
        Client clientA = client("Cliente A", "200000001");
        Client clientB = client("Cliente B", "200000002");

        invoiceRepository.saveAndFlush(invoice("FT-2026/1", a, clientA));

        // Antes do fix a UNIQUE global rebentava aqui; agora as duas empresas coexistem.
        assertDoesNotThrow(() -> invoiceRepository.saveAndFlush(invoice("FT-2026/1", b, clientB)));
        assertEquals(2, invoiceRepository.count());
    }

    @Test
    void mesmoNumeroRejeitadoNaMesmaEmpresa() {
        Company a = company("Empresa A", "100000003");
        Client clientA = client("Cliente A", "200000003");

        invoiceRepository.saveAndFlush(invoice("FT-2026/1", a, clientA));

        // A unicidade por empresa mantém-se: repetir o número dentro da mesma empresa é rejeitado.
        assertThrows(DataIntegrityViolationException.class,
                () -> invoiceRepository.saveAndFlush(invoice("FT-2026/1", a, clientA)));
    }

    private Company company(String name, String taxId) {
        Company c = new Company();
        c.setName(name);
        c.setTaxId(taxId);
        return companyRepository.saveAndFlush(c);
    }

    private Client client(String name, String taxId) {
        Client c = new Client();
        c.setName(name);
        c.setTaxId(taxId);
        c.setEmail(taxId + "@example.co.mz");
        return clientRepository.saveAndFlush(c);
    }

    private Invoice invoice(String number, Company company, Client client) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setCompany(company);
        invoice.setClient(client);
        return invoice;
    }
}
