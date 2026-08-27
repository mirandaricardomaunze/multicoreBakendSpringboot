-- V18 — Imagem por produto (catálogo POS em cards).
-- Bytes da imagem (thumbnail ~320px) guardados na BD para portabilidade multi-utilizador.
ALTER TABLE products ADD COLUMN image_data bytea;
