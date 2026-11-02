package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/** Ver docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md. */
class BackupAtRestHarnessTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";
    private static final long HOUR = 60L * 60 * 1000;

    @TempDir Path dir;

    private final BackupFileCipher cipher = new BackupFileCipher(KEY);

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SC-30: Se o pg_restore recusa o dump, a cópia em claro fica")
    void sc30_failedSealKeepsPlain() throws Exception {
        Path plain = Files.writeString(dir.resolve("a.dump"), "dados");
        PostgresBackupProcessRunner runner = mock(PostgresBackupProcessRunner.class);
        doThrow(new BusinessRuleException("falhou")).when(runner).runProcess(any(), anyString());
        BackupEncryption encryption = new BackupEncryption(cipher, new PostgresBackupCommandBuilder(""), runner);

        assertThatThrownBy(() -> encryption.seal(plain.toFile())).isInstanceOf(BusinessRuleException.class);

        assertThat(plain).exists();
        assertThat(dir.resolve("a.dump.enc")).doesNotExist();
    }

    @Test
    @DisplayName("SC-30: Se a cifra funciona, só fica a cópia cifrada")
    void sc30_successfulSealRemovesPlain() throws Exception {
        Path plain = Files.writeString(dir.resolve("b.dump"), "dados");
        BackupEncryption encryption = new BackupEncryption(cipher, new PostgresBackupCommandBuilder(""),
                mock(PostgresBackupProcessRunner.class));

        assertThat(encryption.seal(plain.toFile())).exists();
        assertThat(plain).doesNotExist();
    }

    @Test
    @DisplayName("SC-31: Backups antigos ficam cifrados, legíveis e com a data original")
    void sc31_oldBackupsSealed() throws Exception {
        Path dump = old("velho.dump", "conteudo-dump");
        Path json = old("velho.json", "{}");
        long original = dump.toFile().lastModified();

        assertThat(sealer().sealPlainFiles(System.currentTimeMillis())).isEqualTo(2);

        assertThat(dump).doesNotExist();
        assertThat(json).doesNotExist();
        Path sealed = dir.resolve("velho.dump.enc");
        assertThat(new String(cipher.decryptToMemory(sealed), StandardCharsets.UTF_8)).isEqualTo("conteudo-dump");
        assertThat(Files.readAllBytes(sealed)).isNotEqualTo("conteudo-dump".getBytes(StandardCharsets.UTF_8));
        assertThat(sealed.toFile().lastModified()).isEqualTo(original);
    }

    @Test
    @DisplayName("SC-31: Ficheiro recente não é tocado: pode estar a ser escrito")
    void sc31_recentFileSkipped() throws Exception {
        Path fresh = Files.writeString(dir.resolve("novo.dump"), "a escrever");

        assertThat(sealer().sealPlainFiles(System.currentTimeMillis())).isZero();
        assertThat(fresh).exists();
    }

    @Test
    @DisplayName("SC-31: Cifra interrompida é refeita; a cópia em claro não se perde")
    void sc31_brokenEncryptedCopyRedone() throws Exception {
        Path dump = old("meio.dump", "conteudo");
        Files.writeString(dir.resolve("meio.dump.enc"), "cortado");

        assertThat(sealer().sealPlainFiles(System.currentTimeMillis())).isEqualTo(1);

        assertThat(dump).doesNotExist();
        assertThat(new String(cipher.decryptToMemory(dir.resolve("meio.dump.enc")), StandardCharsets.UTF_8))
                .isEqualTo("conteudo");
    }

    @Test
    @DisplayName("SC-31: Sem chave, nada muda")
    void sc31_noKeyLeavesFiles() throws Exception {
        Path dump = old("sem.dump", "conteudo");
        BackupPlainFileSealer sealer = new BackupPlainFileSealer(new BackupFileCipher(""),
                mock(AuditLogService.class), dir.toString());

        assertThat(sealer.sealPlainFiles(System.currentTimeMillis())).isZero();
        assertThat(dump).exists();
    }

    @Test
    @DisplayName("SC-32: O backup lógico é gravado cifrado e continua verificável")
    void sc32_logicalBackupEncrypted() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("companyId", 7L);
        data.put("generatedAt", "2026-11-01T10:00:00");
        BackupVerificationService.REQUIRED_COLLECTIONS.forEach(section -> data.put(section, List.of()));
        data.put("clients", List.of("Cliente Secreto"));

        String path = new BackupExecutionService(dir.toString(), cipher).writeBackupFile(7L, data);

        assertThat(path).endsWith(".json.enc");
        assertThat(Files.list(dir).filter(file -> file.toString().endsWith(".json"))).isEmpty();
        assertThat(new String(Files.readAllBytes(Path.of(path)), StandardCharsets.ISO_8859_1))
                .doesNotContain("Cliente Secreto");
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(7L);
        BackupVerificationDTO result = new BackupVerificationService(new BackupContentReader(cipher)).verifyBackup(path);
        assertThat(result.itemCounts()).containsEntry("clients", 1);
    }

    private BackupPlainFileSealer sealer() {
        return new BackupPlainFileSealer(cipher, mock(AuditLogService.class), dir.toString());
    }

    private Path old(String name, String content) throws Exception {
        Path file = Files.writeString(dir.resolve(name), content);
        file.toFile().setLastModified(System.currentTimeMillis() - 3 * HOUR);
        return file;
    }
}
