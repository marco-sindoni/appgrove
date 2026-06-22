#!/usr/bin/env bash
# Launcher root per il dispatcher dev/ (UC 0009): inoltra tutto a dev/dev.
# Esempi:  ./dev.sh doctor   ./dev.sh setup   ./dev.sh up   ./dev.sh down -v
# Per digitare `dev <comando>` senza ./dev.sh:  alias dev="$(pwd)/dev/dev"
exec "$(cd "$(dirname "$0")" && pwd)/dev/dev" "$@"
