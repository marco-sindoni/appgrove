# 0030 — Rapporto della campagna

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 06 — Rendimento e salute della lista
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0021`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena mandato una campagna
> voglio sapere quanti messaggi sono arrivati, quanti no e — soprattutto — **quante persone del segmento sono
> state escluse e perché**
> così da capire se il problema è il messaggio, la lista o il recapito, invece di scoprirlo fra sei mesi.

**Contesto.** Il numero che manca in tutti gli strumenti della categoria è lo scarto fra «il segmento ne conteneva
1.200» e «ne sono partiti 940». Quelle 260 persone non sono un errore: sono iscritti non inviabili, recapiti
soppressi, contatti in quarantena. Se il rapporto non lo dice, il cliente pensa che l'app abbia perso dei
destinatari e, nel caso peggiore, va a cercare un modo per «forzare l'invio». Il rapporto è quindi anche il punto
in cui l'app spiega la propria regola: si scrive solo a chi si può. Arriva ora perché prima non c'erano invii
veri da raccontare (storia 0019) né ritorni del fornitore da contare (storia 0021).

## 2. Requisiti funzionali

1. **RF-1** — Il rapporto di una campagna mostra, in quest'ordine: destinatari selezionati dal segmento, esclusi,
   invii tentati, consegnati, rimbalzi permanenti, rimbalzi temporanei, segnalazioni di posta indesiderata,
   disiscrizioni arrivate da questa campagna.
2. **RF-2** — Gli **esclusi** sono sempre disaggregati per motivo: nessuna prova di consenso, consenso revocato,
   recapito soppresso, iscritto in quarantena, iscritto non confermato, recapito non valido. La somma degli
   esclusi e degli invii tentati deve tornare uguale ai destinatari selezionati; se non torna, il rapporto lo
   segnala invece di nasconderlo.
3. **RF-3** — Aperture e clic compaiono **solo** per le campagne che li hanno misurati (storia 0029); per le altre
   la voce dice «non misurate» con il motivo. Mai il valore zero.
4. **RF-4** — Il tasso di segnalazione di posta indesiderata della campagna è mostrato accanto alla soglia dello
   0,3 % richiesta dai grandi fornitori di posta, con l'indicazione di cosa succede se la si supera (blocco
   automatico, storia 0021).
5. **RF-5** — Il rapporto è consultabile **mentre** la campagna è in corso: i numeri parziali sono marcati come
   tali e si aggiornano, senza che l'utente debba ricaricare a mano.
6. **RF-6** — Il rapporto è **aggregato**. Gli unici elenchi nominativi disponibili sono quelli che il cliente
   deve poter gestire — rimbalzi permanenti e disiscrizioni — perché servono a tenere pulita la lista. Chi ha
   aperto e chi ha cliccato **non** è mai un elenco: solo un conteggio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura del rapporto e ogni aggregazione filtrano per `tenant_id`
  preso dal token verificato; un `tenant_id` nel corpo o nei parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/campaigns/v1/campaigns/{id}/report` e
  `GET /api/campaigns/v1/campaigns/{id}/report/exclusions`; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit; paginazione a pagina e dimensione con totale sugli elenchi nominativi.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova di dominio: il rapporto si calcola da `delivery`,
  `delivery_event` e dal motivo di esclusione registrato al momento della generazione degli invii (storia 0019). Si
  aggiunge una tabella di riepilogo `campaign_report_snapshot` sullo schema `app_campaigns` — `tenant_id`, chiave
  primaria UUID versione 7, colonne di controllo, cancellazione logica — riempita alla chiusura della campagna,
  così che il rapporto resti leggibile anche dopo la scadenza degli eventi di comportamento (storia 0029, RF-5).
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Rapporto» dentro la scheda della campagna del modulo `campaigns`,
  con il blocco degli indicatori, il riquadro degli esclusi e i due elenchi gestibili; dati letti con il client
  generato dalla definizione delle interfacce; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresi i **nomi dei motivi di esclusione**, che
  sono la parte che il cliente legge davvero — passano dallo spazio-nomi `campaigns` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Leggere un rapporto non consuma la metrica `messages_sent` (natura `flow`).
  Con abbonamento in `past_due` il rapporto resta accessibile; con `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Alimenta lo strumento di lettura `statistiche_campagna`
  (storia 0034) con **lo stesso calcolo**, non con una seconda interrogazione scritta a parte: due calcoli che
  divergono sono un difetto che nessuno scopre. Livello conversazionale non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona: il rapporto legge dati già
  dichiarati nel manifesto (`delivery.*`, `delivery_event.*`). La tabella di riepilogo contiene solo conteggi e va
  comunque aggiunta a `exportData` e `purgeData`, perché è legata all'account. La scelta di RF-6 — nessun elenco di
  chi ha aperto — è una misura di minimizzazione e va scritta come tale nella descrizione del manifesto.
- **RT-9 — Registrazione eventi (§14).** «Rapporto consultato» e «elenco dei rimbalzi esportato» registrati con
  `tenant_id`, `app_id`, `user_id`, identificativo della campagna e identificativo di correlazione; **mai** i
  recapiti contenuti negli elenchi.

## 4. Criteri di accettazione

**CA-1 — I conti tornano**
- **Dato** una campagna verso un segmento di 1.200 iscritti, di cui 260 non inviabili per motivi diversi
- **Quando** si apre il rapporto
- **Allora** i destinatari selezionati sono 1.200, gli esclusi 260 disaggregati per motivo, gli invii tentati 940,
  e la somma torna

**CA-2 — Gli esclusi hanno un motivo leggibile**
- **Dato** il riquadro degli esclusi
- **Quando** lo si apre
- **Allora** ogni voce porta il motivo in parole («nessuna prova di consenso», «recapito soppresso») e il numero,
  senza codici tecnici

**CA-3 — Aperture non misurate**
- **Dato** una campagna inviata con la misurazione spenta
- **Quando** si legge il rapporto
- **Allora** alla voce aperture compare «non misurate» con il motivo, e non `0`

**CA-4 — Parziali marcati durante l'invio**
- **Dato** una campagna in corso
- **Quando** si apre il rapporto
- **Allora** i numeri sono presentati come parziali e si aggiornano da soli, e la percentuale di segnalazione non
  è mostrata finché il campione è troppo piccolo per significare qualcosa

**CA-5 — Nessun elenco di chi ha aperto**
- **Dato** una campagna con la misurazione accesa
- **Quando** si cerca l'elenco dei destinatari che hanno aperto, sia nell'interfaccia sia sulle rotte
- **Allora** non esiste: sono disponibili solo il conteggio e i due elenchi gestibili (rimbalzi permanenti,
  disiscrizioni)

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con campagne proprie
- **Quando** un utente di `A` chiede il rapporto di una campagna di `B`
- **Allora** riceve `404`, identico all'esito di un identificativo inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla quadratura selezionati = esclusi + tentati e sul calcolo del tasso di segnalazione,
      e di **integrazione** sulle due rotte, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul rapporto e sugli elenchi;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) legge il rapporto dopo l'invio
      e verifica che il destinatario disiscritto compaia fra gli esclusi; voce aggiunta al registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, motivi di esclusione compresi;
- [ ] **manifesto dei dati** aggiornato per la tabella di riepilogo e per la scelta di minimizzazione di RF-6;
- [ ] **registro delle decisioni** compilato, con annotato perché il rapporto è aggregato e perché gli esclusi
      sono in evidenza invece che in una nota;
- [ ] contratto degli **strumenti conversazionali**: `statistiche_campagna` alimentato dallo stesso calcolo;
- [ ] controllo automatico di **accessibilità** verde sulla sezione «Rapporto»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0019` | Senza invii veri non c'è nulla da raccontare, e il motivo di esclusione si registra lì |
| Storia `0021` | Rimbalzi e segnalazioni arrivano dai ritorni del fornitore |
| Storia `0029` | Aperture e clic esistono solo se misurati, e il rapporto deve saperlo dire |

## 7. Fuori ambito

- il confronto fra due varianti di oggetto: è la storia 0031;
- il quadro d'insieme della lista, che non è di una campagna ma dell'account: è la storia 0032;
- l'uscita dei numeri in un file: è la storia 0033;
- il rapporto per periodo o per canale aggregato su più campagne: rimandato, perché richiede scelte di
  aggregazione che con tre campagne all'anno non servono a nessuno.

## 8. Punti aperti

- **Soglia sotto la quale non mostrare la percentuale di segnalazione.** Su cento destinatari una segnalazione fa
  l'1 %, che è un numero vero e un'informazione fuorviante. La proposta è non mostrare la percentuale sotto un
  campione minimo e mostrare il conteggio assoluto: il valore del campione minimo lo chiude lo sviluppatore.
