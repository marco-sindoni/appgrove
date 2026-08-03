# 0031 — Portale pubblico degli articoli

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 06 — Base di conoscenza e portale del richiedente
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che riceve ogni settimana le stesse cinque domande
> voglio una pagina pubblica, con il nome della mia azienda, dove il cliente trova la risposta da solo
> così da ridurre il numero di richieste che arrivano e da poter rispondere «trova tutto qui» invece di riscrivere.

**Contesto.** Gli articoli della storia `0029` esistono ma li vede solo chi lavora dentro l'app: finché restano lì,
servono a metà. Il portale è ciò che li rende utili anche quando nessuno risponde, di notte e nel fine settimana, ed
è quello che rende sensato il collegamento inserito nella risposta (storia `0030`). È anche **la superficie più
esposta dell'applicazione dopo il modulo di contatto**: una pagina raggiungibile da chiunque, senza accesso, dentro
un'app dove il resto dei dati è il contenuto delle conversazioni fra il cliente e i suoi clienti. Per questo il
perimetro qui è ridotto all'osso e dichiarato per iscritto: **sola lettura, solo articoli pubblicati, nient'altro**.

## 2. Requisiti funzionali

1. **RF-1** — Ogni account ha un portale raggiungibile da un indirizzo che contiene un **identificativo pubblico
   opaco**, non indovinabile e non sequenziale; il portale si attiva e si disattiva dall'account e nasce disattivato.
2. **RF-2** — Il portale mostra **solo** gli articoli in stato `published` di quell'account, raggruppati per
   categoria, con la variante di lingua scelta dal visitatore fra quelle in cui esistono articoli.
