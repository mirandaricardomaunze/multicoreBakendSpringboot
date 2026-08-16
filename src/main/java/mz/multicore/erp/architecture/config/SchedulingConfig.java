package mz.multicore.erp.architecture.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa o agendamento por cron ({@code @Scheduled}) na aplicação — usado pela cópia de segurança
 * física automática ({@code ScheduledBackupService}). O trabalho em si só actua quando
 * {@code backup.schedule.enabled=true} (perfis desktop/prod, PostgreSQL).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
