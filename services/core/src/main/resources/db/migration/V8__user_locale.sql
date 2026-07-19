-- UC 0018 (change 0040) — lingua dell'utente per le email di autenticazione.
--
-- `locale` è l'UNICA fonte di verità della lingua dell'utente: la valorizza il servizio auth alla
-- registrazione (lingua attiva dell'interfaccia) e la rilegge per scegliere il template EN/IT di
-- verifica, reimpostazione password e invito.
--
-- In cloud la Lambda che compone le email di Cognito NON legge questa colonna: il servizio auth le
-- passa la lingua già risolta come parametro della chiamata (ClientMetadata). È una scelta
-- deliberata — farla leggere dal database significherebbe metterla in rete privata, con connessione
-- a Postgres e avvio a freddo, per scegliere fra due lingue.
--
-- Dato personale (manifesto `platform`, trattamento "email transazionali di autenticazione", base
-- contrattuale #13 B): è una preferenza dell'utente, non un dato di categoria particolare.
--
-- Default 'en' coerente col ripiego applicativo (#13 G38: EN/IT per le email di auth; le altre
-- lingue riguardano i contenuti pubblici del sito). NOT NULL: la lingua non è mai "ignota" a valle —
-- chi non l'ha espressa ricade sull'inglese, e il codice non deve gestire un terzo caso.
ALTER TABLE platform.users
    ADD COLUMN locale varchar(8) NOT NULL DEFAULT 'en';

-- Vincolo esplicito invece di una tabella di lookup: i valori ammessi sono due e cambiano solo con
-- una decisione di prodotto (che passerà comunque da una migrazione).
ALTER TABLE platform.users
    ADD CONSTRAINT ck_users_locale CHECK (locale IN ('en', 'it'));

COMMENT ON COLUMN platform.users.locale IS
    'Lingua dell''utente per le email transazionali (UC 0018): ''en'' | ''it''. Fonte di verità unica.';