3. **RF-3** — Il portale ha una ricerca a testo libero limitata agli articoli pubblicati di quell'account.
4. **RF-4** — Il portale porta il nome e il logo dell'azienda cliente e un'**informativa breve** che dice chi tratta
   i dati (titolare è l'azienda cliente, appgrove è il fornitore che tratta per suo conto); il testo dell'informativa
   lo fornisce l'azienda cliente, l'app le dà il posto dove metterlo e non lo scrive al posto suo.
5. **RF-5** — Un articolo **ritirato** o cancellato smette di essere raggiungibile immediatamente; il suo indirizzo
   restituisce la stessa pagina neutra di un articolo mai esistito, senza rivelare che sia esistito.
6. **RF-6** — Portale disattivato, identificativo inesistente o manomesso portano tutti alla **stessa** pagina
   neutra; le richieste al portale sono soggette a un limite di frequenza per indirizzo di rete, e il superamento
   non rivela nulla su cosa esista.
7. **RF-7** — La superficie pubblica è **in sola lettura**: non accetta metodi di scrittura, non ha moduli, non
   raggiunge nessun'altra entità dell'applicazione — non richieste, non richiedenti, non messaggi, non operatori,
   non bozze.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione dichiarata e ristretta.** Qui **non c'è un token di accesso**: il
  `tenant_id` si ricava dall'**identificativo pubblico opaco del portale**, verificato a ogni richiesta contro la
  tabella dei portali. Un `tenant_id` che arrivasse dai parametri, dal corpo o da un'intestazione viene **ignorato**,
  esattamente come sulle rotte autenticate. La deviazione è ristretta a tre condizioni cumulative, tutte da
  verificare nel codice e nelle prove: **sola lettura**, **solo entità `knowledge_article` e `knowledge_category`**,
  **solo articoli in stato `published` e non cancellati**. È lo stesso schema del punto 5 dei rischi della
  descrizione dell'applicazione, applicato al caso più semplice: qui non c'è nemmeno un gettone personale, perché non
  c'è nulla di personale da mostrare.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche separate e riconoscibili:
  `GET /api/helpdesk/v1/public/{portale}/articles`, `.../articles/{slug}`, `.../categories`. Nessun verbo di
  scrittura sul prefisso `/public/`: un `POST`, `PATCH` o `DELETE` risponde `405`. Errori in
  `application/problem+json` con corpi che non distinguono «non esiste» da «non pubblicato»; limitazione di frequenza
  per indirizzo di rete con risposta `429`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__knowledge_portal.sql` sullo schema `app_helpdesk`: tabella
  `knowledge_portal` con `tenant_id`, chiave primaria UUID versione 7, identificativo pubblico opaco **univoco a
  livello globale**, stato attivo, nome visualizzato, riferimento al logo, testo dell'informativa, colonne di
  controllo e cancellazione logica. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Due superfici distinte. Dentro il backoffice: la sezione «Portale» che attiva,
  disattiva e configura, con l'anteprima. Fuori: la **pagina pubblica non vive dentro il backoffice** — è una
  superficie a sé che non carica nulla che richieda autenticazione, non conosce il contesto della shell e usa gli
  stessi token del sistema di design con il colore-categoria `teal`, in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** **Due elenchi da non confondere.** L'interfaccia dell'operatore (sezione «Portale»
  nel backoffice) passa dallo spazio-nomi `helpdesk` ed è presente in `en, it, fr, es, de`. La **cornice** della
  pagina pubblica (etichette «cerca», «categorie», «nessun risultato») segue la lingua del visitatore fra le stesse
  cinque, con ricaduta su `en`; il **contenuto** invece è nella lingua dell'articolo, che è un dato del cliente e può
  essere una qualsiasi.
- **RT-6 — Varchi e quota (§6, §7).** La superficie pubblica non attraversa la catena dei varchi dell'utente, perché
  non c'è un utente: al suo posto valgono l'esistenza del portale, il suo stato attivo e il limite di frequenza. Non
  consuma la metrica `agents`. **Proposta**: con abbonamento `canceled` il portale si spegne e mostra la pagina
  neutra, perché è una funzione commerciale e non un diritto dell'interessato — ma spegnere una pagina pubblica di un
  cliente è un effetto verso l'esterno e **la decisione è dello sviluppatore** (vedi §8). La configurazione del
  portale richiede ruolo `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di lettura nuovo: `cerca_articoli` è già dichiarato
  dalla storia `0030` e legge gli articoli dell'account, non il portale. Si dichiara
  `attiva_portale(stato) → esito`, marcato **scrittura con effetto verso l'esterno**, con **conferma umana
  obbligatoria**: accendere il portale rende pubblici in un colpo solo tutti gli articoli già pubblicati, e spegnerlo
  fa sparire una pagina a cui potrebbero puntare collegamenti già mandati ai clienti. Il contratto vive dentro il
  servizio; il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo, a una condizione che è un requisito**: l'indirizzo
  di rete del visitatore serve **solo** al limite di frequenza e **non si conserva** — vive in memoria per la durata
  della finestra e non finisce in nessuna tabella. Se in implementazione risultasse necessario conservarlo, allora
  serve una voce nel manifesto `docs/compliance/manifests/helpdesk.yaml` in italiano e inglese, sul modello di
  `webform.ip` (finalità: difesa dall'abuso di una superficie pubblica; conservazione: 30 giorni; cancellazione
  automatica), il campo annotato `@PersonalData` e la tabella in `exportData` e `purgeData` — e diventa una decisione
  da registrare, non un dettaglio. **Nessun tracciamento**: niente strumenti di analisi, niente cookie non tecnici,
  nessun banner di consenso. Nome e logo dell'azienda cliente non sono dati personali. Ricordare che su questa app
  appgrove è **responsabile del trattamento** per conto del cliente, non titolare: la pagina lo deve dire (RF-4).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `portale attivato`, `portale disattivato`, `articolo pubblico
  letto` e `richiesta pubblica respinta per frequenza` sono registrati con `tenant_id`, `app_id`, identificativo
  dell'articolo e identificativo di correlazione. Sulle rotte pubbliche non c'è `user_id`: il campo resta vuoto e
  **non** viene sostituito dall'indirizzo di rete, che è un dato personale e non va nei registri.

## 4. Criteri di accettazione

**CA-1 — Chiunque legge un articolo pubblicato**
- **Dato** un portale attivo con tre articoli pubblicati in due categorie
- **Quando** una persona senza alcun accesso apre l'indirizzo del portale dal telefono
- **Allora** vede il nome dell'azienda, le categorie, i tre articoli e l'informativa breve, e può cercare fra di essi

**CA-2 — Ritiro immediato**
- **Dato** un articolo pubblicato e già raggiungibile · **Quando** un operatore lo ritira
- **Allora** la richiesta pubblica successiva restituisce la pagina neutra, identica a quella di un articolo mai
  esistito, senza attese di scadenza di cache

**CA-3 — Isolamento fra account con manipolazione dell'identificativo pubblico**
- **Dato** due account `A` e `B`, ciascuno col proprio portale, i propri articoli pubblicati e le proprie bozze
- **Quando** un visitatore sostituisce l'identificativo pubblico di `A` con quello di `B`, oppure aggiunge un
  `tenant_id` nei parametri o in un'intestazione
- **Allora** il parametro forzato è **ignorato**, si vedono al più gli articoli **pubblicati** del portale il cui
  identificativo è stato indicato, e in nessun caso una bozza, un articolo cancellato o un dato di un terzo account

**CA-4 — Nessun'altra entità raggiungibile**
- **Dato** un portale attivo · **Quando** si tenta di raggiungere da `/api/helpdesk/v1/public/{portale}/…` una
  richiesta, un richiedente, un messaggio, un allegato o un operatore
- **Allora** la risposta è la stessa pagina neutra, e ogni metodo di scrittura sul prefisso `/public/` risponde `405`

**CA-5 — Portale spento e identificativo inesistente sono indistinguibili**
- **Dato** un portale disattivato, un identificativo mai esistito e un identificativo manomesso
- **Quando** si aprono tutti e tre
- **Allora** si ottiene la **stessa** risposta neutra nei tre casi, senza differenze di testo, di codice o di tempo di
  risposta apprezzabili

**CA-6 — Limite di frequenza**
- **Dato** un indirizzo di rete che supera la soglia di richieste al portale
- **Quando** manda la richiesta successiva
- **Allora** riceve `429`, la risposta non rivela cosa esista, e l'indirizzo non viene scritto in nessuna tabella né
  in nessun registro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla risoluzione dell'identificativo pubblico e sul filtro «solo pubblicati», e di
      **integrazione** sulle rotte pubbliche, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** in forma di **prova di sicurezza sulla superficie pubblica**:
      identificativo di un altro account, identificativo manomesso, portale spento, `tenant_id` forzato nei
      parametri, tentativo di raggiungere una bozza e un'altra entità, metodo di scrittura — tutti respinti allo
      stesso modo;
- [ ] **prova end-to-end**: *coprire ora* — passo «apri il portale pubblico senza accesso e trova l'articolo» del
      percorso `[J-HELPDESK]`, e registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) sia per la sezione di
      configurazione sia per la cornice della pagina pubblica;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese **solo se** l'indirizzo di rete finisce per essere
      conservato (RT-8); altrimenti la decisione «non si conserva» va scritta nel registro delle decisioni;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata **la deviazione
      sull'origine del `tenant_id` e le tre condizioni che la restringono**: è la decisione più importante della
      storia;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `attiva_portale`, con conferma umana obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali: le rotte `/public/` sono
      raggiungibili dal proxy locale grazie alla sola scoperta automatica dei servizi, senza cablaggi a mano;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0029` — articoli della base di conoscenza | Il portale mostra articoli pubblicati: senza lo stato `published` non c'è nulla da mostrare e nulla da nascondere |
| approvazione dello sviluppatore sulla superficie pubblica | L'origine del `tenant_id` da un identificativo invece che da un token è una deviazione da un invariante di piattaforma e va approvata prima dell'implementazione (punto 5 dei rischi della descrizione dell'applicazione) |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: si dichiara il contratto di `attiva_portale`, non lo si espone |

## 7. Fuori ambito

- **La scrittura e la pubblicazione degli articoli**: storia `0029`. Qui si legge soltanto.
- **La pagina con cui il richiedente segue la propria richiesta**: storia `0032`. È una superficie pubblica diversa,
  con un gettone personale e un solo oggetto: non si mescola con questa, che non ha gettoni e non mostra dati di
  persone.
- **Il suggerimento degli articoli dentro il modulo di contatto**: storia `0033`, che riusa questa stessa superficie
  in sola lettura invece di aprirne una seconda.
- **Un dominio personalizzato del cliente per il portale**: rimandato. Comporta certificati, verifica della proprietà
  del dominio e una configurazione assistita; è una storia propria, non un dettaglio di questa.
- **Il conteggio delle visualizzazioni per articolo come misura di efficacia**: rimandato; il contatore d'uso interno
  esiste già (storia `0030`) e il cruscotto è della storia `0028`.
- **La possibilità di commentare o valutare un articolo dal portale**: fuori ambito, e non per pigrizia: aprirebbe
  una superficie di **scrittura** pubblica, che è esattamente ciò che RF-7 esclude.

## 8. Punti aperti

- **Il portale deve spegnersi quando l'abbonamento non è attivo?** La proposta di RT-6 è sì, ma spegnere una pagina
  pubblica di un cliente è un **effetto verso l'esterno**: i collegamenti già mandati ai suoi clienti smettono di
  funzionare, e il danno d'immagine è del cliente, non nostro. Alternativa: lasciare il portale in sola lettura per
  un periodo di tolleranza dopo la disdetta. **La chiude lo sviluppatore.**
- **Gli articoli pubblicati devono essere indicizzabili dai motori di ricerca?** La domanda arriva dalla storia
  `0029`. Indicizzarli aumenta il valore per il cliente (chi cerca in rete trova la risposta) ma rende pubblici e
  duraturi testi che il cliente potrebbe considerare riservati, e li fa sopravvivere nelle copie dei motori anche
  dopo il ritiro. Proposta prudente: **non indicizzabile in modo predefinito**, con un interruttore per account.
  **La chiude lo sviluppatore**, perché è direzione di prodotto con effetti verso l'esterno.
- **Se l'indirizzo di rete del visitatore vada conservato per il limite di frequenza.** La proposta è di non
  conservarlo (RT-8). Se l'implementazione mostrasse che serve, diventa una classificazione di dati personali su una
  superficie pubblica: **la conferma è dello sviluppatore**, con il manifesto aggiornato prima del rilascio.
