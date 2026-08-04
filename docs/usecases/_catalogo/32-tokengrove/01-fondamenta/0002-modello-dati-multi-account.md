# 0002 — Modello dati multi-account

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio le tabelle di base dell'app, con l'isolamento fra account già dimostrato da una prova
> così da poter scrivere le storie di dominio senza dover tornare a rimettere in discussione il fondamento.

**Contesto.** La tabella centrale di questa app è la **misura di consumo**, ed è particolare per due motivi che
vanno affrontati adesso e non dopo: sarà di gran lunga la più popolosa (una riga per chiamata a un modello, anche
milioni al mese per account) e porta un **costo congelato** che non deve più cambiare. Sbagliare adesso la sua
forma — per esempio dimenticare l'identificativo esterno che serve alla deduplica, o non prevedere la versione di
listino accanto al costo — costa una migrazione su una tabella grande, che è la cosa più scomoda che ci sia.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le tabelle `fonte`, `misura`, `rendiconto` e `dimensione`, ciascuna con `tenant_id`, chiave
   primaria a UUID versione 7, colonne di controllo e cancellazione logica.
2. **RF-2** — La tabella `misura` porta: istante della chiamata, fornitore, chiave del modello, i quattro conteggi
   (ingresso, uscita, ingresso servito da cache, scrittura in cache), l'identificativo esterno della chiamata,
   l'origine (rendiconto o invio), le etichette, il costo congelato e la versione di listino usata.
3. **RF-3** — Esiste un vincolo di unicità su (`tenant_id`, `fonte`, `identificativo_esterno`) che rende
   impossibile registrare due volte la stessa chiamata: è il fondamento della deduplica (storia `0010`).
4. **RF-4** — Le interrogazioni tipiche dell'app — somma degli importi per periodo, per modello e per etichetta —
   sono servite da indici pensati per esse, e la scelta degli indici è motivata nel registro delle decisioni.
5. **RF-5** — Un tentativo di leggere o scrivere una riga di un altro account fallisce, sia passando per le rotte
   sia forzando `tenant_id` dal corpo della richiesta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle entità `fonte`, `misura`, `rendiconto` e
  `dimensione` filtra per `tenant_id` preso dal gettone verificato; un `tenant_id` che arrivasse dal corpo della
  richiesta o dai parametri viene ignorato. Prova di isolamento fra due account su ogni tabella introdotta.
- **RT-2 — Persistenza (§8).** Migrazione `V2__tabelle_di_base.sql` sullo schema `app_spesa_modelli`: le quattro
  tabelle con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`. Nessuna chiave
  esterna verso altri schemi: `tenant_id` è un riferimento logico.
- **RT-3 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in questa storia oltre a quelle già esistenti;
  la definizione OpenAPI cambia solo se cambiano gli schemi già pubblicati.
- **RT-4 — Dati personali (§10).** La colonna delle etichette di `misura` **può** contenere dati riferibili a una
  persona (identificativo di utente finale, ragione sociale di una ditta individuale): il campo si annota come dato
  personale, la voce entra nel manifesto in italiano e inglese, e la tabella entra fin da subito in `exportData` e
  `purgeData` del contratto dati dell'app — anche se il contratto sarà completato nella storia `0035`. Rimandare
  questo passo è il modo con cui si dimentica una tabella.
- **RT-5 — Registrazione eventi (§14).** Nessun evento applicativo nuovo; le migrazioni sono registrate dallo
  strumento di migrazione con `tenant_id` assente (sono operazioni di schema, non di account).

## 4. Criteri di accettazione

**CA-1 — Le tabelle esistono con la forma attesa**
- **Dato** un database effimero
- **Quando** si applicano le migrazioni vere
- **Allora** lo schema `app_spesa_modelli` contiene le quattro tabelle, ciascuna con `tenant_id`, chiave primaria a
  UUID versione 7, colonne di controllo e `deleted_at`

**CA-2 — La stessa chiamata non si conta due volte**
- **Dato** una fonte e una misura già registrata con identificativo esterno `abc-123`
- **Quando** si tenta di inserire una seconda riga con la stessa fonte e lo stesso identificativo esterno
- **Allora** l'inserimento è rifiutato dal vincolo di unicità e nulla viene duplicato

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie misure
- **Quando** un utente di `A` chiede l'elenco delle misure
- **Allora** vede solo le proprie, anche se forza l'identificativo dell'account `B` nel corpo della richiesta o nei
  parametri

**CA-4 — Le somme per periodo non degenerano**
- **Dato** un account con centomila misure distribuite su tre mesi
- **Quando** si chiede la somma degli importi del mese corrente raggruppata per modello
- **Allora** la risposta arriva con un piano di esecuzione che usa gli indici previsti e non una scansione
  completa della tabella

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sui repository e di **integrazione** sulle migrazioni, con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** su ognuna delle quattro tabelle;
- [ ] **prova end-to-end**: nessun impatto (nessuna superficie utente in questa storia);
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce delle etichette della misura, campo
      annotato, tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta degli indici e sull'unicità che regge la
      deduplica;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione del modello di dominio allineata al documento capofila.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Servono lo schema e il servizio che ospita le migrazioni |

## 7. Fuori ambito

- il **calcolo** del costo che riempie la colonna congelata: è dell'epica 03 (storia `0014`);
- le tabelle di budget, avviso e regola di attribuzione: nascono nelle epiche che le usano, per non creare tabelle
  vuote di cui nessuno ricorda la forma;
- la strategia di conservazione dello storico per piano: è una funzionalità del listino e vive nella storia `0004`.

## 8. Punti aperti

- **Il tipo di dato dell'importo.** Un importo monetario si conserva in valuta minima intera o in decimale a
  precisione fissa; qui il costo di una singola chiamata può valere frazioni di centesimo e sommarne un milione
  deve restare esatto. La scelta ha conseguenze su tutta l'app ed è legata al punto P7 del documento capofila
  (valuta e cambio). La chiude lo sviluppatore prima della storia `0014`.
