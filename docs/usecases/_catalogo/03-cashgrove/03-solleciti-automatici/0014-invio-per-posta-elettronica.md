# 0014 — Invio per posta elettronica

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta all'amministrazione
> voglio che il sollecito arrivi davvero al cliente, partendo dall'indirizzo della mia azienda
> così da poterlo richiamare in una telefonata («le ho scritto martedì») senza temere che sia finito in un limbo.

**Contesto.** È il primo canale reale e, per il segmento, il principale: costa quasi nulla, non richiede approvazioni e
tutti lo leggono. Il rischio è il recapito: i solleciti hanno il profilo perfetto per finire nella posta indesiderata —
invio automatico, molti destinatari, parole come «scaduto» e «pagamento». Da qui la scelta di far partire il messaggio
dal **dominio del creditore** con le sue credenziali, che è anche ciò che tiene il canone piatto e ci tiene fuori dal
ruolo di mittente di massa.

## 2. Requisiti funzionali

1. **RF-1** — L'account configura il proprio mittente: indirizzo, nome visualizzato e credenziali del proprio fornitore
   di posta in uscita; una verifica di prova conferma che la configurazione funziona prima di attivarla.
2. **RF-2** — Il motore prende gli invii maturi con canale «posta elettronica», compila il modello e li trasmette.
3. **RF-3** — Ogni tentativo produce un esito: accettato dal fornitore, respinto, non recapitato; l'esito è visibile
   sulla scheda del credito e nella coda.
4. **RF-4** — Un invio respinto per errore temporaneo viene ritentato con distanze crescenti, fino a un numero massimo
   di tentativi; dopo di che è marcato fallito e il debitore è segnalato come «non raggiungibile per posta».
5. **RF-5** — Un indirizzo che risulta inesistente non viene più usato: i solleciti successivi a quel debitore restano
   in coda con un avviso che chiede di correggere il recapito.
