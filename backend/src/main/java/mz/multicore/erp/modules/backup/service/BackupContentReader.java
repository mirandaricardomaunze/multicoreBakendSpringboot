package mz.multicore.erp.modules.backup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/** Lê um backup lógico, em claro ou cifrado, sem escrever o conteúdo decifrado em disco. */
@Component
public class BackupContentReader {

    private static final String ENCRYPTED_JSON = ".json" + BackupFileCipher.ENCRYPTED_SUFFIX;

    private final BackupFileCipher cipher;

    public BackupContentReader(BackupFileCipher cipher) {
        this.cipher = cipher;
    }

    Map<String, Object> read(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".json") && !name.endsWith(ENCRYPTED_JSON)) {
            throw new BusinessRuleException("O ficheiro selecionado não é um backup JSON válido.");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return name.endsWith(ENCRYPTED_JSON)
                    ? mapper.readValue(cipher.decryptToMemory(file.toPath()), new TypeReference<>() {})
                    : mapper.readValue(file, new TypeReference<>() {});
        } catch (IOException e) {
            throw new BusinessRuleException("Falha ao verificar backup: " + e.getMessage());
        }
    }
}
