-- UC 0076 (change 0071) — audit persistito delle transizioni di stato di un'app di catalogo.
--
-- app_status_audit: una riga per OGNI transizione effettiva di platform.app.status (active ⇄
-- inactive) comandata dalla console admin. È la prova di "chi ha messo in pausa quale app, quando
-- e perché": la leva è reversibile e non tocca né dati né infrastruttura, ma deve restare
-- tracciabile. NON è tenant-scoped — il catalogo è di piattaforma e l'azione vale per tutti gli
-- account insieme; l'unico soggetto registrato è l'operatore (`actor` = `sub` del JWT).
--
-- `reason` è testo libero dell'operatore: resta SOLO qui (conservazione 12 mesi come le altre prove
-- di audit nel database, AuditRetentionSweeper) e non viene mai copiato nell'evento di audit
-- strutturato che confluisce nell'archivio 12 mesi su S3/Glacier (#08) — stessa prudenza già
-- applicata alla nota della limitazione art. 18 (gdpr_restriction_audit).
--
-- L'idempotenza del comando vive nel servizio: nessuna transizione ⇒ nessuna riga qui.

CREATE TABLE platform.app_status_audit (
    id          uuid        PRIMARY KEY,
    app_id      uuid        NOT NULL REFERENCES platform.app (id),
    from_status varchar(32) NOT NULL,   -- active | inactive
    to_status   varchar(32) NOT NULL,   -- active | inactive
    actor       varchar(64) NOT NULL,   -- sub del JWT dell'operatore di piattaforma
    reason      varchar(512),           -- motivazione facoltativa dell'operatore
    executed_at timestamptz NOT NULL
);
CREATE INDEX ix_app_status_audit_app ON platform.app_status_audit (app_id, executed_at DESC);
