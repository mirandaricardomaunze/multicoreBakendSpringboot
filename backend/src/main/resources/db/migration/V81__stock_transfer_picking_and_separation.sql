-- Suporta o circuito de separação e picking em transferências entre armazéns
ALTER TABLE stock_transfers ALTER COLUMN status TYPE VARCHAR(30);