6. **RF-6** — L'utente può inviare a mano il passo successivo di un credito, senza aspettare la maturazione, dalla
   scheda del credito.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il mittente configurato appartiene a un solo account e non è utilizzabile da
  altri; ogni invio filtra per `tenant_id` preso dal token verificato o, per la lavorazione, dall'account in
  elaborazione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PUT /api/crediti/v1/impostazioni/mittente`,
  `POST /api/crediti/v1/impostazioni/mittente/verifica` e `POST /api/crediti/v1/crediti/{id}/sollecito`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `mittente_posta` sullo schema `app_crediti` con `tenant_id`,
  chiave UUID versione 7, colonne di controllo e cancellazione logica. **Le credenziali non si scrivono in chiaro**:
  si conservano cifrate e non escono mai da nessuna rotta di lettura, nemmeno mascherate a metà.
- **RT-4 — Modulo frontend (§3, §5).** Configurazione del mittente nella sezione *Impostazioni*, con verifica in linea;
  esiti degli invii sulla scheda del credito; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i messaggi di errore del fornitore tradotti in
  parole comprensibili, passano dallo spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'invio non consuma la metrica `crediti_monitorati`. Con abbonamento in `past_due`
  gli invii continuano; con `canceled` si fermano e la coda lo dichiara.
- **RT-7 — Esposizione conversazionale (§12).** L'invio a mano di RF-6 è il gemello di `invia_sollecito`, che sarà
  dichiarato nella storia `0029` come **scrittura irreversibile con conferma umana obbligatoria**: qui si costruisce la
  funzione, là si espone il contratto. Dipendenza dichiarata: UC 0061-0063, non implementati.
- **RT-8 — Dati personali (§10).** Il messaggio inviato contiene dati personali del debitore e viaggia attraverso un
  fornitore esterno: quel fornitore è un **responsabile esterno del trattamento** e va dichiarato nell'elenco dei
  fornitori e nell'informativa. La tabella `mittente_posta` contiene un indirizzo che può essere personale ed è aggiunta
  al manifesto, a `exportData` e a `purgeData`. Il **testo inviato** si conserva nel registro dei solleciti
  (storia `0017`), non qui.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «sollecito trasmesso», «trasmissione respinta», «indirizzo
  inesistente» sono registrati con `tenant_id`, `app_id`, `user_id` (o «sistema»), identificativo del credito, esito e
  identificativo di correlazione. **Mai** l'indirizzo del destinatario né il testo.
- **RT-10 — Condotta verso il debitore.** Un solo destinatario, quello del debitore: nessun campo in copia, nessuna
  copia al creditore che contenga il testo (il creditore lo rilegge nel registro dei solleciti).

## 4. Criteri di accettazione

**CA-1 — Invio riuscito**
- **Dato** un mittente configurato e verificato e un invio maturo in coda
- **Quando** il motore lo trasmette
- **Allora** l'esito è «accettato», la scheda del credito mostra data e ora, e la coda non contiene più quell'invio

**CA-2 — Mittente non verificato**
- **Dato** un mittente configurato ma con verifica fallita · **Quando** matura un invio · **Allora** l'invio resta in
  coda con motivo «mittente non verificato» e l'app lo dice nella sezione *Impostazioni*

**CA-3 — Errore temporaneo**
- **Dato** un fornitore che risponde con un errore temporaneo
- **Quando** il motore trasmette
- **Allora** l'invio viene ritentato con distanze crescenti e, superato il numero massimo di tentativi, è marcato
  fallito

**CA-4 — Indirizzo inesistente**
- **Dato** un debitore con indirizzo inesistente · **Quando** la trasmissione riporta il mancato recapito definitivo ·
  **Allora** il debitore è marcato «non raggiungibile per posta» e i solleciti successivi restano in coda con l'avviso
  di correggere il recapito

**CA-5 — Credenziali mai leggibili**
- **Dato** un mittente configurato · **Quando** si legge la configurazione da qualsiasi rotta · **Allora** le
  credenziali non compaiono in nessuna forma, e nemmeno nei registri o nelle diagnostiche

**CA-6 — Isolamento fra account**
- **Dato** due account con mittenti diversi · **Quando** entrambi hanno invii maturi · **Allora** ogni messaggio parte
  dal mittente del proprio account e nessuna credenziale è utilizzabile dall'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla politica dei tentativi e di **integrazione** con un fornitore **simulato** — nelle prove
      non parte mai un messaggio vero verso un indirizzo reale, e gli indirizzi di prova sono su dominio `*.test`;
- [ ] prova di **isolamento fra account** su mittente e invii;
- [ ] **prova end-to-end**: *coprire ora, in parte* — il percorso `[J-CREDITI]` nasce con la storia `0031`; qui si
      registra la voce `da-coprire` nel registro di copertura con proprietaria la storia `0031`, motivo: il percorso
      completo richiede anche la sospensione (`0016`);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `mittente_posta` e con la dichiarazione del fornitore esterno;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta del mittente del cliente invece di un
      mittente di piattaforma, e sul perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta in questa storia, ma la funzione di invio a mano è
      annotata come base di `invia_sollecito`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | Serve la coda degli invii maturi |
| Fornitore di posta in uscita del cliente | È lui che trasmette; senza configurazione l'app non manda nulla |

## 7. Fuori ambito

- I canali brevi e la messaggistica: storia `0015`.
- La conferma di lettura: non si implementa. È un dato di comportamento del destinatario che non serve al flusso e che
  aggiungerebbe un trattamento non necessario.
- Il registro completo con il testo conservato: storia `0017`.

## 8. Punti aperti

Se lo sviluppatore preferisse un mittente **di piattaforma** invece del mittente del cliente, cambierebbero tre cose: il
recapito peggiorerebbe, appgrove diventerebbe mittente di comunicazioni verso terzi e il costo diventerebbe nostro. La
proposta è il mittente del cliente; la decisione è sua.
