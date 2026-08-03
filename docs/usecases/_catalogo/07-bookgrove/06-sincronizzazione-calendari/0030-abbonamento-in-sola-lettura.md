# 0030 — Abbonamento in sola lettura

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 06 — Sincronizzazione con i calendari esterni
**Storia**: `0030` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che non vuole dare a un programma l'accesso al proprio calendario
> voglio comunque vedere i miei appuntamenti di lavoro sul telefono
> così da avere il beneficio principale senza autorizzare nessuno a leggere le mie cose.

**Contesto.** È il ripiego dichiarato dell'epica, e ha due ragioni. La prima è di rispetto: l'autorizzazione a
leggere un calendario personale è una richiesta importante, e una parte degli operatori non la darà mai. La
seconda è di robustezza: se un fornitore cambia le regole o il collegamento si rompe, questa via continua a
funzionare, perché non dipende da nessuna autorizzazione. Copre anche il calendario di Apple, per il quale non
esiste un collegamento nel senso della storia `0027`.

## 2. Requisiti funzionali

1. **RF-1** — Ogni risorsa può esporre un **indirizzo di sottoscrizione** al proprio calendario di lavoro, che si
   aggiunge a qualsiasi programma di calendario.
2. **RF-2** — Il calendario esposto contiene gli appuntamenti futuri di quella risorsa e un margine di passato,
   con titolo **minimizzato** come nella storia `0028`.
3. **RF-3** — L'indirizzo è **segreto**, lungo e casuale, e si può rigenerare: rigenerandolo il precedente smette
   di funzionare.
4. **RF-4** — L'indirizzo è di sola lettura: da lì non si può cambiare nulla.
5. **RF-5** — Quando una prenotazione cambia o viene disdetta, il calendario sottoscritto lo riflette al successivo
   aggiornamento del programma che lo legge; il ritardo è dichiarato all'utente, perché dipende dal suo programma
   e non da noi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione dichiarata, stessa famiglia della storia `0018`.** L'indirizzo
  di sottoscrizione è un **gettone di capacità**: firmato, di ambito singolo (una risorsa, sola lettura,
  finestra temporale limitata), revocabile, memorizzato come impronta. Il `tenant_id` sta dentro il gettone, mai
  nella richiesta. Non è un'identità e non diventa una sessione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta pubblica
  `GET /api/prenotazioni/v1/pubblico/calendario/{gettone}` che restituisce un calendario in formato standard;
  limitazione di frequenza per indirizzo di rete; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova oltre a un gettone nella stessa tabella della storia `0018`,
  distinto dall'ambito.
- **RT-4 — Minimizzazione.** Il contenuto è lo stesso della storia `0028`: nome dell'attività, ora, durata; senza
  nome del cliente e senza nome del servizio, salvo scelta esplicita dell'operatore con avviso.
- **RT-5 — Modulo frontend (§3, §5).** L'indirizzo si copia dalla scheda della risorsa, con un pulsante per
  rigenerarlo e la spiegazione di cosa comporta; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Interfaccia e spiegazioni in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Nessuna voce nuova oltre al gettone, già dichiarato. Va però detto nel
  manifesto che il contenuto esposto è minimizzato: è la garanzia che rende accettabile un indirizzo
  raggiungibile da chiunque lo possieda.
- **RT-8 — Registrazione eventi (§14).** `calendario sottoscritto letto`, `gettone rifiutato` con `tenant_id`,
  `app_id` e correlazione.
- **RT-9 — Prove (§11).** Prova di sicurezza: gettone revocato, manomesso, di un'altra risorsa, di un altro
  account — tutti respinti allo stesso modo.

## 4. Criteri di accettazione

**CA-1 — Sottoscrizione**
- **Dato** una risorsa con appuntamenti · **Quando** si aggiunge l'indirizzo a un programma di calendario
- **Allora** compaiono gli appuntamenti futuri con ora e durata giuste

**CA-2 — Minimizzazione**
- **Dato** il calendario sottoscritto · **Quando** si guarda un evento · **Allora** non contiene né il nome del
  cliente né quello del servizio

**CA-3 — Rigenerazione**
- **Dato** un indirizzo condiviso per errore · **Quando** lo si rigenera · **Allora** il precedente smette di
  funzionare immediatamente

**CA-4 — Gettone non valido**
- **Dato** un gettone revocato, manomesso o di un'altra risorsa · **Quando** lo si usa · **Allora** la risposta è
  la stessa in tutti i casi e non rivela nulla

**CA-5 — Sola lettura**
- **Dato** un tentativo di scrivere attraverso quell'indirizzo · **Quando** lo si esegue · **Allora** è rifiutato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla generazione del calendario in formato standard e di **integrazione** sulla rotta
      pubblica;
- [ ] prova di **isolamento fra account** in forma di **prova di sicurezza** sul gettone (RT-9);
- [ ] **prova end-to-end**: *rimando* — la verifica richiede un programma di calendario esterno; motivo e storia
      proprietaria dichiarati in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la dichiarazione di minimizzazione del contenuto esposto;
- [ ] **registro delle decisioni** compilato: il gettone di sola lettura come terza forma di accesso senza
      autenticazione, dopo l'identificativo di sede e il gettone di prenotazione;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | servono gli appuntamenti da esporre |
| storia `0018` | riusa la stessa famiglia di gettoni di capacità |

## 7. Fuori ambito

- la scrittura dal programma di calendario dell'operatore: per definizione, è sola lettura;
- l'invio dell'appuntamento come allegato di calendario nel messaggio di conferma: utile e piccolo, ma appartiene
  al motore dei messaggi (storia `0022`) e va valutato lì.

## 8. Punti aperti

Nessuno.
