# Harness — Actualização do programa com um clique

> Cenários que provam [ACTUALIZACAO_UM_CLIQUE_SPEC.md](ACTUALIZACAO_UM_CLIQUE_SPEC.md).

**Última actualização:** 2026-11-28

## Automáticos

| ID | Garantia | Teste |
|---|---|---|
| AU-01 | Manifesto: lê versão, SHA-256 e tamanho; recusa hash inválido, tamanho 0 ou > 1 GB, campos em falta (§4.2) | `UpdateManifestTest` |
| AU-02 | Ordem das recusas: não instalado, versão não mais recente, caixa aberta, vendas suspensas; o 426 salta caixa e vendas (§4.2) | `UpdateEligibilityTest` |
| AU-03 | SHA-256 e tamanho diferentes recusam e apagam; iguais aceitam; descarga maior do que o manifesto pára (§4.3) | `InstallerVerifierTest` · `UpdateDownloaderTest` |
| AU-04 | Sem assinante configurado não exige assinatura; com assinante, exige `Valid` e o nome no assunto (§2, §4.3) | `InstallerVerifierTest` |
| AU-05 | O auxiliar espera pelos `Multicore.exe` da pasta instalada, reconfere o SHA-256, corre `/quiet` e só reabre em "agora" (§4.4) | `UpdateInstallerLauncherTest` |
| AU-06 | O script de build gera o manifesto; o de publicação confere o hash e publica o `.exe` antes do manifesto (§3) | `WindowsInstallerHarnessTest` |
| AU-07 | Endereços do manifesto e do instalador derivam só do servidor configurado (§3) | `ClientUpdatePromptTest` |

## Ao vivo

| ID | Cenário | Esperado |
|---|---|---|
| AU-50 | Instalado 1.0.x, servidor publica 1.0.y; clicar no rodapé, "Reiniciar e instalar agora" | ERP fecha, instala, reabre em 1.0.y; configuração intacta |
| AU-51 | Igual, "Instalar ao fechar" | rodapé muda; ao sair do ERP instala; não reabre |
| AU-52 | Manifesto com SHA-256 errado | recusa e não fecha o ERP |
| AU-53 | Operador com caixa aberta | recusa com "Feche a caixa antes de actualizar" |
