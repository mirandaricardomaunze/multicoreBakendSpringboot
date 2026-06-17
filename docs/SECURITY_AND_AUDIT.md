# Seguranca e Auditoria - Multicore ERP

Este documento define regras para autenticacao, autorizacao, tenant e auditoria.

## Principios

- Cada request ou operacao deve saber quem e o utilizador actual.
- Dados operacionais pertencem a uma empresa/tenant.
- Permissao deve ser validada antes da operacao sensivel.
- Operacoes sensiveis devem deixar rasto.

## Autenticacao

- Token/sessao identifica o utilizador.
- Desktop guarda token apenas durante a sessao.
- Backend deve ser a fonte de verdade para permissoes.
- Nunca escrever tokens, passwords ou hashes em logs.

## Autorizacao

Validar permissao em operacoes como:

- Criar/anular factura.
- Emitir nota de credito/debito.
- Aprovar pedidos.
- Transferir stock.
- Ajustar stock.
- Abrir/reabrir/fechar caixa.
- Alterar configuracao fiscal.
- Processar salarios.
- Aceder a relatorios sensiveis.

## Tenant/empresa

- Usar `CurrentUserContext` para empresa actual quando aplicavel.
- Validar que o utilizador tem acesso a empresa seleccionada.
- Repositories/Services devem filtrar por empresa em dados operacionais.
- Nao aceitar `companyId` do cliente como verdade sem confirmar acesso.

## Auditoria

Auditar:

- Login e falhas relevantes.
- Anulacoes.
- Aprovacoes e rejeicoes.
- Alteracoes fiscais.
- Ajustes de stock.
- Transferencias de stock.
- Fecho e reabertura de caixa.
- Alteracoes de utilizador/role/acesso.
- Processamento salarial.

Evento de auditoria deve ter:

- Utilizador.
- Empresa.
- Tipo de accao.
- Entidade afectada.
- Data/hora.
- Resultado.
- Motivo quando aplicavel.

## Dados sensiveis

Nao expor em DTOs:

- Password/hash.
- Tokens.
- Detalhes internos de seguranca.
- Campos de auditoria irrelevantes para o utilizador.
- Stack traces.

## Checklist

- [ ] Operacao usa utilizador actual.
- [ ] Empresa/tenant foi validado.
- [ ] Permissao sensivel foi verificada.
- [ ] Evento importante foi auditado.
- [ ] DTO nao expoe dados sensiveis.
- [ ] Mensagem de erro e clara, sem detalhes internos.
