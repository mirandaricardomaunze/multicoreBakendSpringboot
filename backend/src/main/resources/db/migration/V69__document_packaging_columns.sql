ALTER TABLE document_column_config
    ADD COLUMN show_packages BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE document_column_config
    ADD COLUMN show_box_percentage BOOLEAN NOT NULL DEFAULT TRUE;
