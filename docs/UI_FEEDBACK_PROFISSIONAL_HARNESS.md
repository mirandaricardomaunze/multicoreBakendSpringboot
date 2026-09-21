# HARNESS — Feedback profissional da UI

Complementa [UI_FEEDBACK_PROFISSIONAL_SPEC.md](UI_FEEDBACK_PROFISSIONAL_SPEC.md).

## A. Verificação automática

| ID | Cenário | Esperado |
|---|---|---|
| FP-01 | Banner de erro recebe detalhe | Fica visível, sem modal |
| FP-02 | Banner recebe acção de retry | Acção existe e executa uma vez |
| FP-03 | Banner é fechado | Desaparece sem remover o conteúdo do módulo |
| FP-04 | Toast de sucesso é construído | Ícone, mensagem e nome acessível presentes |
| FP-05 | Corpo de confirmação é construído | Usa `ModernPanel` e semântica acessível |
| FP-06 | Erro no `ModernFormDialog` | É apresentado por `InlineFeedbackPanel` |
| FP-07 | Aprovações carrega com erro | Banner oferece `Tentar novamente` |
| FP-08 | Aprovação/rejeição termina | Sucesso usa toast não bloqueante |
| FP-09 | Código novo do lote | Não introduz emoji nem cor ad-hoc em painel de negócio |
| FP-10 | Inventário global de `JOptionPane` | No máximo 45; só pode diminuir |
| FP-11 | Comercial | Zero `JOptionPane`; toast/banner conforme semântica |
| FP-12 | POS | Zero `JOptionPane`; impressão usa confirmação Multicore |
| FP-13 | Stock e Compras | Zero `JOptionPane` nos quatro painéis prioritários |
| FP-14 | Validação de Stock/Compras | Campo inválido fica marcado; formulário permanece aberto |
| FP-15 | Encomendas a fornecedor | No máximo 1 `JOptionPane` composto |
| FP-16 | Transferências de stock | No máximo 5 confirmações/fluxos legados |
| FP-17 | Produto/lote de stock | No máximo 16 chamadas legadas; sucessos usam toast |
| FP-18 | Configurações | Zero `JOptionPane`; backup e utilizadores usam feedback canónico |
| FP-19 | Plataforma | No máximo dois diálogos compostos; confirmações críticas são Multicore |
| FP-20 | Recursos Humanos | Painel no máximo quatro; acções do colaborador no máximo dois; feedback simples é canónico |
| FP-21 | CRM | Painel sem `JOptionPane`; acções conservam no máximo três recolhas contextuais de texto |
| FP-22 | Leitura POS | `PosBarcodeActions` sem `JOptionPane` |
| FP-23 | Caixa POS | No máximo dois diálogos compostos; feedback simples usa banner/toast |
| FP-24 | Stock operacional | Contagem no máximo um e catálogo/lotes no máximo dois, apenas confirmações |
| FP-25 | Comercial autónomo | Notas e recibos sem `JOptionPane`; guias ≤3, cotações ≤4 e clientes ≤1 |
| FP-26 | Fiscal e devoluções | Fiscal ≤2 e devolução POS ≤1, apenas confirmações ou conteúdo composto |
| FP-27 | Operacional complementar | Armazéns, contas a pagar e promoções sem `JOptionPane` |
| FP-28 | Subpainéis RH | Despesas e férias a zero; restantes apenas confirmações, recolha ou vista composta |
| FP-29 | Controladores auxiliares | Catálogo POS, conversão/submissão, fornecedores, categorias, lotes e histórico a zero |
| FP-30 | Tesouraria e notificações | Ambos sem `JOptionPane`, com repetição contextual de cargas |
| FP-31 | Diálogos comerciais | Editor, cancelamento e contas correntes a zero; faturar conserva uma confirmação composta |
| FP-32 | Impressão e exportação | Pré-visualização e exportação sem `JOptionPane`; falhas aparecem dentro do fluxo |
| FP-33 | Allowlist final | Toda ocorrência restante pertence ao conjunto revisto de confirmações, formulários ou vistas compostas |

Executar:

```powershell
mvn -pl desktop -am -Dtest=ProfessionalFeedbackHarnessTest test
```

## B. Verificação manual em Windows

| ID | Cenário | Evidência esperada |
|---|---|---|
| M-01 | Aprovar documento | Toast aparece sem roubar foco e desaparece sozinho |
| M-02 | Rejeitar documento | Toast aparece; tabela é actualizada |
| M-03 | API de aprovações indisponível | Banner fica no módulo e retry volta a carregar |
| M-04 | Formulário devolve erro | Modal permanece aberto e erro aparece acima dos campos |
| M-05 | Vários toasts seguidos | Um de cada vez, pela ordem de emissão |
| M-06 | Tema claro e escuro | Texto, bordas e cores permanecem legíveis |
| M-07 | Escalas 100%, 125% e 150% | Sem corte do texto ou dos botões |
| M-08 | Teclado | `Esc` cancela confirmação e `Enter` confirma |

Os casos M-01..M-08 exigem execução visual no Windows; testes headless não os substituem.

### Driver visual isolado

O driver `ProfessionalFeedbackVisualDriver` permite executar M-01..M-08 sem backend nem dados
reais. Compilar com `mvn -pl desktop -am test-compile` e arrancar a classe com o classpath de
teste. Para M-07, repetir com `-Dsun.java2d.uiScale=1.0`, `1.25` e `1.5`; isto isola a escala da
JVM e não altera as definições do Windows.

### Resultado da homologação — 2026-09-03

| Caso | Resultado | Evidência |
|---|---|---|
| M-01 | Aprovado | Estado passou a `APROVADO`; a janela principal conservou o foco |
| M-02 | Aprovado | Estado passou a `REJEITADO` imediatamente |
| M-03 | Aprovado | Banner de erro permaneceu no módulo; retry apresentou sucesso |
| M-04 | Aprovado | Modal permaneceu aberto e mostrou o erro acima do campo |
| M-05 | Aprovado com suporte automático | Fila exclusiva e ordem cobertas pelo harness; toasts não são janelas focáveis |
| M-06 | Aprovado | Tema escuro e claro verificados; contraste corrigido durante a sessão |
| M-07 | Aprovado | 100%, 125% e 150% sem texto ou botões cortados |
| M-08 | Aprovado | `Esc` devolveu `CANCELADO`; `Enter` devolveu `CONFIRMADO` |

Foram encontrados e corrigidos dois defeitos: revalidação do contentor ao mostrar/ocultar o banner
e recálculo das cores semânticas do banner ao mudar de tema. A máquina disponibiliza apenas
Microsoft Print to PDF, XPS e Fax; não existe impressora física instalada para um ensaio real.

## C. Gate de regressão

- `JOptionPane` pode existir em fluxos legados, mas o total não pode aumentar sem justificação na SPEC.
- Toast nunca confirma destruição nem esconde erro que exige correcção.
- Nenhum feedback executa chamada HTTP no EDT.
- Mensagens visíveis permanecem em português de Moçambique.
