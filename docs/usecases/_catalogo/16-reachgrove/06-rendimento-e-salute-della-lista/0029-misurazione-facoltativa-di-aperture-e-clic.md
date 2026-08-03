# 0029 — Misurazione facoltativa di aperture e clic

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 06 — Rendimento e salute della lista
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0019` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che manda comunicazioni commerciali e risponde di come tratta i dati delle persone
> voglio decidere campagna per campagna se misurare le aperture e i clic, con la misurazione spenta finché non la
> accendo io
> così da non accumulare dati sul comportamento delle persone senza essermi accorto di averlo fatto.

**Contesto.** Misurare le aperture significa inserire nel messaggio un'immagine invisibile che il programma di
posta scarica; misurare i clic significa riscrivere ogni collegamento in modo che passi da noi. Sono due
**trattamenti ulteriori** rispetto all'invio, e l'analisi in rete non ha trovato una fonte che chiuda la questione
della loro liceità per la posta elettronica commerciale
([application-description.md](../application-description.md) §2.7). Finché quella domanda è aperta la scelta
prudente è una sola: la misurazione esiste, è **spenta in partenza** e si accende con un atto consapevole. Va
fatta adesso, prima del rapporto di campagna (storia 0030), perché un rapporto che dà per scontate le aperture
costringerebbe ad accendere la misurazione per tutti.

## 2. Requisiti funzionali

1. **RF-1** — Ogni campagna ha due interruttori distinti, «misura le aperture» e «misura i clic», entrambi
   **spenti alla creazione**. Duplicare una campagna che li aveva accesi produce una campagna con gli interruttori
   **spenti**: la scelta non si eredita.
2. **RF-2** — L'account ha un interruttore generale «non misurare mai il comportamento»: quando è attivo gli
   interruttori di campagna sono visibili ma non attivabili, con la spiegazione del perché.
3. **RF-3** — Accendere un interruttore richiede una conferma esplicita che dice, in parole comprensibili, cosa
   comporta (immagine invisibile nel messaggio, collegamenti che passano da noi), che i dati raccolti riguardano
   il comportamento di una persona e che il cliente deve dichiararlo **nella propria informativa**, perché il
   titolare del trattamento verso l'iscritto è lui.
4. **RF-4** — A misurazione spenta il messaggio esce **senza** immagine invisibile e **senza** collegamenti
   riscritti: i destinatari ricevono gli indirizzi originali. È verificabile ispezionando il messaggio prodotto.
5. **RF-5** — Gli eventi di apertura e di clic si conservano per un periodo dichiarato (proposta: 12 mesi) e poi
   vengono cancellati da una lavorazione periodica; i conteggi aggregati già calcolati restano.
6. **RF-6** — Dove aperture e clic non sono stati misurati, l'interfaccia scrive «non misurate» con il motivo, e
   **mai** il valore zero: uno zero è un'informazione falsa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli interruttori, gli eventi di apertura e di clic e la lavorazione di
  cancellazione filtrano per `tenant_id` preso dal token verificato; l'indirizzo pubblico che riceve l'apertura o
  il clic risolve l'account **dal riferimento opaco contenuto nell'indirizzo**, mai da un parametro leggibile.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PATCH /api/campaigns/v1/campaigns/{id}/measurement` e
  `PATCH /api/campaigns/v1/settings/measurement`; corpo validato; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit. Le due rotte pubbliche di raccolta (immagine invisibile e reindirizzamento
  del clic) non richiedono token, rispondono sempre allo stesso modo e non rivelano se il riferimento esiste.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__add_measurement_flags.sql` sullo schema `app_campaigns`: colonne
  `track_opens` e `track_clicks` su `campaign` (valore predefinito falso), interruttore di account nella tabella di
  impostazioni, tipi `apertura` e `clic` su `delivery_event` con indice per campagna. Chiavi primarie UUID versione
  7, colonne di controllo e cancellazione logica come le altre tabelle.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Misurazione» dentro la scheda della campagna del modulo
  `campaigns`, con i due interruttori, il testo di spiegazione e la finestra di conferma; solo token del sistema di
  design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `campaigns` e sono presenti in
  `en, it, fr, es, de`, compreso il testo di conferma, che è la parte che nessuno deve leggere tradotta a metà.
- **RT-6 — Varchi e quota (§6, §7).** Cambiare gli interruttori non consuma la metrica `messages_sent` (natura
  `flow`); richiede ruolo `owner` o `admin`, altrimenti `403`. Con abbonamento non attivo la rotta risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. `statistiche_campagna` (storia 0034)
  restituisce aperture e clic **solo** per le campagne che li hanno misurati, e per le altre restituisce il valore
  «non misurate», mai zero. Livello conversazionale non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** È la storia che rende vere le voci `delivery_event.open` e `delivery_event.click`
  del manifesto `docs/compliance/manifests/campaigns.yaml`, in italiano e inglese, con finalità «misurare il
  rendimento», conservazione proposta 12 mesi e base giuridica **dichiarata come da decidere**: è il punto aperto
  §11.6a della descrizione, e va scritto come tale invece di essere riempito a intuito. Campi annotati
  `@PersonalData`; la tabella è già in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Misurazione accesa» e «misurazione spenta» registrate con `tenant_id`,
  `app_id`, `user_id`, identificativo della campagna e identificativo di correlazione. Le aperture e i clic
  **non** si registrano nei registri applicativi: sono dati, non diagnostica.

## 4. Criteri di accettazione

**CA-1 — Spenta in partenza, anche duplicando**
- **Dato** una campagna con entrambe le misurazioni accese
- **Quando** la si duplica
- **Allora** la copia nasce con entrambe **spente**

**CA-2 — Il messaggio non misurato è pulito**
- **Dato** una campagna con le misurazioni spente
- **Quando** si genera il messaggio da inviare
- **Allora** non contiene alcuna immagine invisibile e i collegamenti sono quelli scritti dall'utente, non riscritti

**CA-3 — L'interruttore di account vince**
- **Dato** un account con «non misurare mai» attivo
- **Quando** un utente prova ad accendere la misurazione di una campagna
- **Allora** l'operazione è rifiutata con la spiegazione, e nulla cambia

**CA-4 — «Non misurate», non zero**
- **Dato** una campagna conclusa senza misurazione
- **Quando** si apre il suo rapporto
- **Allora** alla voce aperture compare «non misurate» con il motivo, e non il numero `0`

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con campagne proprie
- **Quando** un utente di `A` prova a cambiare la misurazione di una campagna di `B`, anche forzando l'account nel
  corpo della richiesta
- **Allora** riceve `404` e la campagna di `B` resta invariata

**CA-6 — La conservazione scade**
- **Dato** eventi di apertura più vecchi del periodo dichiarato
- **Quando** la lavorazione periodica gira
- **Allora** quegli eventi non esistono più, mentre i conteggi aggregati della campagna restano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione del messaggio con e senza misurazione e di **integrazione** sulle rotte,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sugli interruttori e sulle rotte pubbliche di raccolta;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) verifica che il messaggio
      inviato con le impostazioni predefinite non contenga né immagine invisibile né collegamenti riscritti; voce
      aggiunta al registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, testo di conferma compreso;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `delivery_event.open` e `.click`, con la base
      giuridica scritta come «da decidere» e il rimando al punto aperto;
- [ ] **registro delle decisioni** compilato, con annotato perché la misurazione è spenta in partenza e perché la
      scelta non si eredita duplicando;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; comportamento di `statistiche_campagna`
      documentato;
- [ ] controllo automatico di **accessibilità** verde sulla sezione «Misurazione»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | La riscrittura dei collegamenti opera sul messaggio composto |
| Storia `0019` | Gli eventi di apertura e clic si attaccano a un invio esistente |
| Revisione legale sulla liceità della misurazione ([application-description.md](../application-description.md) §2.7, §11.6a) | La base giuridica della voce di manifesto non si può scrivere senza |

## 7. Fuori ambito

- il rapporto che mostra questi numeri: è la storia 0030;
- la prova a due varianti, che li usa per confrontare due oggetti: è la storia 0031;
- il consenso dell'iscritto alla misurazione come atto separato dall'iscrizione: non è previsto in questa storia,
  perché prima serve sapere se è necessario (punto aperto).

## 8. Punti aperti

- **Base giuridica della misurazione** — consenso proprio oppure legittimo interesse: non deciso, è la fermata di
  escalation §11.6a. Chiude lo sviluppatore con la revisione legale. Finché non è chiusa, la misurazione resta
  spenta in partenza e il manifesto lo dichiara.
- **Durata di conservazione degli eventi di comportamento** — proposta 12 mesi, senza un fondamento di legge che
  l'analisi abbia trovato. Chiude lo sviluppatore.
