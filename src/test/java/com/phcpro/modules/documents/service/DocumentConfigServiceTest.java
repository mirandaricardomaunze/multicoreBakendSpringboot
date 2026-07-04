package com.phcpro.modules.documents.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.documents.dto.DocumentColumnsDTO;
import com.phcpro.modules.documents.model.DocumentColumnConfig;
import com.phcpro.modules.documents.model.DocumentType;
import com.phcpro.modules.documents.repository.DocumentColumnConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do DocumentConfigService (colunas configuráveis por tipo de documento).
 * Cobre DC-01..DC-05 e DC-07 (independência entre tipos) do harness. Dependências mockadas.
 */
class DocumentConfigServiceTest {

    private static final Long COMPANY_ID = 1L;

    private DocumentColumnConfigRepository repository;
    private AuditLogService auditLogService;
    private DocumentConfigService service;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentColumnConfigRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new DocumentConfigService(repository, auditLogService);

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // DC-01
    @Test
    void getColumns_semConfig_devolveTodas() {
        when(repository.findByCompanyIdAndDocumentType(COMPANY_ID, DocumentType.COMMERCIAL))
                .thenReturn(Optional.empty());

        DocumentColumnsDTO cols = service.getColumns(COMPANY_ID, DocumentType.COMMERCIAL);

        assertTrue(cols.barcode());
        assertTrue(cols.subtotal());
    }

    // DC-02
    @Test
    void save_desligaAlgumas_getColumnsReflecte() {
        when(repository.findByCompanyIdAndDocumentType(COMPANY_ID, DocumentType.COMMERCIAL))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentColumnConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentColumnsDTO input = new DocumentColumnsDTO(false, true, true, false, true, true, true, true, null);
        DocumentColumnsDTO saved = service.save(COMPANY_ID, DocumentType.COMMERCIAL, input);

        assertFalse(saved.barcode());
        assertFalse(saved.expiry());
        assertTrue(saved.description());
        verify(auditLogService).logCurrent(eq("DOCUMENT_COLUMNS_UPDATE"), any());
    }

    // DC-03
    @Test
    void save_esconderTodas_lancaBusinessRule() {
        DocumentColumnsDTO none = new DocumentColumnsDTO(false, false, false, false, false, false, false, false, null);

        assertThrows(BusinessRuleException.class, () -> service.save(COMPANY_ID, DocumentType.COMMERCIAL, none));
        verify(repository, never()).save(any());
    }

    // DC-04
    @Test
    void save_semPerfilManagerOuAdmin_bloqueado() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> service.save(COMPANY_ID, DocumentType.COMMERCIAL, DocumentColumnsDTO.all()));
        verify(repository, never()).save(any());
    }

    // DC-05
    @Test
    void getColumnsESave_empresaDiferenteDaActiva_bloqueado() {
        Long other = 99L;

        assertThrows(BusinessRuleException.class, () -> service.getColumns(other, DocumentType.COMMERCIAL));
        assertThrows(BusinessRuleException.class,
                () -> service.save(other, DocumentType.COMMERCIAL, DocumentColumnsDTO.all()));
    }

    // DC-07 — os tipos são independentes: guardar POS_RECEIPT não afecta COMMERCIAL.
    @Test
    void save_posReceipt_naoAfecta_comercial() {
        when(repository.findByCompanyIdAndDocumentType(COMPANY_ID, DocumentType.POS_RECEIPT))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentColumnConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        // COMMERCIAL não tem config guardada → all().
        when(repository.findByCompanyIdAndDocumentType(COMPANY_ID, DocumentType.COMMERCIAL))
                .thenReturn(Optional.empty());

        service.save(COMPANY_ID, DocumentType.POS_RECEIPT,
                new DocumentColumnsDTO(false, false, true, false, true, false, false, true, "Volte sempre"));

        // A leitura da config COMMERCIAL continua a devolver tudo visível (independente do POS).
        DocumentColumnsDTO commercial = service.getColumns(COMPANY_ID, DocumentType.COMMERCIAL);
        assertTrue(commercial.barcode());
        assertTrue(commercial.unitPrice());
    }
}
