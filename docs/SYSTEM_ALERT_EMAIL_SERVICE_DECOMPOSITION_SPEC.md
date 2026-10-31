# Especificação de Decomposição: SystemAlertEmailService (Fase 189)

## Objetivo
Decompor `SystemAlertEmailService.java` (168 linhas) numa fachada enxuta (`<= 65` linhas) que gere o cooldown de incidentes e delega o despacho SMTP e composição HTML de e-mails para `SystemAlertMailDispatcher`.

## Arquitetura Alvo
1. `SystemAlertEmailService.java` (`<= 65` linhas) — Fachada `@Service` preservando `sendCriticalAlert`, `testEmailAlert`, `clearCooldowns` e o construtor legado.
2. `SystemAlertMailDispatcher.java` (`<= 145` linhas) — Componente `@Component` responsável pelo controlo de cooldown (15 minutos), validação de endereço e envio SMTP / simulação local de alarmes e testes NOC.
