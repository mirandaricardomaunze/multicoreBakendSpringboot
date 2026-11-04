package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifica um backup físico (.dump ou .dump.enc): o pg_restore só lista o conteúdo de um dump legível.
 * O dump cobre a base inteira, por isso o resultado não tem empresa.
 */
@Component
public class PhysicalBackupVerifier {

    private static final Pattern CREATED_AT = Pattern.compile("(?m)^;\\s*Archive created at (.+)$");

    private final PostgresBackupCommandBuilder commandBuilder;
    private final PostgresBackupProcessRunner processRunner;
    private final BackupEncryption encryption;

    public PhysicalBackupVerifier(PostgresBackupCommandBuilder commandBuilder,
                                  PostgresBackupProcessRunner processRunner,
                                  BackupEncryption encryption) {
        this.commandBuilder = commandBuilder;
        this.processRunner = processRunner;
        this.encryption = encryption;
    }

    static boolean supports(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".dump") || name.endsWith(".dump" + BackupFileCipher.ENCRYPTED_SUFFIX);
    }

    BackupVerificationDTO verify(File file) {
        boolean encrypted = file.getName().toLowerCase(Locale.ROOT).endsWith(BackupFileCipher.ENCRYPTED_SUFFIX);
        AtomicReference<String> toc = new AtomicReference<>();
        if (encrypted) {
            encryption.withDecrypted(file, plain -> toc.set(list(plain)));
        } else {
            toc.set(list(file));
        }
        return summarize(file, toc.get());
    }

    private String list(File dump) {
        return processRunner.runForOutput(commandBuilder.buildListCommand(dump.getAbsolutePath()), "pg_restore");
    }

    static BackupVerificationDTO summarize(File file, String toc) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("tabelas", count(toc, " TABLE DATA "));
        counts.put("indices", count(toc, " INDEX "));
        counts.put("sequencias", count(toc, " SEQUENCE SET "));
        Matcher created = CREATED_AT.matcher(toc);
        String generatedAt = created.find() ? created.group(1).trim()
                : Instant.ofEpochMilli(file.lastModified()).toString();
        return new BackupVerificationDTO(file.getName(), null, generatedAt, counts.get("tabelas"), counts);
    }

    private static int count(String toc, String entryType) {
        int total = 0;
        for (String line : toc.split("\\R")) {
            if (!line.startsWith(";") && line.contains(entryType)) total++;
        }
        return total;
    }
}
