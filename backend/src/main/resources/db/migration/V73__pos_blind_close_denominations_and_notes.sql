-- V73: Suporte a notas/justificação e discriminação de notas/moedas no Fecho Cego de Caixa (POS)
ALTER TABLE till_sessions ADD COLUMN IF NOT EXISTS closing_notes VARCHAR(500);
ALTER TABLE till_sessions ADD COLUMN IF NOT EXISTS cash_breakdown_json TEXT;
