# 0014 — Modelli di preventivo e testi standard

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 03 — Redazione dell'offerta
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che manda offerte simili tutte le settimane
> voglio partire da un modello con la mia intestazione, i miei testi e le mie condizioni già scritte
> così da non ricopiare ogni volta le stesse quattro paragrafi e da non dimenticarne uno importante.

**Contesto.** «Partire da un modello» è la prima cosa che il segmento chiede (§2.5 della descrizione
dell'applicazione) ed è anche il modo in cui l'app aiuta a rispettare l'obbligo del preventivo scritto per i
professionisti (§2.3, punto 1: grado di complessità e costi prevedibili vanno comunicati). Il modello è anche il
posto in cui il cliente-titolare mette, se il destinatario è un consumatore, le informazioni precontrattuali e
l'avviso sul recesso: **i testi li scrive lui, non appgrove**.

## 2. Requisiti funzionali

1. **RF-1** — Si creano modelli con: intestazione, testo di apertura, testo di chiusura, condizioni di pagamento,
   condizioni di validità, note legali; uno è il modello predefinito dell'account.
2. **RF-2** — Ogni testo del modello si compila in ciascuna delle cinque lingue; alla creazione del preventivo si
   usa la versione nella lingua preferita del destinatario, con ricaduta sulla lingua predefinita dell'account se
   manca.
3. **RF-3** — I testi ammettono **segnaposto** semplici e dichiarati (nome del destinatario, numero del
   preventivo, data di validità, totale): l'elenco è chiuso e documentato in interfaccia.
4. **RF-4** — Un modello può essere marcato «per destinatari consumatori»: l'app lo propone quando il destinatario
   è di quel tipo e avvisa se non contiene i blocchi che l'account ha dichiarato obbligatori.
5. **RF-5** — Cambiare un modello **non** cambia i preventivi già creati: il testo viene copiato nel documento al
   momento della creazione, non collegato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I modelli filtrano per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `/api/preventivi/v1/modelli`; corpo validato; errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V8__modelli.sql`: tabella `modello_preventivo` con `tenant_id`, UUID
  versione 7, colonne di controllo, cancellazione logica; i testi per lingua in una tabella figlia.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Modelli**; editor di testo semplice, non un elaboratore di
  documenti; anteprima con i segnaposto risolti; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Doppia attenzione: **l'interfaccia** della sezione è nelle cinque lingue dallo
  spazio-nomi `preventivi`; **i testi del modello** sono contenuto del cliente, e le cinque lingue sono i cinque
  campi che lui compila.
- **RT-6 — Dati personali (§10).** I testi dei modelli sono liberi e possono nominare persone: la tabella entra in
  `exportData` e `purgeData` (storia `0007`), e va detto perché.
- **RT-7 — Registrazione eventi (§14).** `modello creato`, `modello applicato` con gli identificativi d'obbligo,
  senza il contenuto dei testi.

## 4. Criteri di accettazione

**CA-1 — Preventivo che nasce già scritto**
- **Dato** un modello predefinito con testi in italiano · **Quando** si crea un preventivo per un destinatario
  italiano · **Allora** il documento contiene già apertura, chiusura e condizioni, modificabili

**CA-2 — Lingua del destinatario**
- **Dato** un destinatario con lingua preferita francese e un modello compilato in francese · **Quando** si crea
  il preventivo · **Allora** i testi sono in francese; se il francese mancasse, si userebbe la lingua predefinita
  dell'account e l'app lo direbbe

**CA-3 — Segnaposto**
- **Dato** un testo che contiene il segnaposto del numero del preventivo · **Quando** si apre l'anteprima
- **Allora** il segnaposto è sostituito dal numero reale, e un segnaposto sconosciuto è segnalato come errore
  invece di essere lasciato a vista

**CA-4 — Modello per consumatori**
- **Dato** un destinatario marcato consumatore e un modello che non contiene il blocco sul recesso dichiarato
  obbligatorio dall'account · **Quando** si crea il preventivo · **Allora** l'app avvisa prima dell'invio

**CA-5 — Modifica del modello senza effetti retroattivi**
- **Dato** un preventivo creato ieri da un modello · **Quando** oggi si cambia il modello · **Allora** il
  preventivo di ieri resta identico

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sui segnaposto e sulla ricaduta di lingua, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sui modelli;
- [ ] **prova end-to-end**: rimando alla storia `0029`;
- [ ] **traduzioni** dell'interfaccia in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: la tabella dei modelli entra in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato (elenco chiuso dei segnaposto, copia anziché collegamento,
      avviso per i consumatori);
- [ ] avvio locale invariato; dati di prova estesi con un modello.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | il modello si applica a un preventivo |

## 7. Fuori ambito

- la generazione automatica dei testi con un modello linguistico: non è di questa storia; se arriverà, arriverà
  dal livello conversazionale (epica 06) con bozza e conferma;
- fornire testi legali predefiniti: sarebbe consulenza legale, non una funzione (vedi punti aperti di `0006`);
- l'impaginazione del documento: storia `0016`.

## 8. Punti aperti

Nessuno.
