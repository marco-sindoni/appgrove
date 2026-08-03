# 0010 — Sottoscrizione di un abbonamento

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 02 — Piani e abbonati
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta alla reception con una persona davanti al banco
> voglio abbonarla a un piano in meno di un minuto e vedere subito quando e quanto pagherà
> così da poterglielo dire ad alta voce mentre firma, senza fare conti a mano.

**Contesto.** È il momento in cui le due entità dell'epica si incontrano: un abbonato, un piano, una data di
decorrenza, e nasce un rapporto che da lì in poi vive da solo. La cosa che questa storia deve fare bene non è
salvare una riga — è **calcolare e mostrare** cosa succederà: quando finisce il primo periodo, quanto è dovuto,
quando si rinnoverà, entro quando si può disdire. Sono i numeri che l'addetta ripete a voce e che l'abbonato
ricorderà; se l'app li sbaglia, se ne accorgono tutti e subito. È anche il punto in cui si consuma la quota di
appgrove, e dove il rifiuto per tetto raggiunto deve arrivare **prima** che qualcuno abbia firmato qualcosa.

## 2. Requisiti funzionali

1. **RF-1** — Si sottoscrive un abbonamento scegliendo abbonato, piano e data di decorrenza; l'abbonamento si
   aggancia alla **versione di prezzo viva** in quel momento e ci resta.
2. **RF-2** — Alla conferma l'app mostra, prima di salvare: fine del primo periodo, importo dovuto, data del
   primo rinnovo, ultimo giorno utile per disdire senza rinnovo (calcolato dal preavviso del piano).
3. **RF-3** — Se il piano prevede giorni di prova, l'abbonamento nasce in stato `in_prova` e la prima scadenza è
   collocata alla fine della prova; altrimenti nasce `attivo` con scadenza immediata.
4. **RF-4** — La sottoscrizione **consuma una unità** della metrica `abbonamenti_attivi`: a tetto raggiunto
   risponde `429` e nulla viene creato.
5. **RF-5** — Un abbonato può avere più abbonamenti contemporanei su piani diversi; l'app avvisa (senza bloccare)
   se se ne sta creando un secondo sullo **stesso** piano ancora vivo.
6. **RF-6** — L'abbonamento porta, per tutta la vita, un riferimento a chi lo ha sottoscritto e quando.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Abbonato, piano e prezzo devono appartenere allo **stesso** account del
  token verificato: un riferimento incrociato fra account è un rifiuto, non un caso limite.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/abbonati/v1/abbonamenti` e
  `POST /api/abbonati/v1/abbonamenti/anteprima` (che calcola senza salvare); errori in `problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V5__abbonamento.sql` sullo schema `app_abbonati`: tabella
  `abbonamento` con `tenant_id`, chiave UUID versione 7, colonne di controllo, cancellazione logica, stato,
  inizio e fine del periodo in corso, riferimenti logici ad abbonato, piano e versione di prezzo.
- **RT-4 — Modulo frontend (§3, §5).** Modulo di sottoscrizione con **anteprima delle date e degli importi**
  prima della conferma; l'avviso di quota compare **prima** del modulo, non dopo il salvataggio; solo token del
  sistema di design.
- **RT-5 — Cinque lingue (§4).** Etichette, anteprima, avvisi e messaggi di rifiuto in `en, it, fr, es, de`.
  Attenzione ai formati di data, che cambiano con la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il servizio prenota una unità di `abbonamenti_attivi` (natura `stock`)
  **prima** di creare; a quota esaurita `429` con il rimedio; con abbonamento di piattaforma non attivo `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `crea_abbonamento(abbonato, piano, decorrenza) → bozza`, marcato **scrittura**: produce una bozza con le date
  calcolate e richiede conferma umana. Contratto dentro il servizio; il server conversazionale è di piattaforma e
  non ancora implementato (use case 0061-0063).
- **RT-8 — Dati personali (§10).** L'abbonamento è riferito a una persona: la tabella va aggiunta al manifesto,
  a `exportData` e a `purgeData` (impianto della storia `0009`).
- **RT-9 — Registrazione eventi (§14).** `abbonamento sottoscritto`, `sottoscrizione respinta per quota`, con
  `tenant_id`, `app_id`, `user_id` e correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Sottoscrizione con anteprima corretta**
- **Dato** un piano mensile a 39 €, decorrenza 15 marzo, preavviso 30 giorni
- **Quando** l'addetta apre l'anteprima
- **Allora** legge: primo periodo fino al 14 aprile, dovuto 39 €, primo rinnovo il 15 aprile, ultimo giorno utile
  per disdire il 16 marzo

**CA-2 — Piano con prova**
- **Dato** un piano con 14 giorni di prova · **Quando** si sottoscrive
- **Allora** l'abbonamento nasce `in_prova`, la prima scadenza è al quindicesimo giorno e l'anteprima lo dice

**CA-3 — Quota esaurita**
- **Dato** un account al tetto di `abbonamenti_attivi` · **Quando** si tenta una sottoscrizione
- **Allora** riceve `429` con il rimedio, **prima** di qualunque salvataggio, e nulla viene creato

**CA-4 — Riferimento incrociato fra account**
- **Dato** un abbonato dell'account `A` e un piano dell'account `B`
- **Quando** un utente di `A` prova a metterli insieme
- **Allora** la richiesta è rifiutata e nulla viene creato

**CA-5 — Doppione sullo stesso piano**
- **Dato** un abbonato già abbonato al piano «Annuale» · **Quando** se ne crea un secondo sullo stesso piano
- **Allora** l'app avvisa ma consente, e l'avviso resta nella cronologia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul calcolo delle date (fine periodo, rinnovo, ultimo giorno utile) e di **integrazione**
      sulla risorsa;
- [ ] prova di **isolamento fra account**, compreso il riferimento incrociato;
- [ ] **prova end-to-end**: *coprire ora* — la sottoscrizione è il cuore del percorso `[J-ABBONATI]`; il passo
      entra nella storia `0033` e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, formati di data compresi;
- [ ] **manifesto dei dati** aggiornato con la tabella `abbonamento`, in italiano e inglese, e tabella presente in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato: aggancio alla versione di prezzo e calcolo dell'ultimo giorno utile;
- [ ] contratto dello strumento `crea_abbonamento` dichiarato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | serve un piano con le sue condizioni |
| storia `0007` | serve una versione di prezzo viva a cui agganciarsi |
| storia `0008` | serve l'abbonato |
| storia `0004` | serve il varco di quota, altrimenti si sottoscrive oltre il tetto |

## 7. Fuori ambito

- la macchina a stati completa e i passaggi successivi: storia `0011`;
- la generazione automatica delle scadenze ai rinnovi: storia `0012` — qui si crea solo la prima;
- l'autorizzazione all'addebito: storia `0017`;
- la sottoscrizione fatta dall'abbonato stesso da una pagina pubblica: **non** è in questo indice (vedi punto
  aperto).

## 8. Punti aperti

**Sottoscrizione self-service dall'esterno.** Un'iscrizione che parte dall'abbonato, senza passare dalla
reception, è una funzione desiderabile e una superficie pubblica in più — con tutto quello che comporta
(difese, consenso, e soprattutto un pagamento che noi **non** possiamo incassare, §5.2 della descrizione). Il
portale della storia `0023` serve un abbonato che **esiste già**; farne un punto d'ingresso per chi non esiste
ancora è un'altra cosa. **Proposta**: fuori dal primo giro, e da valutare solo dopo aver visto come si comporta
il portale. Chiude: lo sviluppatore, con la direzione di prodotto.
