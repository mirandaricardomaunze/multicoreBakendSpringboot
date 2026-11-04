#!/usr/bin/env bash
# Publica o instalador do desktop em https://<DOMAIN>/downloads/ (servido pelo Caddy). Corre no VPS.
# Uso:  ./scripts/publicar-instalador.sh /caminho/Multicore-instalador.exe
# Depois de publicar, subir APP_CLIENT_MIN_VERSION no .env e `docker compose up -d backend`
# obriga os postos antigos a actualizar; eles vêem o botão "Descarregar actualização".
set -eu
SRC="${1:?indique o ficheiro do instalador (.exe)}"
DEST=data/downloads
case "$SRC" in *.exe) ;; *) echo "O instalador tem de ser um .exe"; exit 1;; esac
mkdir -p "$DEST"
cp "$SRC" "$DEST/Multicore-instalador.exe.tmp"
mv "$DEST/Multicore-instalador.exe.tmp" "$DEST/Multicore-instalador.exe"
chmod 755 "$DEST"; chmod 644 "$DEST/Multicore-instalador.exe"
echo "Publicado: $(du -h "$DEST/Multicore-instalador.exe" | cut -f1)  sha256 $(sha256sum "$DEST/Multicore-instalador.exe" | cut -c1-16)"
domain=$(grep -E '^DOMAIN=' .env 2>/dev/null | cut -d= -f2-)
[ -n "$domain" ] && echo "Endereço: https://$domain/downloads/Multicore-instalador.exe"
