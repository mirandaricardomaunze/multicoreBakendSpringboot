ALTER TABLE products ADD COLUMN packages_per_box INTEGER NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN units_per_package INTEGER NOT NULL DEFAULT 1;

UPDATE products
SET packages_per_box = CASE WHEN units_per_box > 0 THEN units_per_box ELSE 1 END,
    units_per_package = 1;

ALTER TABLE products ADD CONSTRAINT chk_products_packages_per_box
    CHECK (packages_per_box > 0);
ALTER TABLE products ADD CONSTRAINT chk_products_units_per_package
    CHECK (units_per_package > 0);

