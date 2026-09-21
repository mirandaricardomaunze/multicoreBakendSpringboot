# SPEC — Feedback profissional e interacções não bloqueantes

**Data:** 2026-08-31  
**Camada:** cliente Swing (`desktop`)  
**Sem alterações:** contratos HTTP, regras de negócio, persistência e numeração documental.

## 1. Problema

A interface usa componentes modernos, mas ainda apresenta confirmações de sucesso, falhas de
carregamento e validações simples através de `JOptionPane`. Um modal interrompe o operador, perde o
contexto visual da acção e mistura o aspecto nativo do sistema operativo com o design Multicore.

## 2. Política canónica

| Situação | Componente | Comportamento |
|---|---|---|
| Operação concluída | `ToastManager` | Não bloqueia, desaparece automaticamente e respeita uma fila |
| Informação transitória | `ToastManager` | Breve, sem exigir clique |
| Falha de carregamento | `InlineFeedbackPanel` | Permanece no módulo e oferece `Tentar novamente` |
| Validação/gravação de formulário | `InlineFeedbackPanel` no `ModernFormDialog` | Mantém o diálogo aberto e o contexto visível |
| Decisão importante | `ModernMessageDialog` | Modal temático, `Enter` confirma e `Esc` cancela |
| Operação destrutiva | `ModernMessageDialog` vermelho | Confirmação explícita e verbo específico |

`JOptionPane` continua permitido apenas durante a migração de fluxos legados. Código novo não o
usa para sucesso, erro recuperável ou validação simples.

## 3. Toast

- Ancorado ao canto inferior direito da janela activa, sem roubar foco.
- Duração canónica de 3,6 segundos.
- Apenas um toast visível; os seguintes aguardam em fila.
- Tipo semântico com ícone e cor: sucesso, informação, aviso ou erro.
- Texto e nome acessível presentes; mensagem vazia recebe alternativa segura.
- Nunca substitui confirmação, erro que exige correcção ou informação legal.

## 4. Banner contextual

- Vive dentro do painel ou formulário afectado.
- Tem título curto, detalhe accionável, ícone, cor semântica e botão de fechar.
- Pode expor uma única acção de recuperação, normalmente `Tentar novamente`.
- Actualização visual ocorre no EDT.
- Não revela stack trace, URL interna, SQL ou nomes técnicos de excepção.

## 5. Diálogo de mensagem e confirmação

- Usa paleta, tipografia, ícones e botões Multicore.
- Confirmação destrutiva usa cor vermelha e verbo específico (`Eliminar`, `Anular`, `Cancelar`).
- `Esc` fecha/cancela; o botão principal é o default apenas quando existe decisão.
- Fica contido na janela principal e adapta-se ao tema.

## 6. Adopção

Este lote cria a infraestrutura transversal, converte os erros de gravação de todos os
`ModernFormDialog` para feedback inline e usa Aprovações como fluxo de referência completo:
carregamento com retry e sucessos de aprovar/rejeitar por toast.

As restantes chamadas legadas são migradas por módulo, sem alterar regra ou sequência de negócio.
O contador de `JOptionPane` deve ser monotonicamente decrescente; nenhuma feature nova o aumenta.

### Fase 2 — fluxos operacionais prioritários

- Comercial e POS deixam de usar `JOptionPane`: sucesso usa toast, pré-condições e falhas usam
  banner contextual e a impressão do recibo usa `ModernMessageDialog`.
- Stock e Compras adoptam banner transversal para falhas de carregamento com retry e toast nos
  registos concluídos. A contagem de stock usa o ciclo assíncrono do `ModernFormDialog`; quantidade,
  motivo, preço, data e IVA inválidos ficam marcados no campo activo, sem fechar o formulário.
- O inventário global fica congelado no máximo em **365 chamadas legadas**. O harness falha se
  esse número aumentar e exige zero em `ComercialPanel`, `POSPanel`, `StockPanel` e `ComprasPanel`.

### Fase 4 — subfluxos de Stock e Compras

- Encomendas a fornecedor usam banner para pré-condições e selecção, toast para criação/recepção e
  `ModernMessageDialog` na recepção total. Resta apenas o editor tabular da recepção parcial, que
  ainda depende de um `JOptionPane` com componente composto.
- Transferências usam feedback do painel para pré-condições/erros e toast nas decisões concluídas;
  confirmações críticas continuam modais até a migração para `ModernMessageDialog`.
- Catálogo de stock usa feedback contextual nas pré-condições e toast no registo de lote e edição.
- Inventário global reduzido para **337**; limites por subfluxo também ficam protegidos no harness.

### Fase 5 — Plataforma e Configurações

