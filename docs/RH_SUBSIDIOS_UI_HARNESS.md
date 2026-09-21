# HARNESS — Subsídios legais no desktop RH

## Automático

| ID | Prova | Resultado esperado |
|---|---|---|
| SUH-01 | Cliente consulta 13.º mês | GET no endpoint tipado |
| SUH-02 | Cliente paga 13.º mês | POST no endpoint tipado |
| SUH-03 | Cliente consulta subsídio de férias | GET no endpoint tipado |
| SUH-04 | Cliente paga subsídio de férias | POST no endpoint tipado |
| SUH-05 | UI não calcula valores | Usa exclusivamente os DTOs devolvidos pelo backend |
| SUH-06 | Operações remotas | Executadas por `UIHelper.loadAsync/runWithProgress` |
| SUH-07 | Pagamento | Exige `ModernMessageDialog.confirm` |
| SUH-08 | Descoberta | 13.º mês em Recibos e subsídio em Férias, sem novo tab |

Executar:

```powershell
mvn -pl desktop -am "-Dtest=HRSubsidiesUiHarnessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Manual

| ID | Acção | Resultado esperado |
|---|---|---|
| SUH-50 | Abrir 13.º mês e escolher ano | Lista e total coincidem com o backend |
| SUH-51 | Confirmar pagamento | Toast indica quantidade e total pagos; repetição não duplica |
| SUH-52 | Seleccionar férias não aprovadas | Backend recusa com mensagem clara |
| SUH-53 | Seleccionar férias aprovadas | Pré-visualização mostra dias, taxa diária e total |
| SUH-54 | Pagar duas vezes | Segunda tentativa é recusada sem nova saída de tesouraria |
| SUH-55 | Simular API lenta/indisponível | UI não bloqueia e apresenta erro contextual |

