-- Cash received at the counter for a receipt enters the drawer of the operator's open POS session
-- (MOVIMENTOS_COMERCIAIS.md, open point 10): two new till movement types.

ALTER TABLE till_movements DROP CONSTRAINT IF EXISTS till_movements_movement_type_check;
ALTER TABLE till_movements ADD CONSTRAINT till_movements_movement_type_check
    CHECK (movement_type IN ('SALE', 'SUPRIMENTO', 'SANGRIA', 'REFUND', 'COUNTER_RECEIPT', 'COUNTER_RECEIPT_REVERSAL'));
