# Especificação Técnica: Motor de Feedback Sonoro Discreto no POS (SPEC-PAUD-001)

## 1. Visão Geral & Objetivos
Esta especificação define o **Motor de Feedback Sonoro Discreto no POS** (`PosAudioFeedbackEngine`). O objetivo é fornecer sinais sonoros curtos, limpos e sintetizados em tempo real pela JVM (sem dependência de ficheiros áudio externos) para informar o operador do POS durante a leitura de códigos de barras, captura de peso da balança e ocorrência de erros sem necessidade de olhar constantemente para o ecrã.

## 2. Regras de Negócio & Ergonomia de Áudio

### 2.1. Tipos de Eventos Sonoros (`SoundEvent`)
1. **`SUCCESS` (Leitura Válida de Código de Barras)**:
   - Tom sinusoidal curto de alta frequência (800 Hz, 65 ms).
   - Confirma a adição com sucesso de um artigo ao carrinho do POS.
2. **`WARNING` (Alerta Operacional / Stock Baixo / Pedido de PIN)**:
   - Tom duplo moderado (600 Hz, 120 ms).
   - Acionado em avisos de limite de crédito, stock mínimo ou solicitação de PIN de gestor.
3. **`ERROR` (Código Inválido / Produto Não Encontrado)**:
   - Tom descendente de baixa frequência (350 Hz, 160 ms).
   - Acionado quando o código lido não existe no catálogo ou o checkout falha.
4. **`SCALE_STABLE` (Estabilização da Balança USB/Serial)**:
   - Tom cristalino agudo (1000 Hz, 40 ms).
   - Confirma que o peso da balança foi capturado com estabilidade.

### 2.2. Execução Não-Bloqueante & Tolerância a Falhas
1. **Thread Dedicada**: Toda a síntese e reprodução PCM de áudio é executada numa fila assíncrona de thread única (`ExecutorService`), nunca bloqueando o Event Dispatch Thread (EDT) nem o leitor de código de barras.
2. **Modo Silencioso / Degradabilidade**:
   - O utilizador pode alternar entre som ativado/desativado (`enabled`) através da barra superior do POS `[ 🔊 Som ]` ou nas Configurações.
   - Em ambiente headless (testes automatizados) ou sistemas sem placa de som/colunas, o motor deteta a ausência de hardware e ignora a emissão sonora sem lançar exceções.

### 2.3. Persistência de Preferências de Áudio
1. As definições (`enabled`, `volume`) são salvas no ficheiro local:
   `${user.home}/.multicore/audio_settings.json`

## 3. Componentes a Implementar (`desktop`)

### 3.1. `PosAudioFeedbackEngine.java` (`mz.multicore.erp.gui.pos.audio`)
- Motor Singleton responsável por gerar amostras PCM em memória via `javax.sound.sampled.AudioSystem` e `SourceDataLine`.
- Métodos públicos:
  - `playAsync(SoundEvent event)`
  - `setEnabled(boolean enabled)`
  - `isEnabled()`
  - `setVolume(float volume)`
  - `getVolume()`

### 3.2. Integração no `POSPanel.java`
- Disparo de `playAsync(SoundEvent.SUCCESS)` na leitura válida de artigo.
- Disparo de `playAsync(SoundEvent.ERROR)` em produtos não encontrados.
- Botão de comutação `[ 🔊 Som ]` na barra de ferramentas.

## 4. Critérios de Aceitação & Conformidade
- Todos os testes da suíte `PosAudioFeedbackHarnessTest` passam a 100%.
- A classe `POSPanel.java` mantém-se estritamente $\le 1000$ linhas.
