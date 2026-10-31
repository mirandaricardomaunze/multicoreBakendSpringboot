# Especificação de Decomposição — PrintController (Fase 195)

## Objetivo
Decompor `PrintController` (`432` linhas e 25 dependências directas) extraindo a orquestração de primeira impressão sensível, reimpressão autorizada e construção de respostas HTTP PDF (`Content-Disposition`, sanitização de nome de ficheiro e encaminhamento por `SensitiveDocumentType`) para o colaborador coeso `PrintRequestDispatcher`.

## Colaborador Extraído (`mz.multicore.erp.modules.printing`)
- `PrintRequestDispatcher` (`@Component`) — Encapsula os serviços de renderização PDF, controlo de primeira impressão/reimpressão sensível (`SensitiveDocumentPrintService`) e empacotamento HTTP `ResponseEntity<Resource>`.

## Compatibilidade
- Todos os endpoints `@GetMapping` e `@PostMapping` de `/api/print/**` permanecem inalterados em `PrintController`.
- Construtores legados de 22 e 25 argumentos preservados em `PrintController`.
