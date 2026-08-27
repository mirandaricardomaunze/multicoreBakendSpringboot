-- Preço de venda ao grosso por produto + quantidade mínima a partir da qual se aplica.
-- Ambos opcionais (NULL = produto sem preço de grosso, usa sempre o preço de retalho).
ALTER TABLE products ADD COLUMN IF NOT EXISTS wholesale_price   numeric(38,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS wholesale_min_qty numeric(12,3);
