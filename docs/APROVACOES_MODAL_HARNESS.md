# Harness — Decisão de aprovações em modal

> Cenários de validação manual da [spec](APROVACOES_MODAL_SPEC.md). UI/Swing — verificação manual.
> Critério técnico: `mvn clean compile` + app arranca + `mvn test` sem regressões.

**Última actualização:** 2026-06-30

| ID    | Passos                                                                          | Esperado                                                                                   |
|-------|---------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| AP-01 | Abrir **Aprovações**                                                            | Tabela de pendentes a **largura total** + histórico em baixo; sem inspector lateral.       |
| AP-02 | Sem selecção                                                                    | Botão **Abrir / Decidir** desactivado.                                                      |
| AP-03 | Selecionar uma linha pendente                                                   | Botão **Abrir / Decidir** activa.                                                           |
| AP-04 | **Duplo-clique** numa linha pendente (ou botão Abrir/Decidir)                   | Abre **modal de decisão** premium (badge `fas-clipboard-check` + subtítulo "Tipo #id").    |
| AP-05 | Ver o conteúdo do modal                                                         | Tipo, ID, Solicitante, Valor, Perfil (só-leitura) + Descrição com quebra de linha.         |
| AP-06 | Clicar **Aprovar** (com perfil autorizado)                                      | Documento aprovado; modal fecha; listas e histórico refrescam.                             |
| AP-07 | Clicar **Aprovar** sem permissão (perfil insuficiente)                          | Mensagem de erro de autorização; **modal mantém-se aberto**.                               |
| AP-08 | Clicar **Rejeitar** e deixar o motivo vazio                                     | Erro "motivo obrigatório"; não rejeita.                                                     |
| AP-09 | Clicar **Rejeitar** com motivo válido                                           | Documento rejeitado; modal fecha; aparece no histórico com o motivo.                       |
| AP-10 | Clicar **Fechar**                                                               | Fecha sem decidir; nada muda.                                                              |
| AP-11 | Arrastar o modal / janela pequena                                               | Modal **contido na janela principal**, com scroll quando necessário.                       |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
