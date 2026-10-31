package mz.multicore.erp.modules.monitoring.service;

import mz.multicore.erp.modules.monitoring.dto.SystemAlertTestResultDTO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Serviço de disparo de notificações por e-mail em caso de anomalias críticas e falhas de subsistemas.
 */
@Service
public class SystemAlertEmailService {

    private final SystemAlertMailDispatcher mailDispatcher;

    @Autowired
    public SystemAlertEmailService(SystemAlertMailDispatcher mailDispatcher) {
        this.mailDispatcher = mailDispatcher;
    }

    public SystemAlertEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.monitoring.admin-email:admin@multicore.co.mz}") String defaultAdminEmail,
            @Value("${spring.mail.username:suporte@multicore.co.mz}") String senderFrom
    ) {
        this(new SystemAlertMailDispatcher(mailSenderProvider, defaultAdminEmail, senderFrom));
    }

    public boolean sendCriticalAlert(String subsystem, String tenantName, String message, String recipientEmail) {
        return mailDispatcher.sendCriticalAlert(subsystem, tenantName, message, recipientEmail);
    }

    public SystemAlertTestResultDTO testEmailAlert(String recipientEmail) {
        return mailDispatcher.testEmailAlert(recipientEmail);
    }

    public void clearCooldowns() {
        mailDispatcher.clearCooldowns();
    }
}
