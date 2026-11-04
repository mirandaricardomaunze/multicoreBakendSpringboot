package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PostgresBackupProcessRunner {

    private static final long PROCESS_TIMEOUT_MINUTES = 10;

    private final String dbPassword;

    public PostgresBackupProcessRunner(@Value("${spring.datasource.password:}") String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public void runProcess(List<String> command, String tool) {
        runForOutput(command, tool);
    }

    /** Corre o comando e devolve o que escreveu; falha com a saída quando o código não é zero. */
    public String runForOutput(List<String> command, String tool) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        if (dbPassword != null && !dbPassword.isBlank()) {
            pb.environment().put("PGPASSWORD", dbPassword);
        }
        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessRuleException(tool + " excedeu o tempo limite e foi cancelado.");
            }
            if (process.exitValue() != 0) {
                throw new BusinessRuleException(tool + " falhou: " + output.trim());
            }
            return output;
        } catch (IOException e) {
            throw new BusinessRuleException(
                    "Não foi possível executar " + tool + ". Verifique se está instalado e no PATH "
                            + "(ou configure backup.pg-bin-dir). Detalhe: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException(tool + " foi interrompido.");
        }
    }
}
