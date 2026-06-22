#!/usr/bin/env sh
# Alias di `./dev.sh up` (UC 0009) — mantenuto per comodità/abitudine.
# Avvia lo stack di sviluppo locale (UC 0008). Argomenti inoltrati a `dev up`.
exec "$(dirname "$0")/dev/dev" up "$@"
