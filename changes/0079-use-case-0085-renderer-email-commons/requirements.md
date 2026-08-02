# Change 0079: Unificazione in `services/commons` dei renderer dei template email

**Branch**: `change/0079-use-case-0085-renderer-email-commons`
**Aree**: `services/commons`, `services/auth`, `services/core`
**Data**: 2026-08-02
**Autore**: Platform Engineering (modalità fast)
**Use case sorgente**: [`docs/usecases/17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md`](../../docs/usecases/17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md)
**Tocca dati personali?**: No — è una rifattorizzazione interna di codice di resa. Nessuna nuova tabella,
nessun nuovo trattamento, nessun manifesto dati o RoPA da aggiornare. Gli indirizzi email trattati sono
esattamente quelli di prima, per le stesse finalità.

## Problema / Obiettivo

La logica Java che rende le email — risolve le stringhe della lingua e riempie l'impaginazione condivisa —
esiste oggi in **due copie quasi identiche**:

- `EmailTemplates` in `services/auth` (verifica indirizzo, reimpostazione password, invito — UC 0018);
- `NewsletterEmailRenderer` in `services/core` (conferma iscrizione newsletter — UC 0039), che nel proprio
  commento si dichiara "il gemello compatto" del primo.

Due copie divergono in silenzio: si corregge l'escape da una parte e non dall'altra, si aggiunge una lingua
in un servizio e l'altro resta indietro. I **testi** sono già a sorgente unica in `shared/email-templates` e
vanno bene così; il difetto è la duplicazione della **logica di resa**.

Obiettivo: **un solo renderer** in `services/commons`, con **set di lingue parametrizzabile**, usato da
entrambi i servizi. È una rifattorizzazione **a comportamento invariato**: nessuna email cambia contenuto,
resa o byte prodotti.

## Scope

