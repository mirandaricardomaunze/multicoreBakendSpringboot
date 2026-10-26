-- V96: Normalizacao de caracteres e acentuacao em equipamentos e colaboradores
UPDATE employees SET name = 'João Silva' WHERE name LIKE 'Jo%' AND (name LIKE '%?%' OR name LIKE '%Ã%');
UPDATE app_users SET name = 'João Silva' WHERE username = 'joao';

-- Actualizar os equipamentos se a tabela existir
UPDATE quality_equipments SET name = 'Botas de Segurança Biqueira de Aço S3', description = 'Calçado de segurança certificado EN ISO 20345 para piso industrial e armazém.' WHERE code = 'EPI-BOTAS';
UPDATE quality_equipments SET name = 'Capacete de Proteção Industrial com Jugular', description = 'Capacete Classe G em polietileno com absorção de impacto frontal e lateral.' WHERE code = 'EPI-CAPACETE';
UPDATE quality_equipments SET name = 'Colete Refletor de Alta Visibilidade Classe 2', description = 'Faixas refletoras de 50mm para circulação noturna e operações de armazém/frota.' WHERE code = 'EPI-COLETE';
UPDATE quality_equipments SET name = 'Manómetro de Precisão Calibrado 0-16 bar', description = 'Instrumento de medição de pressão hidráulica sujeito a recalibração periódica.' WHERE code = 'INST-MANOM';
