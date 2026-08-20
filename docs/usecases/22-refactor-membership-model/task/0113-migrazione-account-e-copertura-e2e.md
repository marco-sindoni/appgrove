# Piano di lavoro — UC 0113 · Migrazione degli account esistenti e copertura end-to-end

**Storia**: [0113](../story/0113-migrazione-account-e-copertura-e2e.md) · **Aree toccate**: `services/core`, `services/crm`, `tools/platform-e2e`, `docs/testing`
**Dimensione stimata**: grande · **Prerequisiti**: tutte le storie precedenti dell'epica (0098–0112, 0114, 0115)

## Passo 1 — La migrazione dei dati

**File nuovo di migrazione** in `services/core/src/main/resources/db/migration/`, in **quattro istruzioni
distinte e commentate**, nell'ordine:

1. **accessi dai ruoli attuali** — inserimento in `platform.app_access` di una riga per ogni terna (account,
   applicazione con diritto, utente non-owner), col ruolo tradotto: `admin` → `admin`, `member` → `editor`.
   La sorgente delle applicazioni con diritto è la stessa regola dei diritti d'accesso: se scriverla in
   linguaggio di interrogazione è troppo delicato, farlo con un comando applicativo eseguibile una volta
   (esiste già il precedente dei comandi di manutenzione del core: cercare `MigrateCommand`);
2. **accessi dai posti del Mini-CRM** — per ogni riga di `app_crm.seat`, una riga di accesso su quella
   applicazione con ruolo `editor` (o `admin` se la persona era `admin` di piattaforma);
3. **ruolo di piattaforma** — `update platform.membership set role = 'member' where role = 'admin'` (dopo UC 0116 il ruolo vive sull'appartenenza). **Dopo** i due
   passi precedenti, che quel valore lo leggono;
4. **inviti in attesa** — azzeramento del ruolo (colonna che resta ma non si usa più).

**File nuovo**: la **migrazione inversa**, scritta e provata prima del rilascio. Non serve che sia elegante;
serve che esista.

## Passo 2 — Il controllo di sicurezza sui posti

Il nodo economico è **chiuso**: nessun account supera i tre posti gratuiti, perché la piattaforma è ancora
solo in locale (risposta dello sviluppatore). Niente periodo di grazia, niente comunicazione commerciale,
niente addebito da far partire.

Resta **un controllo**, poche righe dentro la migrazione: se un account risultasse oltre i tre posti, la
migrazione **si ferma** con un messaggio esplicito invece di proseguire. Serve a un caso solo, ma decisivo:
che il presupposto sia invecchiato (per esempio si è andati in cloud e ci sono clienti veri) e che qualcuno
stia per addebitare denaro senza averlo deciso.

## Passo 3 — I percorsi di piattaforma

**File nuovo**: `tools/platform-e2e/journeys/J-ROLES.spec.ts` — «la stessa applicazione vista dai quattro
ruoli»:

1. iscrizione di un account nuovo, con la casella di posta di prova già in uso;
2. l'owner invita tre persone (il simulatore del pagamento copre l'addebito) e ne accetta gli inviti;
3. le abilita al Mini-CRM come `admin`, `editor`, `viewer`;
4. **entra come ognuna** e verifica: voci di menu presenti e assenti, applicazioni visibili, comandi attivi e
   disabilitati nella schermata dei contatti, che cosa vede nella schermata degli utenti;
5. l'`admin` abilita una quarta persona esistente; l'`editor` tenta la stessa cosa e non può.

**File nuovo**: `tools/platform-e2e/journeys/J-SEATS.spec.ts` — «il ciclo di vita del posto»: superamento
della franchigia con importo mostrato, indicazione per la cessazione, invito bloccato, annullamento, invito
riuscito.

Etichetta del percorso in testa al titolo di ogni test (`test('[J-ROLES] …')`): il controllo del registro la
pretende.

## Passo 4 — Il registro di copertura

**Modifica**: [docs/testing/copertura-e2e.yaml](../../../testing/copertura-e2e.yaml) —

1. **rimozione** delle esenzioni `non-implementato` per le storie implementate (0100, 0106, 0107, 0108, 0109,
   0110, 0111) e loro inserimento fra gli use case con superficie;
2. **aggiunta** dei due percorsi nuovi con i loro use case;
3. **aggiornamento** dei percorsi di livello 2 estesi dalle storie precedenti;
4. le storie senza superficie (0098, 0099, 0101, 0102, 0103, 0104, 0105, 0112, 0113) restano esenti con la
   categoria giusta — attenzione: `senza-superficie`, **non** `non-implementato`, una volta implementate.

Verifica: `node tools/e2e-coverage/check.mjs`.

## Passo 5 — Il rilascio

Ordine, da scrivere anche nel registro delle decisioni della change:

1. servizi (tolleranti sia al valore vecchio sia al nuovo);
2. migrazione dei dati;
3. frontend;
4. verifica su tre account reali;
5. promemoria datato per togliere la tolleranza sui token vecchi (UC 0099).

## Verifica finale

```bash
./run-tests.sh            # l'intera suite, senza parametri
node tools/e2e-coverage/check.mjs
```

## Trappole note

1. **L'ordine delle istruzioni della migrazione**: convertire il ruolo di piattaforma prima di aver creato gli
   accessi cancella l'informazione che serve a tradurli. È irreversibile e va provato su una copia.
2. **La migrazione inversa scritta dopo il rilascio** non è una migrazione inversa: è una speranza.
3. **Il trattamento economico degli account esistenti** non è una decisione tecnica: chiedere, non dedurre.
4. **La suite intera**, non solo le aree toccate: è la storia che chiude l'epica e tocca tutto.
