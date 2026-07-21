-- Dados de contacto e logótipo da empresa, para os cabeçalhos de TODOS os documentos.
-- phone: telefone/contacto. logo: imagem reduzida (bytea, espelha products.image_data).
alter table companies add column if not exists phone varchar(40);
alter table companies add column if not exists logo bytea;
