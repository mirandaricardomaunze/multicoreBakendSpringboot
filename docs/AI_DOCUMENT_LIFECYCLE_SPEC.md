# SPEC-AI-DOCUMENT-LIFECYCLE-001

## Objectivo

Guardar documentos informativos gerados pelo Assistente de Gestao com historico imutavel de versoes e aprovacao auditavel. O recurso nao emite facturas, recibos, notas fiscais nem movimentos operacionais.

## Estados

`RASCUNHO -> EM_REVISAO -> APROVADO | REJEITADO -> RASCUNHO` e `APROVADO | REJEITADO -> ARQUIVADO`.

## Criterios verificaveis

- **DL-01** Cada documento pertence a uma empresa e recebe numero unico por empresa.
- **DL-02** Cada regeneracao cria uma versao nova; versoes anteriores nao sao alteradas.
- **DL-03** Apenas MANAGER ou ADMIN cria, regenera, submete ou arquiva documentos.
- **DL-04** Apenas ADMIN aprova ou rejeita.
- **DL-05** O autor nao pode aprovar nem rejeitar o proprio documento.
- **DL-06** A rejeicao exige motivo claro.
- **DL-07** Todas as leituras e escritas validam o tenant activo.
- **DL-08** Criacao, nova versao, submissao, decisao, arquivo e exportacao sao auditados sem gravar conteudo nem PDF no log.
- **DL-09** A pesquisa aceita titulo, numero, tipo e estado dentro da empresa activa.
- **DL-10** O PDF de qualquer versao pode ser recuperado sem substituir a versao guardada.
- **DL-11** DTOs permanecem no modulo `contracts` e nao dependem de Spring, JPA ou Swing.
- **DL-12** O desktop comunica apenas pela API e apresenta o historico numa tabela padronizada dentro de `ModernPanel(16)`.

## Dados

`ai_documents` guarda identidade, estado, autor, versao corrente e decisao. `ai_document_versions` guarda instrucoes, conteudo textual e PDF de cada versao. Exclusao em cascata existe apenas para manutencao tecnica do agregado; a interface nao expoe eliminacao.

## Seguranca

O backend deriva empresa, utilizador e papel de `CurrentUserContext`. Nenhum endpoint aceita `companyId`, autor ou aprovador enviados pelo cliente. Conteudo e bytes do documento nunca entram em mensagens de auditoria.
