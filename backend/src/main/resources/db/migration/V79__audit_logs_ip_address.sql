-- V79: Adicionar coluna ip_address na tabela audit_logs para trilha de auditoria forense
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS ip_address VARCHAR(50);
