# Pedido de esclarecimento — exames médicos de aptidão e respectivos registos

> **Para que serve este documento.** As linhas **H-01 a H-04** da
> [tabela de homologação](CONFORMIDADE_LEGAL_MZ_HARNESS.md#4-homologação-jurídica--por-assinar)
> não se resolvem por pesquisa: confirmámos que a Lei n.º 13/2023 **remete** as regras dos testes,
> exames médicos e dos seus **registos** para diploma conjunto dos Ministros que superintendem as
> áreas do Trabalho e da Saúde, mas esse diploma não é identificável em fontes públicas — nem
> número, nem data, nem texto.
>
> Enquanto a resposta não existir, o sistema **não inventa** periodicidade nem prazo de conservação:
> recusa-se a supor, exactamente como faz com os multiplicadores de hora extra. Este é o pedido a
> enviar. Minuta para o jurista rever, pôr em papel timbrado e assinar — **não deve seguir sem
> revisão jurídica**.

---

## Minuta

**Assunto:** Pedido de esclarecimento sobre o regime dos exames médicos de aptidão ao trabalho e a
conservação dos respectivos registos

**Exmo. Senhor Ministro do Trabalho, Género e Acção Social**
*(com conhecimento ao Ministério da Saúde)*

`[Empresa]`, NUIT `[NUIT]`, com sede em `[morada]`, vem por este meio solicitar esclarecimento sobre
o regime aplicável aos exames médicos de aptidão ao trabalho, nos termos que abaixo se expõem.

A Lei n.º 13/2023, de 25 de Agosto, prevê que o empregador possa exigir ao candidato a emprego ou ao
trabalhador a realização ou apresentação de testes e exames médicos destinados a comprovar a aptidão
física e mental para o exercício das funções, e remete as regras aplicáveis a esses testes e exames,
**bem como aos seus registos**, para diploma conjunto dos Ministros que superintendem as áreas do
Trabalho e da Saúde.

Não tendo sido possível localizar esse diploma por consulta pública, e estando esta empresa a
implementar um sistema informático de gestão de recursos humanos que conserva registos de aptidão
médica dos seus trabalhadores, solicita-se resposta às seguintes questões:

1. **Identificação do diploma.** Foi já aprovado o diploma ministerial conjunto previsto na Lei
   n.º 13/2023 quanto a testes e exames médicos e respectivos registos? Em caso afirmativo,
   solicita-se a indicação do número, data e Boletim da República em que foi publicado. Em caso
   negativo, solicita-se indicação do regime transitoriamente aplicável.

2. **Periodicidade.** Qual a periodicidade legalmente exigida dos exames de aptidão — a regra geral
   e as regras agravadas eventualmente aplicáveis a funções de risco, trabalho nocturno e
   trabalhadores menores?

3. **Conservação dos registos.** Por quanto tempo deve o empregador conservar os registos de aptidão
   médica, contado da realização do exame ou da cessação do contrato de trabalho? Findo esse prazo,
   deve o empregador eliminá-los, ou conservá-los por outro fundamento?

4. **Momento do exame de admissão.** O exame de aptidão na admissão deve estar realizado **antes**
   do início efectivo de funções, ou admite-se a sua realização em momento posterior ao início da
   prestação de trabalho e dentro de que prazo?

5. **Conteúdo do registo.** Confirma-se o entendimento de que o empregador apenas pode conservar a
   **conclusão sobre a aptidão ou inaptidão para o trabalho** e as **restrições de função** dela
   decorrentes, estando-lhe vedado conservar diagnóstico clínico ou resultado de exame
   complementar, incluindo — nos termos da Lei n.º 19/2014 — qualquer elemento relativo ao estado
   serológico do trabalhador quanto ao HIV e SIDA?

O esclarecimento destas questões destina-se exclusivamente a assegurar que os procedimentos e os
sistemas desta empresa cumprem a lei, e será usado para configurar os prazos legais aplicados
automaticamente pelo sistema de gestão de recursos humanos.

Com os melhores cumprimentos,

`[Nome]` — `[Cargo]`
`[Empresa]` · `[Contacto]` · `[Data]`

---

## Onde entra cada resposta no sistema

Nenhuma destas respostas exige código novo. Todas caem em configuração já existente ou já prevista.

| Questão | Onde entra | Efeito |
|---|---|---|
| 1 — identificação do diploma | `hr_policy_configs.legal_basis` | Passa a existir contra o quê é que quem audita confere os prazos |
| 2 — periodicidade | Campo novo em `hr_policy_configs`, molde do `vacation_days_*` | Destranca **CL-13**; o sistema passa a saber propor a validade em vez de a esperar do papel da clínica |
| 3 — conservação | Campo novo + relatório de registos fora de prazo | Destranca **CL-14**. A eliminação continua a ser acto humano auditado — nunca automática |
| 4 — momento da admissão | Regra em `missingExams()` | Se for antes do início de funções, **CL-10** passa de aviso a recusa |
| 5 — conteúdo do registo | Confirma o que o código já impõe | Valida a guarda `PROHIBITED_HEALTH_DATA` (**CL-04**) ou obriga a alargá-la |

## Enquanto não houver resposta

O sistema comporta-se assim, e é deliberado:

- A **validade do exame vem do próprio exame** — quem a fixa é a clínica que o realiza, no papel.
  O sistema não a calcula.
- **Não há expurgo automático** de dados de saúde. Não se constrói uma operação irreversível sobre
  dados clínicos sem saber ao fim de quanto tempo ela é devida.
- **CL-13 e CL-14 ficam a ❌ no harness**, visíveis, em vez de serem dadas como feitas com um número
  plausível. Um prazo inventado que parece certo é pior do que um prazo em falta que se vê.
