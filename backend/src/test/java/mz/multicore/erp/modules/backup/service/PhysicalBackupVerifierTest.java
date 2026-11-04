package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** "Verificar Backup" também aceita o backup físico (.dump e .dump.enc). */
class PhysicalBackupVerifierTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";
    private static final String TOC = """
            ;
            ; Archive created at 2026-11-03 18:29:02 UTC
            ;     dbname: multicore
            ;
            3456; 1259 16999 INDEX public idx_invoices_company multicore
            3700; 0 16390 TABLE DATA public users multicore
            3701; 0 16400 TABLE DATA public invoices multicore
            3800; 0 0 SEQUENCE SET public invoices_id_seq multicore
            """;

    @TempDir Path dir;

    private final BackupFileCipher cipher = new BackupFileCipher(KEY);
    private final PostgresBackupProcessRunner runner = mock(PostgresBackupProcessRunner.class);

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("PV-01: Dump em claro é listado pelo pg_restore e resumido sem empresa")
    void plainDumpIsListed() throws Exception {
        Path dump = Files.writeString(dir.resolve("multicore_1.dump"), "dados");
        when(runner.runForOutput(any(), anyString())).thenReturn(TOC);

        BackupVerificationDTO result = service(cipher).verifyBackup(dump.toString());

        assertThat(result.companyId()).isNull();
        assertThat(result.generatedAt()).isEqualTo("2026-11-03 18:29:02 UTC");
        assertThat(result.itemCounts()).containsEntry("tabelas", 2).containsEntry("indices", 1)
                .containsEntry("sequencias", 1);
    }

    @Test
    @DisplayName("PV-02: Dump cifrado é decifrado só durante a verificação")
    void encryptedDumpIsDecryptedTemporarily() throws Exception {
        Path plain = Files.writeString(dir.resolve("multicore_2.dump"), "dados do dump");
        Path encrypted = dir.resolve("multicore_2.dump.enc");
        cipher.encrypt(plain, encrypted);
        Files.delete(plain);
        when(runner.runForOutput(any(), anyString())).thenAnswer(call -> {
            List<String> command = call.getArgument(0);
            assertThat(Files.readString(Path.of(command.get(command.size() - 1)))).isEqualTo("dados do dump");
            return TOC;
        });

        BackupVerificationDTO result = service(cipher).verifyBackup(encrypted.toString());

        assertThat(result.fileName()).isEqualTo("multicore_2.dump.enc");
        try (var files = Files.list(dir)) {
            assertThat(files.map(file -> file.getFileName().toString())).containsExactly("multicore_2.dump.enc");
        }
    }

    @Test
    @DisplayName("PV-03: Dump que o pg_restore recusa não passa na verificação")
    void unreadableDumpFails() throws Exception {
        Path dump = Files.writeString(dir.resolve("multicore_3.dump"), "lixo");
        when(runner.runForOutput(any(), anyString()))
                .thenThrow(new BusinessRuleException("pg_restore falhou: input file does not appear to be a valid archive"));

        assertThatThrownBy(() -> service(cipher).verifyBackup(dump.toString()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pg_restore falhou");
    }

    @Test
    @DisplayName("PV-04: Dump cifrado com outra chave é recusado")
    void wrongKeyFails() throws Exception {
        Path plain = Files.writeString(dir.resolve("multicore_4.dump"), "dados");
        Path encrypted = dir.resolve("multicore_4.dump.enc");
        new BackupFileCipher("ZmVkY2JhOTg3NjU0MzIxMHp5eHd2dXRzcnFwb25tbGs=").encrypt(plain, encrypted);

        assertThatThrownBy(() -> service(cipher).verifyBackup(encrypted.toString()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Backup ilegível");
    }

    private BackupVerificationService service(BackupFileCipher activeCipher) {
        PostgresBackupCommandBuilder commands = new PostgresBackupCommandBuilder("");
        BackupEncryption encryption = new BackupEncryption(activeCipher, commands, runner);
        return new BackupVerificationService(new BackupContentReader(activeCipher),
                new PhysicalBackupVerifier(commands, runner, encryption));
    }
}
