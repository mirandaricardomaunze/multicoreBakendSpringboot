package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF do contrato de trabalho. Ver docs/RH_COMPLETO_SPEC.md §B1.
 *
 * <p>Ao contrário das facturas e encomendas, este documento <b>não tem linhas nem totais</b> — tem
 * cláusulas. Por isso compõe o cabeçalho da empresa e o bloco de assinaturas partilhados, e não o
 * {@code LineItemsTableRenderer}/{@code TotalsBlockRenderer}: forçar uma tabela de linhas aqui
 * seria dobrar o documento à ferramenta em vez do contrário.
 *
 * <p>As cláusulas são <b>geradas do que está gravado</b>, não texto fixo: um contrato impresso que
 * diga coisa diferente do que a folha usa é pior do que não haver contrato nenhum.
 */
@Service
public class EmploymentContractPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmploymentContractRepository contractRepository;

    public EmploymentContractPrintService(EmploymentContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long contractId) {
        EmploymentContract contract = contractRepository
                .findByIdWithEmployeeAndCompanyId(contractId, CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado."));
        Company company = contract.getEmployee().getCompany();

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company,
                    "Contrato de Trabalho — " + contract.getContractType().getLabel(),
                    contract.getContractNumber()));
            doc.add(buildPartiesBlock(contract));
            doc.add(buildTermsTable(contract));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(buildClauses(contract));
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(new Paragraph(java.time.LocalDate.now().format(DATE_FMT), PdfTheme.bodyFont()));
            doc.add(PdfDocumentBuilder.spacer(18f));
            doc.add(SignatureBlockRenderer.build(
                    "Pela entidade empregadora", "O(A) trabalhador(a)"));
        });
    }

    private PdfPTable buildPartiesBlock(EmploymentContract c) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingBefore(8f);
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Trabalhador(a)", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(c.getEmployee().getName(), PdfTheme.boldFont()));
        left.addElement(new Paragraph("Nº interno: " + c.getEmployee().getEmployeeNumber(), PdfTheme.bodyFont()));
        if (c.getEmployee().getTaxId() != null) {
            left.addElement(new Paragraph("NUIT: " + c.getEmployee().getTaxId(), PdfTheme.bodyFont()));
        }
        if (c.getEmployee().getInssNumber() != null) {
            left.addElement(new Paragraph("Nº INSS: " + c.getEmployee().getInssNumber(), PdfTheme.bodyFont()));
        }
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.addElement(rightAligned("Situação", PdfTheme.subtitleFont()));
        right.addElement(rightAligned(c.getStatus().getLabel(), PdfTheme.boldFont()));
        // A caducidade é derivada da data, nunca gravada — o PDF diz o mesmo que o ecrã.
        if (c.isExpired(java.time.LocalDate.now())) {
            right.addElement(rightAligned("Prazo terminado em " + c.getEndDate().format(DATE_FMT),
                    PdfTheme.smallFont()));
        }
        table.addCell(right);

        return table;
    }

    private PdfPTable buildTermsTable(EmploymentContract c) {
        PdfPTable table = new PdfPTable(new float[]{35f, 65f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);

        header(table, "Condições acordadas");
        header(table, "");

        row(table, "Tipo de contrato", c.getContractType().getLabel());
        row(table, "Função", c.getJobTitle());
        row(table, "Início", c.getStartDate().format(DATE_FMT));
        row(table, "Termo", c.getEndDate() == null
                ? "Sem termo" : c.getEndDate().format(DATE_FMT));
        if (c.getProbationEndDate() != null) {
            row(table, "Período experimental", "Até " + c.getProbationEndDate().format(DATE_FMT));
        }
        row(table, "Remuneração base", MoneyFormat.format(c.getAgreedSalary()));
        row(table, "Horário semanal", c.getWeeklyHours() + " horas");
        if (c.getWorkLocation() != null) {
            row(table, "Local de trabalho", c.getWorkLocation());
        }
        if (c.getTermReason() != null) {
            row(table, "Motivo do termo", c.getTermReason());
        }
        if (c.getRenewedFrom() != null) {
            row(table, "Renovação de", c.getRenewedFrom().getContractNumber());
        }
        if (c.getTerminationDate() != null) {
            row(table, "Cessação", c.getTerminationDate().format(DATE_FMT)
                    + (c.getTerminationReason() == null ? "" : " — " + c.getTerminationReason()));
        }
        return table;
    }

    /**
     * Cláusulas montadas a partir do que está gravado. Cada uma repete um valor da tabela acima de
     * propósito: é a tabela que se lê num relance, e são as cláusulas que se assinam.
     */
    private Paragraph buildClauses(EmploymentContract c) {
        List<String> clauses = new ArrayList<>();
        clauses.add(String.format(
                "1.ª (Objecto) — O(A) trabalhador(a) é admitido(a) para exercer as funções de %s, "
                        + "sob a autoridade e direcção da entidade empregadora.", c.getJobTitle()));
        clauses.add(c.getEndDate() == null
                ? String.format("2.ª (Duração) — O presente contrato é celebrado sem termo, com "
                        + "início em %s.", c.getStartDate().format(DATE_FMT))
                : String.format("2.ª (Duração) — O presente contrato vigora de %s a %s.",
                        c.getStartDate().format(DATE_FMT), c.getEndDate().format(DATE_FMT)));
        if (c.getTermReason() != null) {
            clauses.add(String.format(
                    "3.ª (Justificação do termo) — O termo estipulado fundamenta-se em: %s.",
                    c.getTermReason()));
        }
        clauses.add(String.format(
                "%d.ª (Retribuição) — O(A) trabalhador(a) aufere a remuneração base mensal de %s, "
                        + "paga nos termos legais e sujeita aos descontos obrigatórios.",
                clauses.size() + 1, MoneyFormat.format(c.getAgreedSalary())));
        clauses.add(String.format(
                "%d.ª (Período normal de trabalho) — O período normal de trabalho é de %d horas "
                        + "semanais%s.",
                clauses.size() + 1, c.getWeeklyHours(),
                c.getWorkLocation() == null ? "" : ", prestadas em " + c.getWorkLocation()));
        if (c.getProbationEndDate() != null) {
            clauses.add(String.format(
                    "%d.ª (Período experimental) — As partes acordam um período experimental que "
                            + "termina em %s.",
                    clauses.size() + 1, c.getProbationEndDate().format(DATE_FMT)));
        }
        clauses.add(String.format(
                "%d.ª (Legislação aplicável) — Em tudo o que não estiver expressamente previsto "
                        + "aplica-se a Lei do Trabalho e demais legislação em vigor.",
                clauses.size() + 1));

        Paragraph body = new Paragraph();
        body.setFont(PdfTheme.bodyFont());
        for (String clause : clauses) {
            Paragraph line = new Paragraph(clause, PdfTheme.bodyFont());
            line.setSpacingAfter(6f);
            body.add(line);
        }
        return body;
    }

    private Paragraph rightAligned(String text, com.lowagie.text.Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private void header(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell(new Paragraph(label, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void row(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, PdfTheme.boldFont()));
        labelCell.setBorderColor(PdfTheme.BORDER);
        labelCell.setPadding(6f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, PdfTheme.bodyFont()));
        valueCell.setBorderColor(PdfTheme.BORDER);
        valueCell.setPadding(6f);
        table.addCell(valueCell);
    }
}
