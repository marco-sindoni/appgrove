# 0029 — Strumenti di scrittura con conferma

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0016`, `0025`, `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole chiudere il conto della cliente parlando, mentre riordina la postazione
> voglio che l'assistente prepari il conto e me lo faccia vedere prima di chiuderlo davvero
> così da guadagnare i dieci secondi senza rischiare di scaricare il magazzino sbagliato o di scalare una seduta a
> chi non doveva.

**Contesto.** Gli strumenti di lettura (storia `0028`) non cambiano nulla; questi sì. E in SalonGrove ciò che
cambiano è tutto quello che conta: **denaro, magazzino e quanto spetta a una persona**. La regola di sicurezza del
catalogo non ammette sfumature ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12): gli strumenti di
scrittura con effetti irreversibili producono una **bozza** e richiedono una **conferma umana esplicita** —
l'intelligenza artificiale prepara, la persona approva. La chiusura del conto (storia `0019`) è l'esempio da
manuale: fa scattare tre effetti insieme e non si annulla.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati i **cinque** strumenti di scrittura, con nome stabile, descrizione, schemi e
   marcatura *scrittura*: `apri_conto(prenotazione)`, `aggiungi_riga_conto(conto, voce, operatore)`,
   `chiudi_conto(conto, modo_incasso)`, `rettifica_giacenza(prodotto, deposito, quantita, motivo)`,
   `chiudi_prospetto_provvigioni(periodo)`.
2. **RF-2** — **Ognuno dei cinque** produce una **bozza** con un identificativo e una scadenza breve, e non ha
   effetto finché non arriva una conferma umana esplicita che cita quell'identificativo. Nessuna eccezione «per
   comodità» sui due considerati leggeri: aprire un conto sbagliato è recuperabile, ma la regola che vale sempre è
   più facile da rispettare di una che vale a volte.
3. **RF-3** — La bozza **dice che cosa sta per succedere, con i numeri**: per `chiudi_conto`, i tre effetti
   («scarico 60 ml di tinta, scalo 1 seduta del pacchetto, maturano 12 € di provvigione») e il totale; per
   `chiudi_prospetto_provvigioni`, quante persone e quale importo complessivo sta per congelare; per
   `rettifica_giacenza`, la giacenza prima e dopo. Una conferma che non dice che cosa conferma non è una conferma.
4. **RF-4** — La conferma è **idempotente**: confermare due volte la stessa bozza produce un solo effetto; una
   bozza scaduta o già confermata risponde con un errore chiaro e non fa nulla.
5. **RF-5** — Sono dichiarati **divieti espliciti**, cioè operazioni che nessuno strumento compie in nessun caso:
   inviare messaggi ai clienti, caricare o cancellare fotografie, scrivere o modificare la nota tecnica libera,
   modificare le regole di provvigione, riaprire un conto o un prospetto chiuso, esportare o cancellare dati
   personali.
6. **RF-6** — Le invocazioni di scrittura, la creazione della bozza e la conferma sono registrate come eventi
   distinti, con l'indicazione che l'operazione è arrivata dal livello conversazionale.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Cinque strumenti dichiarati **scrittura**, tutti con bozza e
  conferma umana; `chiudi_conto` e `chiudi_prospetto_provvigioni` sono marcati **irreversibili**, e per loro la
  conferma è obbligatoria per contratto, non per impostazione. Dipendenza dichiarata: casi d'uso 0061-0063
  (livello conversazionale, non implementato).
- **RT-2 — Isolamento fra account (§1).** Bozze e conferme filtrano per `tenant_id` preso dal token verificato; una
  bozza creata nel contesto di un account non è confermabile da un altro, e il tentativo viene registrato.
- **RT-3 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti dei conti, del
  magazzino e dei prospetti: **nessuna seconda implementazione** della chiusura, o la chat e la schermata si
  metteranno a comportarsi in modo diverso. Errori in `application/problem+json`.
- **RT-4 — Persistenza (§8).** Migrazione sullo schema dell'app: tabella `bozza_operazione` (strumento, parametri
  normalizzati, riepilogo degli effetti, stato, scadenza) con `tenant_id`, UUID versione 7, colonne di controllo e
  cancellazione logica; le bozze scadute si eliminano con una pulizia periodica.
- **RT-5 — Atomicità.** La conferma esegue l'operazione con la stessa transazione della schermata (storia `0019`):
  o succedono tutti e tre gli effetti, o nessuno.
- **RT-6 — Varchi e quota (§6, §7).** Le invocazioni attraversano i cinque varchi. La creazione di una bozza **non**
  consuma nulla; l'effetto vero rispetta gli stessi limiti della schermata. Con abbonamento `canceled`: `402`.
- **RT-7 — Dati personali (§10).** Il riepilogo della bozza contiene **il minimo indispensabile a decidere**:
  importi, quantità, servizi; il nome del cliente compare solo se necessario a distinguere il conto, mai i suoi
  recapiti, mai le note. Il manifesto dichiara la tabella delle bozze e la sua durata breve; la tabella entra in
  esportazione e cancellazione (storie `0014` e `0032`).
