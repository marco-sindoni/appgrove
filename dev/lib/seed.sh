# shellcheck shell=bash
# dev seed — carica il seed deterministico (UC 0009). STUB → finalizzato da UC 0011.

cmd_seed() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev seed — carica il seed deterministico (idempotente, condiviso dev↔E2E).
STUB: il contenuto del seed (cast multi-tenant, catalogo, subscription) arriva con UC 0011.
EOF
    return 0
  fi
  warn "dev seed: nessun seed ancora definito → implementazione in UC 0011 (cast multi-tenant + catalogo + subscription)."
  return 0
}
