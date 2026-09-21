ALTER TABLE treasury_accounts
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(16);

UPDATE treasury_accounts
SET account_type = CASE
    WHEN account_number IS NULL OR TRIM(account_number) = '' THEN 'CASH'
    ELSE 'BANK'
END
WHERE account_type IS NULL;

ALTER TABLE treasury_accounts
    ALTER COLUMN account_type SET DEFAULT 'CASH';

ALTER TABLE treasury_accounts
    ALTER COLUMN account_type SET NOT NULL;

ALTER TABLE treasury_accounts
    DROP CONSTRAINT IF EXISTS chk_treasury_account_type;

ALTER TABLE treasury_accounts
    ADD CONSTRAINT chk_treasury_account_type CHECK (account_type IN ('CASH', 'BANK'));
