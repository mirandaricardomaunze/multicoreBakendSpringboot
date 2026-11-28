-- Online shop delivery kept on the order (docs/LOJA_ONLINE_SPEC.md §3.5, LO-12): "Levantamento — {loja}" or
-- "Entrega — {zona}" and, for home delivery only, the checkout address. Desktop and API orders stay null.
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS fulfilment_label VARCHAR(120);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(255);