- Configurações fica sem `JOptionPane`: selecção, permissões e falhas usam banner; utilizadores,
  perfis, configuração documental e backups concluídos usam toast.
- Plataforma migra selecção, pré-condições e sucessos para banner/toast. Activação de empresa e
  suspensão/reactivação de assinatura usam `ModernMessageDialog` com verbo explícito.
- Permanecem dois diálogos compostos na Plataforma: histórico tabular de pagamentos e conversa de
  assistência com resposta. Não são mensagens simples e serão substituídos por componentes próprios.
- Inventário global reduzido para **299**; Configurações exige zero e Plataforma no máximo dois.

### Fase 6 — Recursos Humanos e CRM

- RH e CRM apresentam falhas e pré-condições num banner contextual no topo do módulo e usam
  toast para conclusões bem-sucedidas, sem interromper o operador.
- Permanecem em RH quatro diálogos compostos no painel principal e dois nas acções do colaborador:
  selecção/confirmacão com componentes e a vista tabular de saúde ocupacional.
- CRM fica sem `JOptionPane` no painel. As acções conservam apenas três recolhas contextuais de
  texto (motivo ou nota) até à migração dos formulários compostos.
- Inventário global reduzido para **244**, protegido por limites específicos por ficheiro.

### Fase 7 — Subfluxos de Stock e POS

- Leitura de códigos/etiquetas no POS deixa de abrir alertas modais; produto inexistente, caixa
  fechado, falta de stock e falha de promoções surgem no banner do POS.
- Abertura e movimentos de caixa usam toast para sucesso e banner para erros. Permanecem apenas os
  dois diálogos compostos de fecho e movimento.
- Inventário físico usa feedback do Stock para selecção, estado, rascunho, resultado e falhas;
  conserva somente a confirmação destrutiva de cancelamento.
- Catálogo/lotes migra todas as validações e falhas simples; conserva duas confirmações de
  decisão do operador. Inventário global reduzido para **207**.

### Fase 8 — Módulos restantes (concluída)

- Cotações, guias de remessa, clientes e fiscal usam banner/toast no feedback simples.
- Notas comerciais e recibos ficam sem `JOptionPane`; notas mantêm banners independentes por
  separador para não misturar o contexto de crédito e débito.
- Devoluções POS conservam somente a decisão de iniciar uma venda de troca.
- Inventário global intermédio: **155**; FP-25/FP-26 protegem o progresso enquanto a fase continua.
- Armazéns, contas a pagar e promoções ficam sem `JOptionPane`. Nos subpainéis de RH, despesas
  e férias ficam a zero e os restantes conservam apenas confirmações, recolhas de referência ou
  vistas compostas. Novo inventário intermédio: **111**, protegido por FP-27/FP-28.
- Controladores auxiliares de POS/comercial/stock/compras, Tesouraria e Notificações deixam de
  usar feedback modal simples. O histórico operacional conserva somente a tabela composta e a
  autorização de reimpressão. Inventário intermédio: **85**, protegido por FP-29/FP-30.
- Diálogos comerciais eliminam feedback simples; faturar conserva apenas a confirmação com
  resumo. A pré-visualização de impressão passa a mostrar validação e falhas no próprio modal,
  e exportações usam feedback não bloqueante. Inventário: **58**, com FP-31/FP-32.
- Contabilidade, movimentos comerciais, suporte e transferências eliminam o feedback simples
  remanescente. Motivos obrigatórios usam o formulário canónico e a validade da cotação usa
  `DateField`. Restam **45** chamadas reais, todas revistas e limitadas por allowlist no FP-33:
  confirmações críticas, recolhas opcionais, autorizações e vistas compostas.

## 7. Definition of done

- Componentes canónicos implementados e acessíveis.
- Erro de `ModernFormDialog` não abre `JOptionPane`.
- Aprovações cobre toast, banner e retry.
- Harness automático FP-01..FP-33 verde.
- `mvn clean compile` e testes focados verdes.
- Validação manual em tema claro/escuro e escalas 100%, 125% e 150% permanece obrigatória.

### Fase 9 — Auditoria final

- O harness automático cobre componentes, integrações prioritárias, limite global e allowlist.
- Toda chamada restante foi revista: 34 confirmações, nove vistas/mensagens críticas e duas
  recolhas opcionais; nenhuma é feedback simples de sucesso, pré-condição ou erro recuperável.
- A conclusão automática exige `mvn clean compile`, FP-01..FP-33 e a suite integral do desktop.
- A aceitação visual/física exige somente M-01..M-08 do HARNESS; impressora real, escala do Windows,
  temas e comportamento de foco não podem ser certificados por testes headless.
- Estado automático final: compilação limpa, FP-01..FP-33 e 174 testes do desktop aprovados.
