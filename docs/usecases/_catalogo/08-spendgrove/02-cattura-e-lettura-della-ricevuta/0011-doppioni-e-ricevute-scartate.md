# 0011 — Doppioni e ricevute scartate

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 02 — Cattura e lettura della ricevuta
**Storia**: `0011` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che approva le note spese
> voglio che l'app riconosca quando la stessa ricevuta è stata caricata due volte e che si possa scartare una foto
> sbagliata cancellandola davvero
> così da non rimborsare due volte lo stesso pranzo e da non tenere in archivio documenti che non c'entrano nulla.

**Contesto.** Due problemi opposti che si risolvono nello stesso punto del flusso. Il **doppione** nasce da solo: il
collaboratore fotografa lo scontrino, non è sicuro che sia partito, lo rifotografa; oppure carica a fine mese una
ricevuta che aveva già caricato in trasferta. Lo **scarto** serve quando la foto non è una spesa aziendale — un
documento personale finito lì per sbaglio, la ricevuta della farmacia, una foto venuta male. Il secondo caso è
anche la mitigazione principale del rischio di categorie particolari per ingresso incidentale (descrizione, §6):
serve un modo per **togliere subito** dall'archivio ciò che non doveva entrarci.

## 2. Requisiti funzionali

1. **RF-1** — Al caricamento, se l'impronta del file coincide con quella di una ricevuta già presente
   nell'account, la nuova ricevuta è marcata come **probabile doppione** e mostra quella originale.
2. **RF-2** — Anche a impronta diversa (foto rifatta), l'app segnala il probabile doppione quando coincidono
   esercente, data e totale di una spesa già confermata; la segnalazione è un **avviso**, non un blocco.
