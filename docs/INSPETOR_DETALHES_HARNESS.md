# Harness — Inspetor de detalhes (modal profissional)

> Cenários de validação manual da [spec](INSPETOR_DETALHES_SPEC.md). UI/Swing — verificação manual.
> Critério técnico: `mvn clean compile` + app arranca + `mvn test` sem regressões.

**Última actualização:** 2026-06-30

| ID    | Passos                                                                       | Esperado                                                                                       |
|-------|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| ID-01 | Em qualquer tabela, **duplo-clique** numa linha                              | Abre modal **Detalhes do Registo** com cabeçalho premium (badge + ícone `fas-info-circle` + subtítulo). |
| ID-02 | Observar os pares etiqueta → valor                                           | Etiqueta da coluna a acento; valor em campo só-leitura com estilo dos restantes campos.         |
| ID-03 | Selecionar texto de um valor e copiar (Ctrl+C)                               | O valor é **copiável** (campo read-only, não editável).                                         |
| ID-04 | Linha com um valor longo (descrição/observações)                            | Esse valor aparece numa área com **quebra de linha** e scroll próprio.                          |
| ID-05 | Tabela com coluna de ID escondida (largura 0)                                | A coluna escondida **não** aparece no inspector.                                                |
| ID-06 | Arrastar o modal / janela pequena                                            | Modal **contido na janela principal** e com scroll quando há muitos campos.                     |
| ID-07 | Rodapé do modal                                                              | Um único botão **Fechar** (`fas-check`) com divisória por cima; fecha o modal.                  |
| ID-08 | Trocar para **tema claro** e repetir o duplo-clique                          | Campos seguem o tema (sem cor fixa); cabeçalho e botão coerentes.                               |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
