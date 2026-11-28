#!/usr/bin/env bash
# Publica o instalador do desktop e o manifesto da actualização de um clique em https://<DOMAIN>/downloads/
# (servido pelo Caddy). Corre no VPS. Ver docs/ACTUALIZACAO_UM_CLIQUE_SPEC.md §3.
# Uso:  ./scripts/publicar-instalador.sh /caminho/Multicore-instalador.exe /caminho/Multicore-instalador.json
# Os postos instalados vêem "Versão X disponível" no rodapé e actualizam-se com um clique. Subir
# APP_CLIENT_MIN_VERSION no .env e `docker compose up -d backend` obriga os postos antigos a actualizar.
set -eu
SRC="${1:?indique o ficheiro do instalador (.exe)}"
MANIFEST="${2:?indique o manifesto (.json) gerado pelo build-windows-installer.ps1}"
DEST=data/downloads
case "$SRC" in *.exe) ;; *) echo "O instalador tem de ser um .exe"; exit 1;; esac
case "$MANIFEST" in *.json) ;; *) echo "O manifesto tem de ser um .json"; exit 1;; esac

# O manifesto tem de descrever exactamente este .exe; senão todos os postos recusariam a descarga.
sha=$(sha256sum "$SRC" | cut -d' ' -f1)
size=$(stat -c %s "$SRC")
grep -q "\"sha256\":\"$sha\"" "$MANIFEST" || { echo "O SHA-256 do manifesto não corresponde ao .exe ($sha)."; exit 1; }
grep -q "\"size\":$size[,}]" "$MANIFEST" || { echo "O tamanho do manifesto não corresponde ao .exe ($size bytes)."; exit 1; }
version=$(sed -n 's/.*"version":"\([0-9.]*\)".*/\1/p' "$MANIFEST")
[ -n "$version" ] || { echo "O manifesto não indica a versão."; exit 1; }

mkdir -p "$DEST"
# Primeiro o .exe, depois o manifesto, cada um por renomeação atómica: um posto nunca lê um manifesto novo
# que aponte para o instalador antigo.
cp "$SRC" "$DEST/Multicore-instalador.exe.tmp"
mv "$DEST/Multicore-instalador.exe.tmp" "$DEST/Multicore-instalador.exe"
cp "$MANIFEST" "$DEST/Multicore-instalador.json.tmp"
mv "$DEST/Multicore-instalador.json.tmp" "$DEST/Multicore-instalador.json"
chmod 755 "$DEST"; chmod 644 "$DEST/Multicore-instalador.exe" "$DEST/Multicore-instalador.json"
echo "Publicada a versão $version: $(du -h "$DEST/Multicore-instalador.exe" | cut -f1)  sha256 ${sha:0:16}"
domain=$(grep -E '^DOMAIN=' .env 2>/dev/null | cut -d= -f2-)
if [ -n "$domain" ]; then echo "Endereço: https://$domain/downloads/Multicore-instalador.exe"; fi
