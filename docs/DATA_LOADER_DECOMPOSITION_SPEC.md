# Especificação de Decomposição — DataLoader (Fase 196)

## Objetivo
Decompor `DataLoader` (`583` linhas) em 2 colaboradores coesos no pacote `mz.multicore.erp.architecture`, separando a sementeira de referência fiscal/administradores (executada em todos os ambientes) da sementeira de demonstração multiempresa (executada apenas quando `app.seed-demo-data=true`).

## Colaboradores Extraídos (`mz.multicore.erp.architecture`)
1. `ReferenceAndAdminDataSeeder` (`@Component`) — Sementeira idempotente de taxas de IVA/retenções moçambicanas (`TaxRate`), categorias base de produtos (`ProductCategory`), conta `superadmin` e conta `admin`.
2. `DemoEnterpriseDataSeeder` (`@Component`) — Sementeira idempotente de empresas de demonstração, utilizadores, armazéns, fornecedores, colaboradores, clientes, artigos, stocks, contas de tesouraria, tickets CRM e despesas de RH.

## Compatibilidade
- Construtor legado de 16 argumentos preservado em `DataLoader`.