- **RT-8 — Registrazione eventi (§14).** `bozza creata`, `bozza confermata`, `bozza scaduta`, `conferma rifiutata`
  con `tenant_id`, `app_id`, `user_id`, correlazione, nome dello strumento e origine conversazionale — mai nomi di
  persone.
- **RT-9 — Prove (§11).** Prova che **nessuno** dei cinque produca effetti senza conferma; prova di idempotenza
  della conferma; prova che i divieti dell'RF-5 non siano aggirabili con nessuna combinazione di parametri.

## 4. Criteri di accettazione

**CA-1 — Niente succede senza il sì**
- **Dato** un conto pronto con un pacchetto attivo e dosi previste
- **Quando** si invoca `chiudi_conto(conto, modo_incasso: contanti)`
- **Allora** si ottiene una bozza con i tre effetti e il totale, il conto è ancora aperto, il magazzino non si è
  mosso e nessuna provvigione è maturata

**CA-2 — La conferma esegue**
- **Dato** la bozza precedente · **Quando** si conferma citandone l'identificativo
- **Allora** il conto risulta chiuso, la giacenza scende della quantità dichiarata, la seduta è scalata e la
  provvigione è maturata, con gli stessi valori mostrati nella bozza

**CA-3 — Confermare due volte non raddoppia**
- **Dato** la stessa bozza già confermata · **Quando** la si conferma di nuovo
- **Allora** l'esito è lo stesso della prima volta, nulla accade una seconda volta e nessun errore silenzioso viene
  nascosto

**CA-4 — La bozza scade**
- **Dato** una bozza creata oltre la scadenza dichiarata
- **Quando** si tenta la conferma
- **Allora** la risposta spiega che è scaduta e invita a ricreare l'operazione, e nulla cambia

**CA-5 — I divieti tengono**
- **Dato** l'insieme completo degli strumenti
- **Quando** si tenta di inviare un messaggio, caricare una fotografia, scrivere la nota tecnica, modificare una
  regola di provvigione, riaprire un conto chiuso o cancellare dati personali
- **Allora** non esiste alcuno strumento che lo faccia, e la prova negativa lo verifica

**CA-6 — Isolamento fra account**
- **Dato** una bozza creata nell'account `A` · **Quando** la si conferma nel contesto di `B`
- **Allora** la conferma è respinta come per una bozza inesistente e il tentativo è registrato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (area `backend`; l'intera suite prima del commit, perché la storia tocca
      la chiusura del conto);
- [ ] prove di **unità** sul riepilogo degli effetti e sulla scadenza; di **integrazione** su bozza e conferma, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su creazione e conferma;
- [ ] prova **negativa** sui divieti dichiarati;
- [ ] **prova end-to-end**: *nessun impatto* sulla superficie utente — il livello conversazionale non esiste ancora;
      risposta scritta nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in `decisions.json`;
- [ ] **traduzioni**: i testi dei riepiloghi di bozza rivolti a una persona sono in `en, it, fr, es, de`;
- [ ] **manifesto dei dati** aggiornato con la tabella delle bozze e la sua durata;
- [ ] **registro delle decisioni**: bozza obbligatoria per tutti e cinque, durata della scadenza, idempotenza,
      elenco dei divieti;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | `chiudi_conto` riusa la chiusura atomica e i suoi tre effetti |
| storia `0016` | `rettifica_giacenza` riusa il movimento di rettifica con motivo |
| storia `0025` | `chiudi_prospetto_provvigioni` riusa la chiusura del prospetto |
| storia `0028` | condivide il contratto e la sua versione |
| casi d'uso di piattaforma 0061-0063 (non implementati) | server conversazionale e consenso delegato |

## 7. Fuori ambito

- l'**invio** di messaggi ai clienti: escluso per scelta (storia `0027`), e resta escluso;
- la **prenotazione** da chat: è di BookGrove (`crea_prenotazione`), non si riscrive qui;
- l'**esportazione e la cancellazione** dei dati personali: non si delegano a un assistente (storia `0032`);
- l'interfaccia della chat: è di piattaforma.

## 8. Punti aperti

**Quanto dura una bozza.** Proposta: dieci minuti. Troppo corta rende la conferma irritante quando il salone è
pieno; troppo lunga fa confermare qualcosa che non si ricorda più. Da verificare sul campo e registrare.

**Se la conferma debba avvenire nella chat o nell'applicazione.** Confermare nella chat è comodo; confermare
nell'applicazione è più sicuro, perché la persona vede lo stato vero. La proposta è: nella chat per i tre
strumenti recuperabili, **nell'applicazione** per i due irreversibili. È una decisione di piattaforma quanto di
prodotto (casi d'uso 0061-0062), e va portata là.
