package mz.multicore.erp.modules.intelligence.service;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiEvidenceDTO;
import mz.multicore.erp.modules.intelligence.dto.AiProfessionalDocumentRequest;
import mz.multicore.erp.modules.printing.CompanyHeaderRenderer;
import mz.multicore.erp.modules.printing.PdfDocumentBuilder;
import mz.multicore.erp.modules.printing.PdfTheme;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@Service
public class AiManagementReportService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private static final Set<String> ALLOWED_TYPES = Set.of("RELATORIO", "MEMORANDO", "PROPOSTA", "CARTA", "ACTA", "PLANO_ACCAO");

    public AiManagementReportService(CompanyRepository companyRepository, AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    public byte[] render(AiAssistantResponse response) {
        PermissionGuard.requireManagerOrAdmin("gerar o relatório do Assistente de Gestão");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) throw new BusinessRuleException("Seleccione uma empresa para gerar o relatório.");
        if (response == null || response.answer() == null || response.answer().isBlank()) {
            throw new BusinessRuleException("Execute uma análise antes de gerar o relatório.");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        byte[] pdf = PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Relatório de Gestão Assistido por IA", "RG-IA"));
            Paragraph meta = new Paragraph("Gerado em " + response.generatedAt().format(DATE_TIME), PdfTheme.smallFont());
            meta.setSpacingAfter(12f);
            doc.add(meta);
            doc.add(section("Resumo executivo"));
            Paragraph answer = new Paragraph(response.answer(), PdfTheme.bodyFont());
            answer.setLeading(15f);
            answer.setSpacingAfter(14f);
            doc.add(answer);
            doc.add(section("Indicadores e evidências"));
            doc.add(evidenceTable(response));
            doc.add(PdfDocumentBuilder.spacer(12f));
            doc.add(section("Nota de utilização"));
            doc.add(new Paragraph(response.notice(), PdfTheme.bodyFont()));
            doc.add(new Paragraph("Documento informativo. Não substitui documentos fiscais, contabilísticos ou decisões operacionais aprovadas.", PdfTheme.smallFont()));
        });
        auditLogService.logCurrent("AI_MANAGEMENT_REPORT_GENERATED",
                "Relatório de gestão gerado; indicadores=" + response.evidence().size());
        return pdf;
    }

    public byte[] renderProfessionalDocument(AiProfessionalDocumentRequest request) {
        PermissionGuard.requireManagerOrAdmin("gerar documentos profissionais com o Assistente de Gestão");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) throw new BusinessRuleException("Seleccione uma empresa para gerar o documento.");
        if (request == null || request.title() == null || request.instructions() == null) {
            throw new BusinessRuleException("Indique o tipo, título e conteúdo pretendido.");
        }
        String type = request.documentType().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessRuleException("Este tipo de documento deve ser preparado e confirmado no módulo operacional correspondente.");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        String title = request.title().trim();
        String instructions = request.instructions().trim();
        byte[] pdf = PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, displayType(type), "DOCUMENTO INFORMATIVO"));
            Paragraph heading = new Paragraph(title, PdfTheme.titleFont());
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingAfter(14f);
            doc.add(heading);
            doc.add(section("Objectivo e enquadramento"));
            Paragraph body = new Paragraph(instructions, PdfTheme.bodyFont());
            body.setLeading(15f);
            body.setSpacingAfter(14f);
            doc.add(body);
            if (request.analysis() != null && request.analysis().answer() != null && !request.analysis().answer().isBlank()) {
                doc.add(section("Análise de suporte"));
                doc.add(new Paragraph(request.analysis().answer(), PdfTheme.bodyFont()));
                if (!request.analysis().evidence().isEmpty()) {
                    doc.add(PdfDocumentBuilder.spacer(8f));
                    doc.add(evidenceTable(request.analysis()));
                }
            }
            doc.add(PdfDocumentBuilder.spacer(18f));
            doc.add(section("Conclusão e próximos passos"));
            doc.add(new Paragraph("Rever o conteúdo, confirmar os dados apresentados e aprovar as acções no módulo responsável.", PdfTheme.bodyFont()));
            doc.add(PdfDocumentBuilder.spacer(28f));
            doc.add(new Paragraph("Preparado por: ______________________________    Data: ____/____/________", PdfTheme.bodyFont()));
            doc.add(PdfDocumentBuilder.spacer(18f));
            doc.add(new Paragraph("Documento informativo gerado com assistência de IA. Não possui valor fiscal ou transaccional.", PdfTheme.smallFont()));
        });
        auditLogService.logCurrent("AI_PROFESSIONAL_DOCUMENT_GENERATED", "Tipo=" + type + "; tituloCaracteres=" + title.length());
        return pdf;
    }

    private static String displayType(String type) {
        return switch (type) {
            case "MEMORANDO" -> "Memorando";
            case "PROPOSTA" -> "Proposta Profissional";
            case "CARTA" -> "Carta Empresarial";
            case "ACTA" -> "Acta";
            case "PLANO_ACCAO" -> "Plano de Acção";
            default -> "Relatório Profissional";
        };
    }

    private static Paragraph section(String title) {
        Paragraph paragraph = new Paragraph(title, PdfTheme.subtitleFont());
        paragraph.setSpacingBefore(4f);
        paragraph.setSpacingAfter(7f);
        return paragraph;
    }

    private static PdfPTable evidenceTable(AiAssistantResponse response) {
        PdfPTable table = new PdfPTable(new float[]{48f, 22f, 30f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : new String[]{"Indicador", "Valor", "Período"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PdfTheme.tableHeaderFont()));
            cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
            cell.setBorderColor(PdfTheme.BORDER);
            cell.setPadding(6f);
            table.addCell(cell);
        }
        for (AiEvidenceDTO item : response.evidence()) {
            addCell(table, item.label(), Element.ALIGN_LEFT);
            addCell(table, item.value() == null ? "0" : item.value().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT);
            addCell(table, item.period(), Element.ALIGN_LEFT);
        }
        return table;
    }

    private static void addCell(PdfPTable table, String value, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, PdfTheme.bodyFont()));
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        table.addCell(cell);
    }
}
