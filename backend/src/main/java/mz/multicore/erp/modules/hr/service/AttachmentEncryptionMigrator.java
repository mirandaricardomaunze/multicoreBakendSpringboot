package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/** Cifra no arranque os anexos gravados antes de haver chave. Backups antigos continuam em claro. */
@Component
public class AttachmentEncryptionMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AttachmentEncryptionMigrator.class);

    private final OccupationalHealthExamRepository examRepository;
    private final AttachmentCrypto attachmentCrypto;
    private final TransactionOperations transactions;

    public AttachmentEncryptionMigrator(OccupationalHealthExamRepository examRepository,
                                        AttachmentCrypto attachmentCrypto,
                                        TransactionOperations transactions) {
        this.examRepository = examRepository;
        this.attachmentCrypto = attachmentCrypto;
        this.transactions = transactions;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!attachmentCrypto.isEnabled()) return;
        int encrypted = 0;
        for (Long id : examRepository.findIdsWithAttachment()) {
            // Um anexo por transacção, para não carregar todos os ficheiros para a memória.
            Boolean changed = transactions.execute(status -> examRepository.findById(id).map(exam -> {
                if (AttachmentCrypto.isEncrypted(exam.getAttachmentData())) return false;
                exam.setAttachmentData(attachmentCrypto.protect(exam.getAttachmentData()));
                examRepository.save(exam);
                return true;
            }).orElse(false));
            if (Boolean.TRUE.equals(changed)) encrypted++;
        }
        if (encrypted > 0) log.info("Anexos clínicos cifrados no arranque: {}", encrypted);
    }
}
