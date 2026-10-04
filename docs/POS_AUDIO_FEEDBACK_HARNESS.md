# Matriz de Testes & Harness: Motor de Feedback Sonoro no POS (HARNESS-PAUD-001)

Este documento especifica a suíte de validação automatizada para o Motor de Feedback Sonoro no POS (`PosAudioFeedbackHarnessTest`).

## Matriz de Validação

| ID | Cenário de Teste | Comportamento Esperado | Resultado Requerido |
|---|---|---|---|
| **PAUD-01** | Inicialização do `PosAudioFeedbackEngine` | Carrega definições por omissão sem exceções | 🟢 PASS |
| **PAUD-02** | Disparo de evento `SUCCESS` em Headless | Ignora de forma segura a reprodução de som quando sem áudio | 🟢 PASS |
| **PAUD-03** | Disparo de eventos `WARNING` e `ERROR` | Executa na fila assíncrona sem bloquear a thread chamadora | 🟢 PASS |
| **PAUD-04** | Comutação de estado (Mute / Unmute) | `setEnabled(false)` desativa novos disparos de áudio | 🟢 PASS |
| **PAUD-05** | Persistência local em disco | Definições salvas em `audio_settings.json` são recarregadas | 🟢 PASS |
| **PAUD-06** | Síntese de amostras PCM em memória | Método `generateToneBytes` produz array de bytes sintéticos válidos | 🟢 PASS |
| **PAUD-07** | Integração no `POSPanel` | Botão de comutação de som altera estado do motor em runtime | 🟢 PASS |
| **PAUD-08** | Decomposição e Linhas de Código | Todos os componentes e `POSPanel` mantêm-se estritamente $\le 1000$ linhas | 🟢 PASS |
