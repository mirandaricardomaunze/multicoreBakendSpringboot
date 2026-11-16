# Galeria visual de todos os ecrãs

**Código:** SPEC-GALERIA-VISUAL  
**Módulo:** `desktop` (testes)  
**Data:** 2026-11-16  
**Estado:** APROVADO / IMPLEMENTADO  
**Ferramenta:** `desktop/src/test/java/mz/multicore/erp/gui/VisualGalleryTest.java`  
**Relacionado:** `AllScreensHeaderAuditTest` (ECR-11), `ScreenFactory`

## 1. Problema

Os defeitos mais graves das últimas rondas só se viam a olho e passaram pelos testes:

- o "Mais" dos separadores tinha 2 px de altura;
- onze botões tinham texto ou ícone branco sobre o contorno claro;
- diálogos eram maiores do que o ecrã;
- a contagem do Fecho de Caixa ficava cortada por um scroll horizontal.

Para os ver era preciso abrir cada ecrã e cada diálogo à mão, em cada tema.

## 2. Regras

1. **Uma imagem de cada estado.** A galeria guarda uma imagem de cada ecrã da janela principal, de cada ecrã
   de sector, de cada separador de primeiro nível e de cada diálogo próprio (classes que estendem `JDialog`).
2. **Como num portátil comum.** Os ecrãs são desenhados a 1270×720, o que corresponde a um ecrã de 1366×720
   com o menu recolhido. Os diálogos são desenhados no tamanho deles, até ao máximo que o `fitOnScreen`
   permite.
3. **Três temas:** claro, escuro e alto contraste. Os números seguem o formato pt-MZ, como na app.
4. **Mesmos ecrãs que a auditoria.** Os ecrãs são construídos pelo `ScreenFactory`, partilhado com o
   `AllScreensHeaderAuditTest`, com clientes simulados. Por isso as tabelas aparecem vazias.
5. **Só a pedido.** A galeria demora alguns minutos e não corre na suite normal:

   ```
   mvn -o -pl desktop -am test -Dtest=VisualGalleryTest -Dgallery=true
   ```

6. **Saída** em `desktop/target/gallery`:
   - `<tema>/*.png`, as imagens em tamanho real;
   - `web/<tema>/*.jpg`, cópias a 75 % para a página de revisão;
   - `manifest.json`, com tema, grupo, nome, separador, ficheiro e tamanho de cada imagem;
   - `failures.txt`, com o que não foi possível construir.

   Um construtor que abra uma janela modal é desbloqueado ao fim de 10 s e fica registado em `failures.txt`.

## 3. Uso

Depois de uma ronda visual, gerar a galeria e rever as imagens, por tema, antes de pedir revisão ao
responsável. Os defeitos encontrados assim em 2026-11-16 foram:

- Fecho de Caixa e Passagem de Turno: a contagem cortada por um scroll horizontal (largura passou a 820 px);
- a etiqueta "Operador Seguinte" branca no tema claro;
- o campo do telemóvel esticado na renovação de subscrição.
