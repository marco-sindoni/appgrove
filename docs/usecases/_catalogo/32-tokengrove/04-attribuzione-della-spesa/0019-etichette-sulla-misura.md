# 0019 — Etichette sulla misura

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 04 — Attribuzione della spesa
**Storia**: `0019` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che ha strumentato il proprio prodotto
> voglio poter dire, insieme a ogni misura, per chi e per quale funzione era quella chiamata
> così da vedere il costo per cliente e per funzionalità, che è il numero per cui sto pagando questo prodotto.

**Contesto.** È il ponte fra le due storie precedenti: le dimensioni dicono quali assi esistono (storia `0018`), il
ricevitore accetta le misure (storia `0009`), e qui i valori delle etichette vengono confrontati con gli assi
dichiarati. Il confronto va fatto **al momento della ricezione** e non alla lettura: un'etichetta sbagliata scoperta
tre settimane dopo è una spesa che nessuno recupererà più. È anche il punto in cui la promessa di non trattare dati
personali si mette alla prova, perché l'etichetta è l'unico testo libero che entra nell'app.

## 2. Requisiti funzionali

1. **RF-1** — Le etichette di una misura sono confrontate con le dimensioni dichiarate dall'account: quelle
   riconosciute diventano attribuzione, quelle non riconosciute sono conservate ma segnalate come «asse
   sconosciuto».
2. **RF-2** — Una dimensione obbligatoria senza valore rende la misura **non attribuita su quell'asse**, e il
   conteggio delle misure non attribuite è disponibile subito, non a fine mese.
3. **RF-3** — I valori delle etichette sono normalizzati in modo dichiarato e prevedibile (spazi rimossi ai bordi,
   confronto senza distinzione fra maiuscole e minuscole), così che `Cliente Rossi` e `cliente rossi` non diventino
   due voci di spesa diverse.
4. **RF-4** — La schermata mostra, per ogni asse, i valori raccolti con il rispettivo importo, e permette di
   **unire** due valori che sono la stessa cosa scritta in due modi, con effetto dichiarato sul passato o solo sul
   futuro.
5. **RF-5** — L'interfaccia e la documentazione avvertono in modo esplicito che le etichette **non sono il posto
   per dati personali**; un valore che ha la forma di un indirizzo di posta viene accettato ma **segnalato**, con
   il suggerimento di sostituirlo con uno pseudonimo.
6. **RF-6** — Le etichette rispettano i limiti del contratto (storia `0008`): numero massimo per misura, lunghezza
   massima di chiave e valore.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le etichette e i loro valori si leggono e si scrivono solo nel contesto
  del `tenant_id` della chiave di invio o del gettone verificato. Prova di isolamento: i valori di un account non
  compaiono mai nei suggerimenti di un altro.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `valore_dimensione` con
  `tenant_id`, dimensione, valore normalizzato, valore come ricevuto, primo e ultimo avvistamento, colonne di
  controllo e cancellazione logica. La misura punta ai valori, così che un'unione di due valori non richieda di
  riscrivere milioni di righe.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET /api/spesa_modelli/v1/dimensioni/{chiave}/valori` e
  `POST .../valori/unione`; errori in `problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Attribuzione», scheda «Valori»; l'avvertenza sui dati personali è
  visibile accanto all'elenco, non in una nota a piè di pagina. Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`, avvertenza compresa.
- **RT-6 — Esposizione conversazionale (§12).** I valori sono il parametro `filtro` di `leggi_spesa` e di
  `elenca_maggiori_consumatori` (storia `0032`), marcati lettura. L'**unione** di due valori è una scrittura che
  cambia dati passati: se esposta, richiede bozza e conferma (storia `0033`).
- **RT-7 — Dati personali (§10).** È la storia che rende concreta la voce di manifesto dichiarata nella storia
  `0008`: le colonne dei valori si annotano come dati personali, la tabella `valore_dimensione` entra in
  `exportData` e `purgeData`, e le voci del manifesto in italiano e inglese descrivono che si tratta di
  identificativi indiretti scelti dal cliente.
- **RT-8 — Registrazione eventi (§14).** Eventi «asse sconosciuto ricevuto», «valore unito» con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione. **Il valore dell'etichetta non compare nei registri**,
  perché può essere un dato personale: si registra la chiave dell'asse, non il valore.

## 4. Criteri di accettazione

**CA-1 — Attribuzione riuscita**
- **Dato** un account con l'asse «cliente» dichiarato e una misura con etichetta `cliente=acme`
- **Quando** la misura viene registrata
- **Allora** compare nella spesa raggruppata per cliente sotto `acme`

**CA-2 — Normalizzazione**
- **Dato** due misure con etichette `cliente=Acme` e `cliente= acme `
- **Quando** si legge la spesa per cliente
- **Allora** compare una sola voce con la somma delle due

**CA-3 — Asse sconosciuto**
- **Dato** una misura con etichetta `reparto=vendite` e nessun asse «reparto» dichiarato
- **Quando** viene registrata
- **Allora** la misura è accettata, l'etichetta è conservata e la schermata segnala che esiste un asse ricevuto e
  non dichiarato, proponendo di dichiararlo

**CA-4 — Etichetta che sembra un dato personale**
- **Dato** una misura con etichetta `utente_finale=mario.rossi@esempio.test`
- **Quando** viene registrata
- **Allora** è accettata e compare la segnalazione che suggerisce di usare uno pseudonimo; il valore non compare
  in alcun registro

**CA-5 — Unione di due valori**
- **Dato** i valori `acme` e `acme s.r.l.` sullo stesso asse
- **Quando** si uniscono scegliendo l'effetto anche sul passato
- **Allora** la spesa passata dei due valori confluisce in uno solo e l'operazione resta tracciata

**CA-6 — Isolamento fra account**
- **Dato** due account che usano entrambi il valore `acme`
- **Quando** ciascuno legge i propri valori
- **Allora** vede solo i propri importi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione e sul riconoscimento della forma di indirizzo di posta, e di
      **integrazione** sull'attribuzione al momento della ricezione;
- [ ] prova di **isolamento fra account** sui valori delle dimensioni;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «invio con etichette,
      spesa raggruppata per cliente», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: colonne annotate, tabella `valore_dimensione` in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di segnalare invece di respingere le
      etichette che sembrano dati personali (punto P4 del documento capofila);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0018` | Servono le dimensioni con cui confrontare le etichette |
| Storia `0009` | Serve il ricevitore da cui arrivano |
| Punto aperto P4 del documento capofila | La decisione «segnalare o respingere» va presa prima di scrivere il controllo |

## 7. Fuori ambito

- l'attribuzione di ciò che arriva **senza** etichette, cioè dal rendiconto: è la storia `0020`;
- la riduzione del non attribuito e la sua misura: è la storia `0021`.

## 8. Punti aperti

- **Se l'unione di due valori debba poter agire sul passato.** Cambia numeri che il cliente può aver già usato: è
  lo stesso problema del ricalcolo (storia `0017`) applicato all'attribuzione. La proposta è **sì, ma con conferma
  esplicita e traccia**, come per il ricalcolo. La chiude lo sviluppatore.
