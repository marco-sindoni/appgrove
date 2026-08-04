# 0009 — Il rapporto sorvegliato

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 02 — Arrivo dei segnali dalle altre app
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della relazione con i clienti in uno studio di consulenza
> voglio vedere i fatti che arrivano dalle varie app raccolti sotto il **cliente** a cui si riferiscono, con il suo
> nome e non con un codice
> così da poter guardare un elenco di persone e aziende invece che un elenco di eventi.

**Contesto.** Fin qui i segnali sono righe sparse: hanno un riferimento opaco, non un soggetto. Questa storia crea
il **rapporto sorvegliato**, che è l'entità attorno a cui gira tutto il resto dell'app — il punteggio si calcola su
di lui, l'intervento si fa verso di lui, l'esito si misura su di lui. Tre cose la rendono delicata. La prima:
l'**etichetta leggibile** è l'unico campo anagrafico dell'applicazione, arriva su un evento separato dai segnali
(`0006`) e va trattata come il dato personale che è. La seconda: è **qui che si consuma la quota**
`rapporti_sorvegliati`, e va deciso bene che cosa succede al rapporto che non entra — perché buttare via i suoi
segnali sarebbe perdere fatti che non tornano. La terza: **archiviare non è cancellare**, altrimenti liberare quota
distruggerebbe proprio lo storico su cui l'epica 05 misura l'efficacia.

## 2. Requisiti funzionali

1. **RF-1** — I segnali si aggregano automaticamente su un `rapporto` identificato, dentro l'account, dalla coppia
   `(app_origine, riferimento_opaco)`. Un segnale che arriva per una coppia sconosciuta crea il rapporto; i
   successivi vi si attaccano.
2. **RF-2** — L'**etichetta leggibile** del rapporto arriva su un **evento separato**, con un consumatore distinto
   da quello dei segnali. È l'unico campo anagrafico dell'app. Finché non arriva, il rapporto esiste e si mostra con
   il proprio riferimento opaco, dichiarato come tale.
3. **RF-3** — Un rapporto sta in uno di tre stati di sorveglianza: `sorvegliato` (i segnali entrano, il punteggio si
   calcola, gli avvisi scattano), `archiviato` (i segnali continuano a entrare e lo storico resta, ma non si calcola
   né si avvisa), `escluso` (deciso da una persona: non si sorveglia e non si sorveglierà, con motivo e autore).
4. **RF-4** — La metrica `rapporti_sorvegliati` conta **solo** i rapporti nello stato `sorvegliato`. Archiviare o
   escludere libera quota; riportare in sorveglianza la consuma.
5. **RF-5** — A tetto raggiunto un rapporto nuovo **non entra in sorveglianza** e l'utente riceve `429` con il
   rimedio; **i suoi segnali non si perdono**: il rapporto viene creato in stato `archiviato` e i segnali continuano
   a essere scritti, così che portandolo in sorveglianza più tardi lo storico sia già lì.
6. **RF-6** — Archiviare **non cancella** lo storico: segnali, punteggi passati e interventi restano consultabili, e
   il rapporto si può riportare in sorveglianza se c'è quota.
7. **RF-7** — La sezione **Rapporti** elenca i rapporti dell'account con etichetta, stato, fonte o fonti che li
   alimentano e momento dell'ultimo segnale, con ricerca per etichetta e filtro per stato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `rapporto` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Il consumatore
  dell'evento di etichetta **copia** il `tenant_id` dall'evento, come il consumatore dei segnali (`0007`), e non
  condivide codice con il percorso di lettura.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/fidelizzazione/v1/rapporti` con paginazione a
  pagina/dimensione e totale, `GET /api/fidelizzazione/v1/rapporti/{id}`,
  `POST /api/fidelizzazione/v1/rapporti/{id}/sorveglianza` e `.../archiviazione`; oggetti di trasferimento al bordo,
  corpo validato, errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_fidelizzazione` che aggiunge a `rapporto` lo stato di
  sorveglianza, il motivo e l'autore dell'esclusione, e popola `etichetta`; `tenant_id`, chiave primaria UUID
  versione 7, colonne di controllo e cancellazione logica come da `0002`; nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Rapporti** del modulo `fidelizzazione`: elenco, ricerca, filtro per
  stato, azioni di archiviazione e di rientro in sorveglianza. Dati letti con il client generato; solo token del
  sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi degli stati, messaggio di rifiuto per quota, avvertenza «etichetta non ancora
  arrivata» e testi della sezione passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di portare un rapporto in `sorvegliato` il servizio prenota una unità
  della metrica `rapporti_sorvegliati` (natura `stock`); a quota esaurita risponde `429` con l'indicazione del
  rimedio, e il rapporto resta `archiviato`. Con abbonamento non attivo risponde `402`. La storia **non fissa
  prezzi**: consuma il tetto pubblicato.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `stato_rapporto(rapporto) → scheda
  minimizzata` marcato **lettura**; `escludi_rapporto(rapporto, motivo) → bozza` marcato **scrittura** con conferma
  umana. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063); l'esposizione vera è delle storie `0028` e `0029`.
