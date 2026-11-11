-- Goods lost on the way (broken, stolen) during a stock transfer: recorded on the line, not returned to origin.
ALTER TABLE stock_transfer_lines ADD COLUMN IF NOT EXISTS lost_quantity NUMERIC(12, 3);
