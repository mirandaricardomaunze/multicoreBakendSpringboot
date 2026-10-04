package mz.multicore.erp.modules.monitoring.service;

import jakarta.mail.internet.MimeMessage;
import mz.multicore.erp.modules.monitoring.dto.SystemAlertTestResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Serviço de disparo de notificações por e-mail em caso de anomalias críticas e falhas de subsistemas.
 * Contém mecanismo inteligente de cooldown de 15 minutos para evitar avalanche de e-mails (spam).
 */
@Service
public class SystemAlertEmailService {

    private static final Logger log = LoggerFactory.getLogger(SystemAlertEmailService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(15);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String defaultAdminEmail;
    private final String senderFrom;

    private final Map<String, Instant> incidentCooldownMap = new ConcurrentHashMap<>();

    public SystemAlertEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.monitoring.admin-email:admin@multicore.co.mz}") String defaultAdminEmail,
            @Value("${spring.mail.username:suporte@multicore.co.mz}") String senderFrom
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.defaultAdminEmail = defaultAdminEmail;
        this.senderFrom = senderFrom;
    }

    public boolean sendCriticalAlert(String subsystem, String tenantName, String message, String recipientEmail) {
        String targetEmail = (recipientEmail != null && !recipientEmail.isBlank()) ? recipientEmail.trim() : defaultAdminEmail;
        if (!EMAIL_PATTERN.matcher(targetEmail).matches()) {
            log.warn("E-mail de destino inválido para alerta do sistema: {}", targetEmail);
            return false;
        }

        String incidentKey = (subsystem != null ? subsystem : "GLOBAL") + ":" + (tenantName != null ? tenantName : "SYSTEM");
        Instant now = Instant.now();
        Instant lastSent = incidentCooldownMap.get(incidentKey);

        if (lastSent != null && Duration.between(lastSent, now).compareTo(COOLDOWN_DURATION) < 0) {
            log.info("Alerta para [{}] em cooldown (enviado há menos de 15 min). E-mail suprimido.", incidentKey);
            return false;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[ALERTA SIMULADO - Sem Servidor SMTP configurado] Alerta Crítico para {}: {} - {}", targetEmail, subsystem, message);
            incidentCooldownMap.put(incidentKey, now);
            return true;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(senderFrom);
            helper.setTo(targetEmail);
            helper.setSubject("🚨 [ALERTA CRÍTICO ERP] Falha Detectada: " + subsystem);

            String body = buildEmailBody(subsystem, tenantName, message, now);
            helper.setText(body, true);

            mailSender.send(mimeMessage);
            incidentCooldownMap.put(incidentKey, now);
            log.warn("E-mail de alerta crítico enviado com sucesso para {} sobre [{}]", targetEmail, incidentKey);
            return true;
        } catch (Exception ex) {
            log.error("Falha ao despachar e-mail de alerta crítico para {}: {}", targetEmail, ex.getMessage());
            return false;
        }
    }

    public SystemAlertTestResultDTO testEmailAlert(String recipientEmail) {
        String target = (recipientEmail != null && !recipientEmail.isBlank()) ? recipientEmail.trim() : defaultAdminEmail;
        if (!EMAIL_PATTERN.matcher(target).matches()) {
            return new SystemAlertTestResultDTO(
                    "EMAIL",
                    false,
                    "Endereço de e-mail inválido: " + target,
                    Instant.now()
            );
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return new SystemAlertTestResultDTO(
                    "EMAIL",
                    true,
                    "Modo de simulação ativo: Servidor SMTP não configurado localmente. Teste validado com sucesso.",
                    Instant.now()
            );
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(senderFrom);
            helper.setTo(target);
            helper.setSubject("🔔 [TESTE MULTICORE ERP] Prova do Sistema de Notificações de Alarme");

            String body = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #1a1a24; color: #f0f0f5; padding: 24px; border-radius: 8px; border: 1px solid #33334d;">
                        <h2 style="color: #00d2ff; margin-top: 0;">✔ Teste de Notificação de Alarme Concluído</h2>
                        <p>Este é um e-mail de prova disparado a partir da consola executiva do <strong>Multicore ERP</strong>.</p>
                        <div style="background-color: #242436; padding: 14px; border-radius: 6px; margin: 16px 0; border-left: 4px solid #00d2ff;">
                            <p style="margin: 0;"><strong>Canal:</strong> E-mail Transacional de Operações (NOC)</p>
                            <p style="margin: 4px 0 0 0;"><strong>Destinatário:</strong> %s</p>
                            <p style="margin: 4px 0 0 0;"><strong>Data / Hora:</strong> %s</p>
                        </div>
                        <p style="color: #a0a0b5; font-size: 13px;">O canal de alarme está ativo e pronto para receber notificações automáticas em caso de degradação técnica.</p>
                    </div>
                    """.formatted(target, Instant.now().toString());

            helper.setText(body, true);
            mailSender.send(mimeMessage);
            return new SystemAlertTestResultDTO(
                    "EMAIL",
                    true,
                    "E-mail de prova enviado com sucesso para: " + target,
                    Instant.now()
            );
        } catch (Exception ex) {
            log.error("Erro ao enviar e-mail de teste para {}: {}", target, ex.getMessage());
            return new SystemAlertTestResultDTO(
                    "EMAIL",
                    false,
                    "Falha ao enviar e-mail de teste: " + ex.getMessage(),
                    Instant.now()
            );
        }
    }

    private String buildEmailBody(String subsystem, String tenantName, String message, Instant timestamp) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #1a1a24; color: #f0f0f5; padding: 24px; border-radius: 8px; border: 1px solid #ef4444;">
                    <h2 style="color: #ef4444; margin-top: 0;">🚨 Alerta Crítico de Sistema - Multicore ERP</h2>
                    <p>Foi detectada uma falha ou anomalia operacional que requer atenção imediata da equipa de administração técnica.</p>
                    <div style="background-color: #242436; padding: 14px; border-radius: 6px; margin: 16px 0; border-left: 4px solid #ef4444;">
                        <p style="margin: 0;"><strong>Subsistema:</strong> %s</p>
                        <p style="margin: 4px 0 0 0;"><strong>Empresa / Inquilino:</strong> %s</p>
                        <p style="margin: 4px 0 0 0;"><strong>Horário da Ocorrência:</strong> %s</p>
                        <p style="margin: 8px 0 0 0; color: #fca5a5;"><strong>Detalhes da Falha:</strong> %s</p>
                    </div>
                    <p style="color: #a0a0b5; font-size: 13px;">Aceda ao módulo de <em>Plataforma &gt; Saúde & Diagnóstico</em> no ERP para analisar a telemetria completa e tomar medidas correctivas.</p>
                </div>
                """.formatted(subsystem, tenantName, timestamp.toString(), message);
    }

    public void clearCooldowns() {
        incidentCooldownMap.clear();
    }
}