3. **RF-3** — L'utente decide: «è la stessa» (la nuova viene scartata e collegata all'originale) oppure «sono due
   spese diverse» (l'avviso decade e non si ripresenta per quella coppia).
4. **RF-4** — Una ricevuta si può **scartare** in qualunque momento prima di entrare in una nota spese, indicando un
   motivo scelto da un elenco chiuso (doppione, non è una spesa aziendale, illeggibile, caricata per errore).
5. **RF-5** — Lo scarto per «non è una spesa aziendale» o «caricata per errore» **cancella fisicamente il file
   dall'archivio subito**, senza attendere alcuna scadenza; restano la riga di controllo e il motivo, senza il
   contenuto.
6. **RF-6** — Le ricevute rimaste in `caricata` oltre un numero di giorni configurabile sono elencate come «da
   sistemare», così che non restino a marcire in archivio ignorate da tutti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il confronto delle impronte e la ricerca dei probabili doppioni avvengono
  **solo dentro l'account**: `tenant_id` dal token verificato, filtro riga per riga. Un'impronta uguale in due
  account diversi non deve produrre alcuna segnalazione — sarebbe una fuga di informazione fra clienti.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/ricevute/{id}/scarta` con il motivo dal
  vocabolario chiuso, `POST /api/notespese/v1/ricevute/{id}/non-e-doppione`; errori in `application/problem+json`;
  definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V8__scarto_e_doppioni.sql`: colonne di scarto sulla ricevuta (motivo,
  autore, momento, riferimento all'originale) e indice sull'impronta per account. La cancellazione del **file** è
  fisica; la riga della ricevuta resta con `deleted_at` valorizzato e senza riferimento all'oggetto archiviato.
- **RT-4 — Modulo frontend (§3, §5).** Avviso di doppione nella schermata di revisione, con l'anteprima
  affiancata dell'originale; azione «Scarta» con scelta del motivo e conferma esplicita, perché cancella un file.
  Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I motivi di scarto sono **codici** con etichetta tradotta in `en, it, fr, es, de`;
  nessun testo scritto a mano nei componenti.
- **RT-6 — Varchi e quota (§6, §7).** Scartare **non** consuma quota e **non** la restituisce se la spesa era già
  confermata: la quota misura il lavoro fatto, non il risultato tenuto. Va detto nel messaggio, altrimenti sembra un
  errore di conteggio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: **lo scarto cancella un file ed è
  irreversibile**, quindi resta un gesto dell'interfaccia. Lo strumento di lettura `elenca_da_rivedere` (storia
  `0008`) segnala i probabili doppioni fra i motivi di attenzione.
- **RT-8 — Dati personali (§10).** La storia è **una misura di minimizzazione**, e va scritta come tale nel
  manifesto: la cancellazione fisica immediata del file scartato è la mitigazione dichiarata contro l'ingresso
  incidentale di categorie particolari (descrizione, §6). La riga di controllo che resta non contiene il contenuto
  del documento.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `doppione segnalato`, `ricevuta scartata` (con il **codice** del
  motivo), `file cancellato` portano `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali.

## 4. Criteri di accettazione

**CA-1 — Stesso file caricato due volte**
- **Dato** una ricevuta già presente nell'account
- **Quando** l'utente carica esattamente lo stesso file
- **Allora** la nuova ricevuta è marcata «probabile doppione», mostra l'originale, e nessuna quota viene consumata

**CA-2 — Foto rifatta della stessa spesa**
- **Dato** una spesa confermata da *Trattoria Belmonte*, 12/07, 38,00 €
- **Quando** si carica una foto diversa con gli stessi tre valori
- **Allora** compare l'avviso di probabile doppione e l'utente può dichiararla distinta, dopodiché l'avviso non
  ricompare per quella coppia

**CA-3 — Scarto con cancellazione immediata**
- **Dato** una ricevuta caricata per errore
- **Quando** l'utente la scarta con motivo «non è una spesa aziendale»
- **Allora** il file non è più recuperabile dall'archivio, la riga resta con il motivo e l'autore, e nessuna
  anteprima è più visibile

**CA-4 — Non si scarta ciò che è già in una nota**
- **Dato** una spesa già inserita in una nota spese inviata · **Quando** si tenta di scartare la sua ricevuta
- **Allora** l'operazione è respinta con `409` e il messaggio spiega che va prima tolta dalla nota

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che caricano **lo stesso identico file**
- **Quando** `B` lo carica dopo `A`
- **Allora** `B` non riceve nessuna segnalazione di doppione e non viene a sapere nulla dell'esistenza della
  ricevuta di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sul confronto delle impronte e sulla regola dei tre valori; di **integrazione** sullo scarto
      con database effimero, migrazioni vere e archivio simulato, **compresa la verifica che il file non esiste più**;
- [ ] prova di **isolamento fra account** sul riconoscimento del doppione (caso dell'impronta identica in due
      account);
- [ ] **prova end-to-end**: *coprire ora* il passo «carico due volte la stessa ricevuta e la scarto» nel percorso
      `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dei motivi di scarto in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: la cancellazione immediata del file scartato è dichiarata come misura di
      minimizzazione, in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con la scelta di non restituire quota allo scarto e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Serve l'impronta calcolata al caricamento |
| `0008` | L'avviso di doppione vive nella schermata di revisione |

## 7. Fuori ambito

- Il confronto visivo di due immagini per riconoscere la stessa ricevuta fotografata da angoli diversi: sarebbe una
  seconda elaborazione dell'immagine, con un altro fornitore e altri dati trattati. La regola dei tre valori copre
  il caso reale a costo zero.
- La ritenzione automatica delle ricevute mai confermate: qui se ne fa l'elenco, la cancellazione automatica è una
  decisione di conformità (punto aperto).

## 8. Punti aperti

- **Cancellazione automatica delle ricevute rimaste in `caricata`**: quanti giorni, e se cancellare in silenzio o
  avvisare prima. È una decisione di prodotto e di conformità insieme (rilevata come punto aperto nella storia
  `0006`): la chiude lo sviluppatore, non questa storia.
- **Se la quota vada restituita quando si scarta una spesa già confermata**: la proposta è di no, ma è una scelta
  commerciale che tocca la percezione del prezzo. Fermata di escalation.
