-- Adiciona campos de motorista e matrícula à guia de transferência entre armazéns
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS driver_name VARCHAR(120);
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS vehicle_plate VARCHAR(30);
