# SPEC-AI-MANAGEMENT-ASSISTANT-001

## 1. Objectivo

Disponibilizar um Assistente de Gestão que responda, em português de Moçambique, a perguntas sobre
vendas, stock e compras da empresa activa. A resposta é explicativa e inclui as métricas usadas.

## 2. Fronteiras obrigatórias

- `desktop` envia apenas `AiAssistantRequest` por HTTPS ao backend.
- `contracts` contém somente records e validação Jakarta; não depende de Spring, JPA ou Swing.
- `backend` recolhe e agrega dados sempre pelo `companyId` de `CurrentUserContext`.
- O fornecedor externo recebe apenas métricas agregadas e uma intenção canónica derivada da pergunta. Nomes, NUIT, emails, telefones, salários,
  credenciais, tokens, documentos e linhas individuais não saem do backend.
- A chave do fornecedor fica exclusivamente no backend e nunca aparece em DTO, log ou resposta.

## 3. Permissões e segurança

- Apenas `MANAGER` ou `ADMIN` pode consultar o assistente.
- A pergunta tem entre 3 e 600 caracteres e passa por normalização de controlo.
- A resposta externa tem limite de tamanho e nunca é interpretada como comando.
- Cada consulta gera auditoria `AI_MANAGEMENT_QUERY`, sem guardar a pergunta completa.
- Falha, ausência de chave ou indisponibilidade externa activa a resposta local baseada em regras.

## 4. Dados e evidência

O contexto contém somente:

- total e quantidade de vendas dos últimos 30 dias;
- valor médio por venda;
- número de posições de stock abaixo do mínimo e esgotadas;
- número e valor de encomendas a fornecedor abertas e atrasadas.

Toda resposta devolve `AiEvidenceDTO` com nome, valor e período. Valores monetários usam
`BigDecimal`. O campo `externalModelUsed` informa se houve chamada ao modelo configurado.

## 5. Comportamento funcional

- Perguntas sobre vendas, stock, reposição e compras recebem uma análise curta e accionável.
- O assistente nunca cria faturas, movimentos de stock, encomendas, pagamentos ou alterações.
- Recomendações são propostas. O utilizador executa a operação no módulo próprio, onde os Services
  aplicam permissões, validações, transacções e auditoria existentes.
- A interface apresenta aviso permanente de que a resposta deve ser confirmada nos documentos.

## 6. Configuração externa opcional

- `MULTICORE_AI_ENDPOINT`: endpoint HTTPS compatível configurado pelo operador.
- `MULTICORE_AI_API_KEY`: segredo disponível somente ao processo backend.
- `MULTICORE_AI_MODEL`: identificador do modelo.
- Sem configuração válida, a funcionalidade continua operacional com o analisador local.

## 7. Critérios do harness

- `AI-01`: records existem em `contracts` e não expõem segredos ou entidades.
- `AI-02`: controller chama Service e nunca Repository.
- `AI-03`: Service exige gestor/admin, usa apenas empresa do contexto e audita a consulta.
- `AI-04`: contexto externo contém apenas agregados e não contém PII.
- `AI-05`: desktop comunica por `AiAssistantApiClient` e não conhece chave nem fornecedor.
- `AI-06`: UI usa componentes canónicos, chamada assíncrona e aviso de confirmação humana.
- `AI-07`: indisponibilidade externa usa fallback local sem impedir a consulta.
- `AI-08`: uma análise concluída gera relatório A4 profissional em PDF, com identidade da empresa,
  resumo, evidências, nota de confirmação humana e pré-visualização.
- `AI-09`: o relatório exige `MANAGER`/`ADMIN`, respeita o tenant, gera auditoria e não emite
  documentos fiscais, contabilísticos, pagamentos ou movimentos de stock.
- `AI-10`: o utilizador escolhe Relatório, Memorando, Proposta, Carta, Acta ou Plano de Acção,
  indica título e instruções e recebe PDF pré-visualizável; tipos operacionais são rejeitados.
