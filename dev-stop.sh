#!/usr/bin/env sh
# Alias di `./dev.sh down` (UC 0009) — mantenuto per comodità/abitudine.
# Ferma lo stack di sviluppo locale (UC 0008). `-v` per il reset volumi. Argomenti inoltrati a `dev down`.
exec "$(dirname "$0")/dev/dev" down "$@"
