# 0010 — Conservazione e attribuzione dei contenuti di terzi

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 02 — Sedi e collegamento alle piattaforme
**Storia**: `0010` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della piattaforma appgrove
> voglio che l'app conservi dei contenuti presi dalle piattaforme di recensione solo quello che le loro condizioni
> permettono, e li mostri sempre con l'attribuzione dovuta
> così da non costruire un archivio che un giorno ci verrà chiesto di smontare.

**Contesto.** RepGrove copia dentro di sé contenuti che non sono suoi: recensioni scritte da terzi, ospitate da
piattaforme che hanno condizioni d'uso proprie. È una posizione scomoda e va presidiata **una volta sola, presto**,
invece di rincorrerla in ogni schermata. Qui c'è anche un limite dichiarato della mia analisi: la pagina ufficiale
delle politiche di una delle due interfacce ha risposto con un errore del server e i termini della piattaforma
mappe si sono troncati in lettura; le fonti secondarie concordano nel dire che i contenuti non si conservano, ma
**non l'ho verificato** (descrizione §2.7 e §11.2). Questa storia esiste proprio per rendere il punto **esplicito
nel codice** invece che implicito: c'è un solo posto che decide quanto si conserva e cosa si mostra, e quando la
verifica sarà fatta si cambierà lì.

## 2. Requisiti funzionali

1. **RF-1** — Esiste una politica di conservazione dichiarata **per piattaforma**, scritta in un solo posto e
   leggibile: cosa si conserva (identificativo, voto, momento, autore pubblico, testo), per quanto, e cosa invece
   si ricarica dal vivo.
2. **RF-2** — La politica ha un valore predefinito **prudenziale**: si conserva il minimo che serve a far
   funzionare le funzioni dell'app, e le recensioni non più pubbliche all'origine escono dalla vista e vengono
   cancellate entro un termine dichiarato.
3. **RF-3** — Ogni schermata che mostra una recensione mostra **la piattaforma d'origine** e un collegamento alla
   recensione originale. Vale nel backoffice e vale — a maggior ragione — nel riquadro pubblico (storia 0024).
4. **RF-4** — Una lavorazione periodica applica la politica: cancella ciò che è scaduto, con una riga di prova di
   quante righe ha cancellato e perché.
5. **RF-5** — Se domani la verifica delle condizioni imponesse di **non conservare il testo**, il cambiamento deve
   costare una configurazione e la disattivazione di due funzioni (ricerca nel testo e analisi dei temi), non una
   riscrittura: le funzioni che dipendono dal testo lo dichiarano e sanno spegnersi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione di scadenza opera per account e non attraversa mai il
  confine; il conteggio delle righe cancellate è per account.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova rivolta al cliente. Le risposte che
  contengono recensioni portano sempre i campi di attribuzione: se mancano, la prova fallisce.
- **RT-3 — Persistenza (§8).** Colonna `scade_il` su `recensione`, calcolata dalla politica; la cancellazione
  qui è **fisica**, non logica, perché non è una cancellazione dell'utente ma l'esecuzione di un limite di
  conservazione. Migrazione `V5__recensione_conservazione.sql`.
- **RT-4 — Modulo frontend (§3, §5).** Ogni riga e ogni scheda di recensione mostra origine e collegamento; nella
  sezione *Impostazioni* una pagina spiega, in parole comuni, cosa l'app conserva e per quanto.
- **RT-5 — Cinque lingue (§4).** Il testo che spiega la conservazione è visibile al cliente e va tradotto in tutte
  e cinque le lingue. È un testo delicato: va scritto da chi sa cosa fa il sistema, non tradotto a orecchio.
- **RT-6 — Varchi e quota (§6, §7).** Non applicabile: la conservazione non è una funzione a pagamento e non si
  può disattivare comprando un piano più caro.
- **RT-7 — Esposizione conversazionale (§12).** Gli strumenti di lettura (storia 0027) restituiscono le recensioni
  **con l'attribuzione**: un assistente che riporta una recensione senza dire da dove viene sta facendo la stessa
  cosa sbagliata di una schermata senza attribuzione.
- **RT-8 — Dati personali (§10).** La politica di conservazione dei contenuti di terzi **si somma** ai termini di
  conservazione del manifesto dei dati, non li sostituisce: vince il più breve. Va scritto nel manifesto in
  italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** `scadenza applicata: n righe cancellate` per account e piattaforma, con
  identificativo di correlazione, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — La politica è uno e uno solo**
- **Dato** il codice del servizio
- **Quando** si cerca dove è deciso quanto si conserva
- **Allora** esiste un solo punto, dichiarativo, e nessuna funzione decide per conto proprio

**CA-2 — L'attribuzione c'è sempre**
- **Dato** una recensione mostrata in una qualunque schermata o restituita da una qualunque rotta
- **Quando** la si guarda
- **Allora** porta piattaforma d'origine e collegamento alla recensione originale

**CA-3 — Ciò che scade se ne va**
- **Dato** una recensione non più pubblica all'origine da più del termine dichiarato
- **Quando** la lavorazione di scadenza gira
- **Allora** la riga è cancellata fisicamente e resta una riga di prova con il conteggio, senza contenuti

**CA-4 — Spegnere il testo non rompe l'app**
- **Dato** la politica configurata per non conservare il testo
- **Quando** si apre l'elenco delle recensioni
- **Allora** l'elenco funziona con voto, data e autore, la ricerca nel testo e l'analisi dei temi sono spente con
  una spiegazione, e nessuna schermata va in errore

**CA-5 — Isolamento fra account**
- **Dato** due account con recensioni scadute
- **Quando** la lavorazione gira per l'account `A`
- **Allora** non tocca nessuna riga dell'account `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prova di **unità** sul calcolo della scadenza e sul comportamento con il testo spento; prova di
      **integrazione** sulla lavorazione di scadenza con database effimero;
- [ ] prova di **isolamento fra account** sulla lavorazione di scadenza;
- [ ] **prova end-to-end**: *nessun impatto* diretto — l'attribuzione è però verificata dentro il percorso
      `[J-RECENSIONI]` (storia 0030) come asserzione sulla schermata dell'elenco;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per la pagina che spiega la conservazione;
- [ ] **manifesto dei dati** aggiornato con i termini di conservazione effettivi, in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con **scritto a chiare lettere** che le condizioni delle piattaforme
      non sono state verificate sulle pagine ufficiali e cosa si è assunto in attesa;
- [ ] contratto degli **strumenti conversazionali**: l'attribuzione entra nello schema del risultato degli
      strumenti di lettura.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | serve avere recensioni da conservare |
| **verifica delle condizioni delle due piattaforme** | è un compito di lettura, non di codice, e va fatto prima di fissare i valori predefiniti |

## 7. Fuori ambito

- la cancellazione richiesta da un interessato: passa dal contratto dati dell'app, non da qui;
- il riquadro pubblico, che ha regole proprie di attribuzione e trasparenza — storie 0024 e 0025.

## 8. Punti aperti

- **Le condizioni non sono state verificate** (descrizione §2.7). È il punto aperto più importante di questa
  storia e va chiuso leggendo i termini delle due piattaforme prima di fissare i valori predefiniti.
- **Se il testo non si potesse conservare**, saltano l'analisi dei temi (storia 0023) e la ricerca nel testo
  (storia 0017): sono due funzioni intere, non due dettagli. La struttura scelta qui serve a rendere quel colpo
  sopportabile, non indolore.
</content>
