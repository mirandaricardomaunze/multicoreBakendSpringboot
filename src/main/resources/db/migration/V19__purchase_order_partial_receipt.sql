-- Recepção parcial de encomenda a fornecedor.
-- Cada linha passa a registar quanto já foi recebido em stock; o em falta = quantity - received_quantity.
-- A encomenda ganha o estado intermédio PARTIALLY_RECEIVED (gerido pela aplicação, sem constraint nova).
ALTER TABLE purchase_order_lines
    ADD COLUMN received_quantity numeric(38,2) NOT NULL DEFAULT 0;
