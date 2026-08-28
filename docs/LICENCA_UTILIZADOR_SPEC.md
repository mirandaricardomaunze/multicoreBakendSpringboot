# SPEC — Licença do utilizador e aceitação electrónica

## Estado jurídico

Este documento especifica comportamento técnico. O texto de licença incluído é uma minuta e deve
ser revisto por advogado em Moçambique antes da distribuição comercial. A implementação conserva
autoria, conteúdo, integridade, data e hora; não declara homologação jurídica.

## Fluxo obrigatório

1. O instalador Windows apresenta `installer/LICENSE.txt` e só continua após aceitação.
2. Depois do login e da selecção da empresa, o desktop consulta a licença vigente no backend.
3. Sem aceitação da versão vigente, a janela principal não abre.
4. Só `MANAGER` ou `ADMIN`, declarando poderes para representar a empresa, pode aceitar.
5. Recusar ou fechar termina a sessão sem gravar aceitação.
6. Uma nova versão da licença exige nova aceitação; aceitar novamente a mesma versão é idempotente.

## Evidência canónica

O backend guarda: empresa, utilizador, versão, SHA-256, texto da declaração, instante UTC, endereço
IP, versão do desktop e user-agent. A licença exacta é servida pelo backend e o hash é calculado
sobre os seus bytes UTF-8. O desktop nunca inventa versão ou hash.

## Arquitectura

- `contracts`: records de consulta e aceitação.
- `backend`: entidade, repository, service, controller, recurso da licença e migration V60.
- `desktop`: cliente HTTP, diálogo modal e gate antes de `MainFrame`.
- `installer`: cópia da licença e script `jpackage` para gerar `.exe`.

## Critérios de pronto

- Aceitação sem declaração de poderes é recusada.
- Papel sem autorização é recusado.
- Registo inclui hash SHA-256 e metadados de auditoria.
- Desktop não abre `MainFrame` se a licença vigente não estiver aceite.
- Harness automático cobre fronteiras, migration, instalador e regras de serviço.

