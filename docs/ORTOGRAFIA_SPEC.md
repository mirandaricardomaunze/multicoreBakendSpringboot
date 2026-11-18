# Uma só ortografia e sem inglês no texto visível

Estado: **feito** · 2026-11-17 · Harness: `desktop/.../gui/OrthographyHarnessTest.java`

## Decisão do responsável (2026-11-17)

O texto que o operador lê segue a grafia **anterior ao Acordo Ortográfico de 1990**, como é habitual em
Moçambique: Actualizar, Seleccione, Acção, Activo, Directo, Efectuar, Exactamente, Electrónico, Sector,
Contacto, Projecção. **"Fatura"** e derivados ficam como estão: é o termo do sistema e dos documentos fiscais.

## Problema

Conviviam as duas normas: "Actualizar" 77 vezes e "Atualizar" 3; "Seleccione" 167 e "Selecione" 49;
"COLABORADORES ATIVOS" ao lado de "Actualizar" no mesmo ecrã. E havia inglês visível: "Blind Drop",
"Picking List", "Cash Flow Forecast", "Workflow", "Aging", "Push USSD", "Ticket", "Backup".

## Regras

| ID    | Regra |
|-------|-------|
| OR-01 | Nenhum texto entre aspas, no desktop ou no backend, usa a grafia de 1990. Ficam de fora códigos de estado de uma palavra em maiúsculas (`"ATIVO"`), chaves (`"atividade"`), URLs e SQL — são dados, não texto para pessoas. |
| OR-02 | Nenhum termo inglês nos textos do desktop. "Backup" passa a "cópia de segurança" (como o separador já dizia); "backup" fica só como palavra-chave da pesquisa global. |

Foram trocados cerca de 525 textos no desktop e no backend, incluindo as mensagens de `BusinessRuleException`.
Os comentários e os identificadores Java não mudam.
