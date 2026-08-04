# 0014 — Manifesto dati e diritti dell'interessato

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 03 — Scheda tecnica e storia del cliente
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come cliente di un salone che chiede una copia dei propri dati, o la loro cancellazione
> voglio riceverli tutti, davvero tutti, e che cancellati voglia dire cancellati
> così da non dover fidarmi di una promessa che nessuno ha verificato.

**Contesto.** Questa storia chiude il contratto dei dati del verticale. Non è una formalità: il manifesto è la
**fonte unica** da cui si generano il registro dei trattamenti e gli strumenti di esportazione e cancellazione, e
un campo non dichiarato è un campo che l'esportazione dimentica e la cancellazione lascia indietro. Nel verticale
beauty le candidate a essere dimenticate sono due e le nomino subito: le **fotografie** (perché non sono una
colonna di testo) e i **movimenti di magazzino** (perché sembrano un registro tecnico e invece dicono chi ha fatto
cosa). C'è poi un caso scomodo, il pacchetto pagato e non consumato, che va deciso e non aggirato.

## 2. Requisiti funzionali

1. **RF-1** — Il manifesto dei dati del verticale è completo in **italiano e inglese** su ogni testo, e comprende
   tutte le voci introdotte dalle epiche 02-06.
2. **RF-2** — L'esportazione restituisce, per un cliente, tutto ciò che lo riguarda: schede tecniche, preferenze
   di variante, conti e righe, pacchetti e utilizzi, punti fedeltà e movimenti, fotografie.
3. **RF-3** — La cancellazione è **fisica** e lascia una riga di prova nel registro delle purghe: sostituire il
   nome con un codice non è cancellare.
4. **RF-4** — La cancellazione di un cliente che ha un **pacchetto pagato e non consumato** chiude il pacchetto
   lasciando un movimento senza intestatario con l'importo residuo, e annulla le sedute future collegate: il
   salone deve continuare a sapere di dovere qualcosa a qualcuno, anche se non sa più a chi.
5. **RF-5** — L'esportazione e la cancellazione riguardano anche i dati di **chi lavora nel salone** (regole di
   provvigione, prospetti, attribuzioni sulle righe di conto): sono persone anche loro, ed è la parte che si
   dimentica più spesso.
