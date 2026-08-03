# 0029 — Strumenti di scrittura con conferma

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vorrebbe dire «rifammi la fattura del mese scorso per Cliente Alfa»
> voglio che l'assistente me la prepari e me la faccia vedere prima di fare qualsiasi cosa
> così da risparmiare i cinque minuti della compilazione senza correre il rischio che parta una fattura che non ho
> mai letto.

**Contesto.** È la storia in cui si applica la regola di sicurezza più importante del catalogo (§8): gli strumenti
di scrittura con effetti irreversibili producono una **bozza** e richiedono una **conferma umana esplicita**. In
BillGrove ci sono due atti che non si disfano — l'emissione, che consuma un numero progressivo, e l'invio, che
fa uscire il documento — e sono esattamente i due su cui la conferma è obbligatoria. Il resto crea bozze, che si
possono buttare.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti di **scrittura**: `crea_preventivo`, `crea_fattura`,
   `converti_preventivo_in_fattura`, `emetti_nota_di_credito`, `registra_incasso`, e i due irreversibili
   `emetti_documento` e `invia_documento`.
2. **RF-2** — Gli strumenti che creano documenti producono sempre una **bozza**: nessuno di essi emette.
3. **RF-3** — I due strumenti irreversibili richiedono una conferma umana esplicita, che non può essere prodotta
   dall'assistente stesso: la conferma è un atto separato, riferito a una bozza precisa.
4. **RF-4** — La conferma **scade**: una bozza confermabile che non viene confermata entro un tempo breve richiede
   di essere ripresentata.
5. **RF-5** — Ogni strumento di scrittura restituisce, insieme alla bozza, ciò che l'utente deve vedere per
   decidere: cliente, righe, totali calcolati **dal servizio**, provenienza dei prezzi, effetto sulla quota.
6. **RF-6** — Ogni strumento di scrittura è **ripetibile senza danno**: la stessa richiesta con la stessa chiave di
   idempotenza non produce due bozze.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Come per la lettura, il `tenant_id` viene dal contesto della chiamata
  autenticata; nessuno strumento accetta un identificativo di account fra i parametri.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti, comprese le loro
  validazioni: nessuna scorciatoia che aggiri i controlli dell'interfaccia di programmazione. Errori restituiti
  all'assistente in forma comprensibile, così che possa spiegare all'utente che cosa manca.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing` per le bozze in attesa di conferma e la loro
  scadenza, con `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo.
- **RT-4 — Modulo frontend (§3).** La conferma umana avviene **nell'interfaccia** — è lì che la persona vede e
  approva. Le bozze in attesa di conferma compaiono nella Panoramica.
- **RT-5 — Cinque lingue (§4).** Le stringhe visibili della conferma passano dallo spazio-nomi `billing` in tutte e
  cinque le lingue; le descrizioni degli strumenti restano in inglese (storia `0028`).
- **RT-6 — Varchi e quota (§6, §7).** La creazione di una bozza **non** consuma quota; `emetti_documento` la
  consuma, come dall'interfaccia. La risposta dello strumento dichiara che l'emissione consumerà una unità: chi
  conferma deve sapere anche questo.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza la regola: `emetti_documento` e
  `invia_documento` sono **scrittura irreversibile** con conferma umana **obbligatoria**; gli altri sono scrittura
  con bozza e conferma. Nessuno strumento cancella dati. Dipendenza dichiarata: UC 0061-0064.
- **RT-8 — Dati personali (§10).** I parametri degli strumenti di scrittura possono contenere dati personali (il
  nome di un cliente nuovo): non si registrano nei log e non si conservano oltre la bozza. Va dichiarato nel
  manifesto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `bozza creata da strumento`, `conferma richiesta`, `conferma
  concessa`, `conferma scaduta` sono registrati con `tenant_id`, `app_id`, `user_id`, nome dello strumento e
  identificativo di correlazione, **senza i parametri**.

## 4. Criteri di accettazione

**CA-1 — Creazione di una bozza**
- **Dato** un cliente esistente
- **Quando** si invoca `crea_fattura` con due righe
- **Allora** nasce una fattura in stato `bozza`, con i totali calcolati dal servizio, e **nessun numero assegnato**

**CA-2 — Emissione senza conferma**
- **Dato** una bozza pronta · **Quando** si invoca `emetti_documento` senza una conferma umana valida
- **Allora** l'operazione è rifiutata e il documento resta in bozza

**CA-3 — Emissione con conferma**
- **Dato** la stessa bozza e una conferma umana concessa nell'interfaccia
- **Quando** l'emissione viene eseguita
- **Allora** il documento è emesso, numerato, e una unità di quota è consumata

**CA-4 — Conferma scaduta**
- **Dato** una conferma richiesta e non concessa entro il tempo previsto
- **Quando** si tenta di usarla · **Allora** è rifiutata e va richiesta di nuovo

**CA-5 — Ripetizione senza danno**
- **Dato** la stessa invocazione di `crea_fattura` con la stessa chiave di idempotenza
- **Quando** viene eseguita due volte · **Allora** esiste una sola bozza

**CA-6 — Isolamento fra account**
- **Dato** una bozza dell'account `B`
- **Quando** si invoca `emetti_documento` nel contesto di `A` su quella bozza
- **Allora** l'operazione fallisce come se la bozza non esistesse

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla scadenza della conferma e sull'idempotenza, di **integrazione** sull'intero giro
      bozza → conferma → emissione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento di scrittura;
- [ ] prova **specifica** che nessun percorso permette a uno strumento di emettere o inviare senza conferma umana:
      è la prova che protegge la regola di sicurezza del catalogo;
- [ ] **prova end-to-end**: *rimando* — non esiste un livello conversazionale da guidare; la conferma nell'interfaccia
      è però coperta dal percorso `[J-BILLING]`. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per la schermata di conferma;
- [ ] **manifesto dei dati** aggiornato per i parametri degli strumenti;
- [ ] **registro delle decisioni** compilato, con annotata la regola «la chat prepara, la persona approva»;
- [ ] contratto degli **strumenti conversazionali** dichiarato, con la marcatura di irreversibilità;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0028` | Riusa la forma del contratto e la risoluzione del contesto |
| UC 0061-0064 (piattaforma, non implementate) | Server conversazionale, consenso delegato e applicazione dei varchi alle chiamate dell'assistente |

## 7. Fuori ambito

- gli strumenti che cancellano dati: **non esistono e non devono esistere** in questa app;
- l'attivazione di solleciti da chat: esclusa (storia `0019`);
- la creazione di modelli ricorrenti da chat: esclusa (storia `0020`);
- l'applicazione dei varchi di abilitazione e quota alle chiamate: storia `0030`.

## 8. Punti aperti

Quanto debba durare la validità di una conferma è una decisione di prodotto, non tecnica: troppo corta rende
l'assistente inutilizzabile, troppo lunga svuota la protezione. La proposta è «pochi minuti», e va confermata dallo
sviluppatore.
