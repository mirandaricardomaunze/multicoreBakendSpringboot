package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de geração de PDF para o Termo de Entrega e Responsabilidade de Equipamento / EPI.
 */
@Service
public class QualityEquipmentPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WorkerEquipmentAssignmentRepository assignmentRepository;

    public QualityEquipmentPrintService(WorkerEquipmentAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public byte[] renderDeliverySheet(Long assignmentId, Long companyId) {
        if (assignmentId == null) {
            throw new BusinessRuleException("Identificador de atribuição obrigatório.");
        }
        Long effectiveCompanyId = companyId != null ? companyId : CurrentUserContext.findCurrentCompanyId();
        if (effectiveCompanyId == null) {
            effectiveCompanyId = 1L;
        }

        WorkerEquipmentAssignment assignment = assignmentRepository
                .findByIdWithDetailsAndCompanyId(assignmentId, effectiveCompanyId)
                .orElseThrow(() -> new BusinessRuleException("Atribuição de equipamento não encontrada para impressão."));

        return PdfDocumentBuilder.buildA4(doc -> renderDocument(doc, assignment));
    }

    private void renderDocument(Document document, WorkerEquipmentAssignment a) {
        Company company = a.getCompany();
        Employee employee = a.getEmployee();
        Equipment equipment = a.getEquipment();

        String docNumber = "EPI-" + String.format("%05d", a.getId());
        String statusLabel = a.getStatus() != null ? a.getStatus().getLabel().toUpperCase() : "ATIVO";

        // 1. Cabeçalho com dados da Empresa
        document.add(CompanyHeaderRenderer.build(
                company,
                "TERMO DE ENTREGA E RESPONSABILIDADE DE EPI",
                docNumber,
                statusLabel
        ));

        // 2. Identificação do Colaborador
        document.add(buildSectionTitle("1. IDENTIFICAÇÃO DO COLABORADOR"));
        document.add(PdfDocumentBuilder.spacer(4f));
        document.add(buildEmployeeInfoTable(employee));
        document.add(PdfDocumentBuilder.spacer(10f));

        // 3. Especificações do Equipamento
        document.add(buildSectionTitle("2. ESPECIFICAÇÕES DO EQUIPAMENTO DE PROTEÇÃO / TRABALHO"));
        document.add(PdfDocumentBuilder.spacer(4f));
        document.add(buildEquipmentTable(equipment, a));
        document.add(PdfDocumentBuilder.spacer(10f));

        // 4. Vigência e Prazos de Validade
        document.add(buildSectionTitle("3. VIGÊNCIA, PRAZO DE VALIDADE E RASTREIO"));
        document.add(PdfDocumentBuilder.spacer(4f));
        document.add(buildDatesTable(equipment, a));
        document.add(PdfDocumentBuilder.spacer(12f));

        // 5. Termo de Compromisso e Responsabilidade
        document.add(buildSectionTitle("4. TERMO DE COMPROMISSO E DECLARAÇÃO DO TRABALHADOR"));
        document.add(PdfDocumentBuilder.spacer(4f));
        document.add(buildTermsText());

        if (a.getNotes() != null && !a.getNotes().isBlank()) {
            document.add(PdfDocumentBuilder.spacer(6f));
            Paragraph notesPara = new Paragraph("Observações de Entrega: " + a.getNotes(), PdfTheme.smallFont());
            notesPara.setSpacingAfter(4f);
            document.add(notesPara);
        }

        document.add(PdfDocumentBuilder.spacer(20f));

        // 6. Data de Emissão e Bloco de Assinaturas
        Paragraph issueDate = new Paragraph("Emitido em: " + LocalDate.now().format(DATE_FMT), PdfTheme.smallFont());
        issueDate.setAlignment(Element.ALIGN_RIGHT);
        document.add(issueDate);
        document.add(PdfDocumentBuilder.spacer(12f));

        document.add(SignatureBlockRenderer.build(
                "O(A) Responsável de Higiene e Segurança (HST)",
                "O(A) Colaborador(a) / Recebedor(a)"
        ));
    }

    private Paragraph buildSectionTitle(String title) {
        Paragraph p = new Paragraph(title, PdfTheme.subtitleFont());
        p.setSpacingBefore(4f);
        p.setSpacingAfter(2f);
        return p;
    }

    private PdfPTable buildEmployeeInfoTable(Employee emp) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{22f, 28f, 22f, 28f});
        } catch (Exception ignored) {}

        addKeyValCell(table, "Nome Completo:", emp.getName());
        addKeyValCell(table, "Nº Funcionário:", emp.getEmployeeNumber() != null ? emp.getEmployeeNumber() : "—");
        addKeyValCell(table, "Departamento:", emp.getDepartment() != null ? emp.getDepartment() : "—");
        addKeyValCell(table, "NUIT / Registo:", emp.getTaxId() != null ? emp.getTaxId() : "—");

        return table;
    }

    private PdfPTable buildEquipmentTable(Equipment eq, WorkerEquipmentAssignment a) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{22f, 28f, 22f, 28f});
        } catch (Exception ignored) {}

        addKeyValCell(table, "Código / Ref:", eq.getCode());
        addKeyValCell(table, "Designação:", eq.getName());
        addKeyValCell(table, "Categoria:", eq.getCategory() != null ? eq.getCategory().getLabel() : "—");
        addKeyValCell(table, "Quantidade:", String.valueOf(a.getQuantity()));
        addKeyValCell(table, "Tamanho / Calibre:", a.getSizeOrSpec() != null && !a.getSizeOrSpec().isBlank() ? a.getSizeOrSpec() : "Padrão");
        addKeyValCell(table, "Nº de Série / Lote:", a.getSerialNumber() != null && !a.getSerialNumber().isBlank() ? a.getSerialNumber() : "—");

        return table;
    }

    private PdfPTable buildDatesTable(Equipment eq, WorkerEquipmentAssignment a) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{25f, 25f, 25f, 25f});
        } catch (Exception ignored) {}

        addHeaderCell(table, "Data de Entrega");
        addHeaderCell(table, "Periodicidade / Validade");
        addHeaderCell(table, "Data Limite de Expiração");
        addHeaderCell(table, "Aviso Prévio");

        String deliveryStr = a.getAssignedDate() != null ? a.getAssignedDate().format(DATE_FMT) : "—";
        String periodStr = eq.getPeriodValue() + " " + (eq.getPeriodUnit() != null ? eq.getPeriodUnit().getLabel() : "dias");
        String expiryStr = a.getExpiryDate() != null ? a.getExpiryDate().format(DATE_FMT) : "—";
        String warnStr = eq.getWarningDaysBefore() + " dias antes";

        addDataCell(table, deliveryStr);
        addDataCell(table, periodStr);
        addDataCell(table, expiryStr);
        addDataCell(table, warnStr);

        return table;
    }

    private Paragraph buildTermsText() {
        String text = "Pelo presente termo, o(a) colaborador(a) acima identificado(a) declara ter recebido da entidade empregadora, "
                + "a título de fornecimento de serviço, o(s) Equipamento(s) de Proteção Individual (EPI) e ferramentas acima discriminado(s), "
                + "em perfeito estado de uso e conservação, assumindo expressamente a obrigação de:\n"
                + "a) Fazer uso obrigatório do(s) equipamento(s) estritamente durante a jornada de trabalho e de acordo com as normas de HST;\n"
                + "b) Responsabilizar-se pela sua guarda, higienização e manutenção preventiva diária;\n"
                + "c) Comunicar imediatamente ao setor de Qualidade / HST qualquer avaria, extravio, dano ou desgaste excessivo;\n"
                + "d) Proceder à sua substituição dentro do prazo de validade estipulado ou devolver ao final do vínculo de utilização.";

        Paragraph p = new Paragraph(text, PdfTheme.bodyFont());
        p.setLeading(14f);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private void addKeyValCell(PdfPTable table, String key, String val) {
        PdfPCell c1 = new PdfPCell(new Phrase(key, PdfTheme.boldFont()));
        c1.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        c1.setBorderColor(PdfTheme.BORDER);
        c1.setPadding(5f);

        PdfPCell c2 = new PdfPCell(new Phrase(val != null ? val : "—", PdfTheme.bodyFont()));
        c2.setBorderColor(PdfTheme.BORDER);
        c2.setPadding(5f);

        table.addCell(c1);
        table.addCell(c2);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