1. **`services/commons`** — nuova area `email` con:
   - il renderer condiviso che esegue i due passaggi (stringhe della lingua risolte contro i valori dinamici;
     stringhe risolte che riempiono l'impaginazione condivisa `layout.html`/`layout.txt`), mantenendo
     l'**escape** dei valori nella versione grafica e la **guardia sui segnaposto non risolti**;
   - la rappresentazione del **set di lingue** (lingue ammesse + lingua di ripiego) come parametro del
     renderer, con la normalizzazione delle forme comuni (`it`, `it-IT`, `IT_it` → `it`);
   - il tipo di ritorno unico con oggetto, corpo testuale e corpo grafico.
2. **`services/auth`** — usa il renderer condiviso per `verify` / `reset` / `invite`; la copia locale si
   riduce a un adattatore sottile (resta il punto di iniezione già usato dal servizio email) e la sua logica
   di resa sparisce. La normalizzazione della lingua di auth (usata anche fuori dalle email) non duplica più
   l'algoritmo.
3. **`services/core`** — usa il renderer condiviso per `newsletter-confirm`; la copia locale si riduce a un
   adattatore sottile, mantenendo il metodo di normalizzazione della lingua già richiamato da altre parti
   della newsletter.
4. **Test** — la copertura della resa (lingue, sostituzioni, escape, guardia, messaggio sconosciuto, set di
   lingue ristretto) e della **parità fra le lingue dei cataloghi** vive in `services/commons`, dove ora vive
   la logica; i test di `auth` e `core` restano verdi e continuano a coprire il fatto che i template siano
   davvero nell'artefatto di quel servizio.
5. **Collaudo di parità byte-a-byte** eseguito durante la change: l'output dei due renderer attuali su un
   insieme rappresentativo di casi viene catturato **prima** del cambiamento e confrontato con l'output del
   renderer condiviso **dopo**; devono coincidere carattere per carattere.

## Fuori scope

- **I testi** `shared/email-templates/*` (cataloghi lingua e impaginazione): invariati, non si toccano.
- **Le configurazioni `pom.xml` di copia risorse** di `auth` e `core`: restano come sono (lo use case le
  dichiara invariate). In `commons` si aggiunge solo la copia verso le risorse **di test**, necessaria perché
  i test del renderer condiviso trovino i template.
- **Il Custom Message Lambda in Python** (`infra/modules/platform_shared/lambda/custom_message`): rende la
  stessa cartella con gli stessi due passaggi ma in un altro linguaggio; unificarlo con il codice Java non è
  possibile e resta fuori (tema di UC 0018).
- **Nuove lingue o nuovi tipi di messaggio**: la newsletter resta su inglese/italiano; passare alle cinque
  lingue è una scelta che appartiene a UC 0039.
- **Il trasporto delle email** (Mailpit in locale, SES in cloud, `Mailer` di Quarkus nel core): invariato.

## Criteri di accettazione

- [ ] Esiste **un unico** renderer email, in `services/commons`, con set di lingue passato come parametro
      dal servizio che lo usa.
- [ ] `services/auth` e `services/core` lo usano; nessuno dei due contiene più la logica di risoluzione dei
      segnaposto, di escape o di riempimento dell'impaginazione.
- [ ] L'output prodotto (oggetto, corpo testuale, corpo grafico) è **identico carattere per carattere** a
      quello di prima per: `verify`, `reset`, `invite` in inglese e italiano, e `newsletter-confirm` in
      inglese e italiano — verificato con una cattura fatta prima del cambiamento.
- [ ] Un set di lingue ristretto ripiega sulla lingua di default del set: la stessa classe serve un servizio
      a due lingue e un servizio con un insieme diverso senza modifiche al codice.
- [ ] La guardia sui segnaposto non risolti e l'escape della versione grafica sono ancora attivi e coperti
      da test in `services/commons`.
- [ ] `./run-tests.sh` (suite completa) verde prima del commit.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato**: non pertinente — il renderer è codice puro di resa, non esegue
  query né legge identità dalla richiesta. Gli adattatori non cambiano il modo in cui i chiamanti ottengono
  destinatario e lingua.
- **Filtro row-level `WHERE tenant_id`**: non pertinente (nessuna query).
- **Modulo Terraform `microsaas_app`**: non pertinente (nessuna infrastruttura).
- **Logging strutturato**: il renderer condiviso non introduce log nuovi; i log esistenti dei chiamanti
  (`NewsletterMailer`) restano invariati.

## Requisiti di test

- **Renderer condiviso** (`services/commons`): resa di `verify`/`reset`/`invite` e `newsletter-confirm`;
  scelta della lingua e ripiego (lingua assente, sconosciuta, variante regionale); sostituzione dei valori
  dinamici in entrambe le versioni; escape della versione grafica (la e commerciale del collegamento di
  verifica) con l'indirizzo grezzo che resta solo nella versione testuale; errore sui segnaposto non risolti;
  errore sul messaggio sconosciuto; comportamento con un set di lingue ristretto.
- **Parità fra le lingue dei cataloghi**: stessi messaggi, stessi campi, stessi segnaposto in ogni lingua;
  impaginazioni presenti e con il segnaposto del collegamento.
- **Regressione servizi**: i test esistenti di `services/auth` (resa, localizzazione, collegamenti Cognito)
  e di `services/core` (flussi newsletter) restano verdi senza essere riscritti nella sostanza.
- **Parità byte-a-byte**: collaudo eseguito nella change (cattura prima → confronto dopo), esito riportato
  nel log di implementazione. Non diventa un test permanente: legherebbe la suite ai testi, che possono
  cambiare legittimamente e sono già protetti dai test di resa e di parità lingue.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nessun contratto esterno cambia; le email restano identiche) |
| Contratto cross-area | N/A — la modifica è interna al backend; nessuna API pubblica, nessun contratto verso il frontend o l'infrastruttura |
| Version bump | patch |
