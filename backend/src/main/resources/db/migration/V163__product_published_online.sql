-- "Publicar na loja online" per product (docs/LOJA_ONLINE_SPEC.md §3.1, LO-02): off by default, so no existing
-- product shows in the public shop until someone publishes it.
ALTER TABLE products ADD COLUMN IF NOT EXISTS published_online BOOLEAN NOT NULL DEFAULT FALSE;
