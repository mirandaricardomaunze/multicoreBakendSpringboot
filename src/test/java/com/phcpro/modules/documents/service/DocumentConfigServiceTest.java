package com.phcpro.modules.documents.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.documents.dto.DocumentColumnsDTO;
import com.phcpro.modules.documents.model.DocumentColumnConfig;
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
 * Testes do DocumentConfigService (colunas configuráveis dos documentos comerciais).
 * Cobre DC-01..DC-05 do harness. Dependências mockadas.
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
        when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        DocumentColumnsDTO cols = service.getColumns(COMPANY_ID);

        assertTrue(cols.barcode());
        assertTrue(cols.reference());
        assertTrue(cols.description());
        assertTrue(cols.expiry());
        assertTrue(cols.quantity());
        assertTrue(cols.unitPrice());
        assertTrue(cols.tax());
        assertTrue(cols.subtotal());
    }

    // DC-02
    @Test
    void save_desligaAlgumas_getColumnsReflecte() {
        when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentColumnConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentColumnsDTO input = new DocumentColumnsDTO(false, true, true, false, true, true, true, true);
        DocumentColumnsDTO saved = service.save(COMPANY_ID, input);

        assertFalse(saved.barcode());
        assertFalse(saved.expiry());
        assertTrue(saved.description());
        assertTrue(saved.subtotal());
        verify(auditLogService).logCurrent(eq("DOCUMENT_COLUMNS_UPDATE"), any());
    }

    // DC-03
    @Test
    void save_esconderTodas_lancaBusinessRule() {
        DocumentColumnsDTO none = new DocumentColumnsDTO(false, false, false, false, false, false, false, false);

        assertThrows(BusinessRuleException.class, () -> service.save(COMPANY_ID, none));
        verify(repository, never()).save(any());
    }

    // DC-04
    @Test
    void save_semPerfilManagerOuAdmin_bloqueado() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
        DocumentColumnsDTO input = DocumentColumnsDTO.all();

        assertThrows(BusinessRuleException.class, () -> service.save(COMPANY_ID, input));
        verify(repository, never()).save(any());
    }

    // DC-05
    @Test
    void getColumnsESave_empresaDiferenteDaActiva_bloqueado() {
        Long other = 99L;

        assertThrows(BusinessRuleException.class, () -> service.getColumns(other));
        assertThrows(BusinessRuleException.class, () -> service.save(other, DocumentColumnsDTO.all()));
    }
}