6. **RF-6** — I diritti dell'interessato restano accessibili **anche** con abbonamento scaduto o app disabilitata.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** L'app implementa il contratto dei dati con `appId()`, `exportData(scope)`,
  `purgeData(scope)` e `manifest()`. **Ogni** tabella che contiene dati di persone compare in entrambi:
  `scheda_tecnica`, `foto_trattamento`, `conto`, `riga_conto`, `pacchetto`, `utilizzo_pacchetto`,
  `tessera_fedelta`, `movimento_punti`, `regola_provvigione`, `prospetto_provvigioni`, `movimento_magazzino` (per
  la colonna di chi l'ha causato), più la preferenza di variante della storia `0008`.
- **RT-2 — Parità delle lingue.** Il controllo automatico di parità italiano/inglese sui manifesti è già nella
  suite: la storia non è conclusa se una voce è in una lingua sola.
- **RT-3 — Campi annotati.** Ogni campo che riguarda una persona è annotato `@PersonalData`; un campo annotato e
  non dichiarato nel manifesto fa fallire la compilazione, e questo è il presidio su cui la storia poggia.
- **RT-4 — Isolamento fra account (§1).** Esportazione e cancellazione agiscono solo dentro l'account del token
  verificato; il perimetro dell'operazione si calcola dal `tenant_id`, mai da un identificativo della richiesta.
- **RT-5 — Immutabilità e cancellazione.** I movimenti sono immutabili (storia `0002`), ma la cancellazione dei
  dati di una persona è un diritto che vince: la purga rimuove il collegamento alla persona **e** il movimento
  quando il movimento esiste solo per lei, lasciando una riga di prova.
- **RT-6 — Cinque lingue (§4).** I testi rivolti all'utente che accompagnano esportazione e cancellazione in
  `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento** espone esportazione o cancellazione: sono
  atti che si compiono guardando in faccia un'interfaccia, non delegando a un assistente.
- **RT-8 — Registrazione eventi (§14).** `esportazione richiesta`, `purga eseguita` con `tenant_id`, `app_id`,
  `user_id`, correlazione e conteggi — **mai** identità dell'interessato.

## 4. Criteri di accettazione

**CA-1 — L'esportazione non dimentica niente**
- **Dato** una cliente con schede tecniche, fotografie, conti, un pacchetto, punti fedeltà e una preferenza di
  variante
- **Quando** si esportano i suoi dati
- **Allora** l'esportazione contiene tutte e sei le categorie, **fotografie comprese**

**CA-2 — Cancellato vuol dire cancellato**
- **Dato** la stessa cliente
- **Quando** si esegue la cancellazione
- **Allora** nessuna riga e nessun file la riguarda più, in nessuna delle tabelle dichiarate, e nel registro delle
  purghe c'è la prova dell'operazione

**CA-3 — Il pacchetto non consumato**
- **Dato** una cliente con un pacchetto da dieci sedute di cui tre usate
- **Quando** si cancellano i suoi dati
- **Allora** il pacchetto risulta chiuso, resta un movimento senza intestatario con il residuo, e le sedute future
  collegate sono annullate

**CA-4 — Anche chi lavora nel salone**
- **Dato** un operatore che ha lasciato il salone
- **Quando** se ne cancellano i dati
- **Allora** regole di provvigione, prospetti e attribuzioni sulle righe di conto sono trattati secondo quanto
  dichiarato nel manifesto, e nessuno dei tre resta indietro in silenzio

**CA-5 — Diritti accessibili anche senza abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede l'esportazione dei propri dati
- **Allora** la ottiene

**CA-6 — Nessuna voce solo in una lingua**
- **Dato** il manifesto del verticale
- **Quando** gira il controllo di parità delle lingue
- **Allora** è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera; l'area `compliance` è quella che conta);
- [ ] prova di **integrazione** che esporta e poi cancella un cliente **completo** e verifica che non resti nulla,
      tabella per tabella, con un elenco che fallisce se una tabella nuova non è stata considerata;
- [ ] prova di **isolamento fra account** su esportazione e cancellazione;
- [ ] **prova end-to-end**: *rimando* — i diritti dell'interessato hanno un percorso di piattaforma proprio; qui
      si dichiara e si registra in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** completo, in italiano e inglese, **con la nota su ciò che non si tratta** (storia
      `0012`);
- [ ] **registro delle decisioni**: durate di conservazione proposte e loro motivo, trattamento del pacchetto non
      consumato, trattamento dei dati di chi lavora nel salone;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0010`, `0012`, `0013` | sono le voci più delicate del manifesto |
| epiche 04, 05, 06 | portano le altre tabelle: **questa storia va rifatta a ogni epica**, o si scrive alla fine |
| **decisione sui dati personali** (§6 della descrizione) | il manifesto si compila insieme allo sviluppatore, non da soli |

## 7. Fuori ambito

- la valutazione d'impatto, se la decisione dello sviluppatore aprisse la via (b) del §6: è un documento, non una
  storia di sviluppo;
- il registro dei trattamenti: si **genera** dal manifesto, non si scrive a mano.

## 8. Punti aperti

**Questa storia è scritta come se fosse l'ultima dell'epica 03, ma le sue voci arrivano fino all'epica 06.** Due
modi: chiuderla qui e riaprirla come voce della definizione di fatto di ogni storia successiva (proposta), oppure
spostarla in coda a tutto. Propongo il primo, perché un manifesto scritto alla fine è un manifesto ricostruito a
memoria — e la regola del repository è che si scrive **quando la decisione viene presa**.

**Le durate di conservazione non nascono da una norma.** Non ho trovato un obbligo che imponga di conservare le
schede tecniche per un tempo determinato (§2.3, punto 6 della descrizione). I 36 mesi proposti per la formula e i
24 per il resto sono minimizzazione ragionata, e vanno validati.