- **RT-8 — Dati personali (§10).** È la storia che rende **vero** il trattamento: voci nuove nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml` in **italiano e inglese** per `rapporto.etichetta` (di norma un
  nome o una ragione sociale, interessato = cliente del nostro cliente) e per lo stato di sorveglianza; campo
  `etichetta` annotato `@PersonalData`; tabelle `rapporto` e `segnale` presenti in `exportData` **e** `purgeData` di
  `FidelizzazioneDataContract`. Conservazione proposta: finché il rapporto è sorvegliato, con cancellazione fisica
  entro trenta giorni dall'archiviazione definitiva o dalla revoca della fonte. Nessun recapito, nessun indirizzo,
  nessun identificativo fiscale: la via A del §4.3 della [descrizione](../application-description.md) lo esclude.
- **RT-9 — Registrazione eventi (§14).** «rapporto creato», «etichetta ricevuta», «rapporto messo in sorveglianza»,
  «messa in sorveglianza respinta per quota», «rapporto archiviato», «rapporto escluso» con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, **senza l'etichetta**: si registra l'identificativo del rapporto, non
  il nome.
- **RT-10 — Prove (§11).** Unità sull'aggregazione per coppia e sul conteggio della quota per stato; integrazione
  sulle rotte e sul consumatore dell'evento di etichetta, con database effimero e migrazioni vere; isolamento fra
  due account; controllo automatico di accessibilità sulla sezione Rapporti.

## 4. Criteri di accettazione

**CA-1 — I segnali si raccolgono sotto un soggetto**
- **Dato** un account con la fatturazione collegata
- **Quando** arrivano tre segnali con lo stesso `(app_origine, riferimento_opaco)`
- **Allora** esiste **un** rapporto con tre segnali attaccati, e non tre rapporti

**CA-2 — L'etichetta arriva a parte**
- **Dato** un rapporto creato da un segnale, senza etichetta
- **Quando** arriva l'evento separato che porta l'etichetta «Studio Bianchi»
- **Allora** l'elenco mostra «Studio Bianchi» al posto del riferimento opaco, e nessun segnale ha mai contenuto
  quel nome

**CA-3 — Quota esaurita: il rapporto non entra, i segnali sì**
- **Dato** un account sul piano `cura` con 250 rapporti sorvegliati
- **Quando** arriva il primo segnale di un cliente mai visto
- **Allora** l'utente riceve `429` con il rimedio, il rapporto è creato in stato `archiviato`, i suoi segnali
  vengono comunque scritti, e nulla entra in sorveglianza

**CA-4 — Archiviare non cancella**
- **Dato** un rapporto sorvegliato con due anni di segnali e tre interventi passati
- **Quando** lo si archivia
- **Allora** la quota si libera, lo storico resta consultabile per intero, e riportandolo in sorveglianza il
  punteggio si ricalcola sui fatti già presenti

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con rapporti che portano la stessa etichetta di fantasia
- **Quando** un utente di `A` chiede l'elenco dei rapporti
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account di `B` nella richiesta

**CA-6 — Nessun nome nei registri**
- **Dato** le operazioni di creazione, etichettatura e archiviazione di un rapporto
- **Quando** si ispezionano i registri applicativi
- **Allora** compaiono `tenant_id`, `app_id`, `user_id`, correlazione e identificativo del rapporto, e **mai**
  l'etichetta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sull'aggregazione e sul conteggio della quota per stato, e di **integrazione** sulle rotte
      e sul consumatore dell'etichetta, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte e sul consumatore dell'evento di etichetta;
- [ ] **prova end-to-end**: *rimando* alla storia `0030`, che dovrà coprire «segnale → rapporto con etichetta →
      elenco visibile → archiviazione che libera quota»; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le voci sull'etichetta e sullo stato di
      sorveglianza, campo `etichetta` annotato `@PersonalData`, tabelle `rapporto` e `segnale` in `exportData` e
      `purgeData`;
- [ ] **registro delle decisioni** compilato: perché il rapporto oltre il tetto nasce archiviato invece di essere
      rifiutato, perché archiviare non cancella, e la conservazione proposta di trenta giorni;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `stato_rapporto` e `escludi_rapporto`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | i segnali devono già arrivare e portare il riferimento opaco su cui si aggrega |
| storia `0008` | senza fonti collegate non arriva nulla da aggregare, e l'elenco dei rapporti sarebbe vuoto |
| classificazione dei dati personali (§6 della descrizione, punto aperto n. 4) | qui l'etichetta si popola davvero: base giuridica e informativa al cliente finale vanno chiuse **prima** |
| epica di piattaforma non implementata, UC 0061-0063 | `stato_rapporto` ed `escludi_rapporto` si dichiarano qui e si espongono nelle storie `0028` e `0029` |

## 7. Fuori ambito

- il punteggio e la sua fascia: epica 03 — qui il rapporto esiste, ma non ha ancora un giudizio addosso;
- l'esclusione come forma di **contestazione** del punteggio, con motivo e traccia: storia `0015`, che riusa lo
  stato `escluso` introdotto qui;
- l'importazione di un elenco di clienti da file: storia `0010`;
- l'esportazione e la cancellazione complete su tutte le tabelle: storia `0032`, che chiude il contratto dati.

## 8. Punti aperti

- **Che cosa succede se due fonti diverse descrivono lo stesso cliente?** Il rapporto è identificato da
  `(app_origine, riferimento_opaco)`, quindi lo stesso cliente visto da SubGrove e da `fatture` genera **due**
  rapporti, che consumano **due** unità di quota. È il comportamento proposto perché è l'unico onesto senza
  un'anagrafica autorevole condivisa, e la descrizione dice chiaramente che RenewGrove tiene un riferimento, non una
  copia (§10). Resta sgradevole e va sorvegliato. Chiude: lo sviluppatore, insieme alla decisione sull'anagrafica
  condivisa di 04 LeadGrove.
- **Il rapporto `escluso` consuma quota?** La proposta è no (storia `0004`, punto aperto). Va confermata insieme al
  listino, perché cambia il significato del tetto pubblicato.
