# 0023 — Canale di messaggistica

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 05 — Promemoria, acconti e mancate presentazioni
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che il promemoria arrivi dove i miei clienti guardano davvero, cioè sul telefono
> così da non affidare la riduzione delle mancate presentazioni a una casella di posta che nessuno apre.

**Contesto.** È il canale che il mercato considera decisivo — tutti i concorrenti lo tolgono dal piano gratuito
(§2.2 della descrizione) — ed è anche quello con più vincoli. Il fornitore consente di iniziare una conversazione
**solo** dopo un consenso esplicito e **solo** con un modello approvato in anticipo; il promemoria di
appuntamento rientra fra i casi ammessi della categoria di servizio, che deve restare non promozionale; e dal
1° luglio 2025 si paga **per messaggio consegnato**, con tariffe che variano per Paese e volume (§2.3, punto 4).

> ⚠️ **Questa storia non parte finché non è deciso chi paga i messaggi** (§5 e §11, punto 5, della descrizione).
> La raccomandazione è che il canale sia **collegato dal cliente con il proprio contratto**, come fa
> l'applicazione 05 del catalogo, così che il costo variabile non entri in un canone piatto: ma è una decisione
> di prezzo e di prodotto, quindi una fermata di escalation dello sviluppatore.

## 2. Requisiti funzionali

1. **RF-1** — L'attività collega il proprio canale di messaggistica dalle impostazioni, e vede a colpo d'occhio
   se il collegamento è attivo, in attesa o rotto.
2. **RF-2** — I testi dei promemoria sul canale sono **modelli approvati**, non testo libero, gestiti come
   contenuto versionato dell'app e disponibili nelle cinque lingue.
3. **RF-3** — Il canale si usa solo verso i clienti che hanno il consenso specifico registrato (storia `0021`);
   per tutti gli altri si usa la posta elettronica, senza che l'attività debba fare nulla.
4. **RF-4** — Se il canale rifiuta il messaggio — modello non approvato, consenso mancante, numero non valido —
   l'esito è mostrato in parole comprensibili e il messaggio **ricade** sulla posta elettronica.
5. **RF-5** — L'attività vede quanti messaggi sono partiti nel mese sul canale, perché è la grandezza che le
   costa.
6. **RF-6** — Il messaggio breve tradizionale è ammesso come canale alternativo con le stesse regole, se
   l'attività lo preferisce.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le credenziali del canale sono per `tenant_id` e non sono mai leggibili
  da un altro account; nessun messaggio può essere inviato usando il canale di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT|DELETE /api/prenotazioni/v1/canali/messaggistica`;
  errori in `problem+json` con codici stabili per «canale non collegato», «modello non approvato», «consenso
  mancante»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V15__canale_messaggistica.sql`: tabella del collegamento con
  `tenant_id`, UUID versione 7, colonne di controllo e i segreti **cifrati**, mai in chiaro e mai nei registri.
- **RT-4 — Nessuna chiamata sincrona sul percorso caldo.** L'invio è asincrono e ripetibile con la stessa chiave
  di idempotenza della storia `0022`; il fallimento del fornitore non deve mai far fallire una prenotazione.
- **RT-5 — Modulo frontend (§3, §5).** Impostazioni del canale con stato leggibile e anteprima dei modelli nelle
  cinque lingue; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Interfaccia e modelli in `en, it, fr, es, de`; il modello usato segue la lingua
  del destinatario.
- **RT-7 — Dati personali (§10).** Il fornitore del canale **riceve numero di telefono e contenuto del
  promemoria**: va dichiarato come fornitore che tratta dati per nostro conto, elencato nell'informativa e
  aggiunto al manifesto in italiano e inglese. Vale in pieno la minimizzazione della storia `0022`: il modello
  predefinito **non** contiene il nome del servizio.
- **RT-8 — Registrazione eventi (§14).** `canale collegato`, `messaggio accettato dal fornitore`, `messaggio
  rifiutato` con `tenant_id`, `app_id`, correlazione e codice del fornitore — **mai il numero né il testo**.
- **RT-9 — Prove (§11).** Il fornitore è **sempre simulato** in locale e nelle prove: nessun messaggio vero parte
  mai da una prova automatica.

## 4. Criteri di accettazione

**CA-1 — Collegamento e primo invio**
- **Dato** un account che collega il canale e un cliente con consenso
- **Quando** scatta il promemoria
- **Allora** parte sul canale di messaggistica con il modello nella lingua del cliente

**CA-2 — Nessun consenso, nessun canale**
- **Dato** un cliente senza consenso specifico · **Quando** scatta il promemoria · **Allora** parte per posta
  elettronica, e l'esito lo dice

**CA-3 — Rifiuto del fornitore**
- **Dato** un numero non valido · **Quando** il fornitore rifiuta · **Allora** l'esito è comprensibile («il numero
  non risulta raggiungibile»), il messaggio ricade sulla posta e nulla resta appeso

**CA-4 — Canale non collegato**
- **Dato** un account che non ha collegato il canale · **Quando** attiva il promemoria su messaggistica
- **Allora** riceve un messaggio che spiega cosa manca, e i promemoria continuano per posta

**CA-5 — Segreti protetti**
- **Dato** un collegamento attivo · **Quando** si esaminano registri, esportazioni e risposte delle interfacce
- **Allora** nessun segreto compare in chiaro da nessuna parte

**CA-6 — Isolamento fra account**
- **Dato** due account, uno solo con il canale collegato · **Quando** il secondo prova a inviare · **Allora** non
  può, e non vede nulla del collegamento altrui

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla scelta del canale e di **integrazione** con fornitore simulato;
- [ ] prova di **isolamento fra account** su credenziali e invii;
- [ ] **prova end-to-end**: *rimando* — il canale non entra nei percorsi automatici perché il fornitore è
      simulato; motivo e storia proprietaria dichiarati in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei modelli in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il fornitore del canale, in italiano e inglese;
- [ ] **registro delle decisioni** compilato: **chi paga i messaggi e con quale meccanismo**, più la
      minimizzazione del contenuto;
- [ ] avvio locale invariato, con fornitore simulato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0022` | il motore dei messaggi esiste già, qui si aggiunge un canale |
| storia `0021` | il consenso specifico al canale |
| **decisione dello sviluppatore** su chi paga i messaggi | cambia il modello di prezzo dell'app |
| coordinamento con l'app 05 del catalogo | per non costruire due volte la stessa integrazione (§10 della descrizione) |

## 7. Fuori ambito

- la conversazione in entrata: se il cliente risponde al promemoria, questa app **non** gestisce la
  conversazione. È il perimetro dell'applicazione 05.

## 8. Punti aperti

**Costo per messaggio in Italia.** Non ho una cifra affidabile (§2.7 della descrizione): so che il messaggio di
servizio costa molto meno di quello promozionale e che esiste una finestra gratuita in risposta all'utente, ma la
tabella è per Paese e per volume. Serve leggerla prima di fissare qualunque cosa nel listino.
