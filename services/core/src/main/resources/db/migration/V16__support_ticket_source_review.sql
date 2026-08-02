-- UC 0075 (change 0084) — completamento del ticketing nativo in-house.
--
-- source: da dove è arrivata la richiesta — 'form' (modulo dentro l'applicazione), 'event'
-- (evento di sistema: oggi solo l'esportazione GDPR fallita), 'email' (predisposto per la
-- ricezione su privacy@/support@ via SES → Lambda, rimandata a UC 0018/0078). È un metadato
-- OPERATIVO, non un dato personale: non entra nel manifesto dei trattamenti.
--
-- flagged_for_review: «questo ticket va guardato da un essere umano prima degli altri», acceso
-- dallo screening delle categorie particolari (art. 9). Di proposito NON registra QUALE categoria
-- sarebbe stata riconosciuta: registrarlo creerebbe un dato di categoria particolare derivato là
-- dove prima c'era solo testo libero. È minimizzazione, non pigrizia.
--
-- ix_support_ticket_queue regge l'ordinamento della coda di amministrazione: le scadenze più
-- vicine per prime, che è l'unica cosa che impedisce di mancare il termine di legge di un mese.

ALTER TABLE platform.support_ticket ADD COLUMN source             varchar(16) NOT NULL DEFAULT 'form';
ALTER TABLE platform.support_ticket ADD COLUMN flagged_for_review boolean     NOT NULL DEFAULT false;

-- I ticket già esistenti nati da un evento sono riconoscibili dal job di esportazione collegato.
UPDATE platform.support_ticket SET source = 'event' WHERE export_job_id IS NOT NULL;

CREATE INDEX ix_support_ticket_queue ON platform.support_ticket (due_at, created_at DESC);
