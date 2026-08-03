# 0030 — Percorso end-to-end

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie precedenti — è l'ultima dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra l'app da capo a fondo, dal documento vuoto al documento conservato
> così da sapere che una modifica in un punto qualsiasi non ha rotto il percorso che il cliente fa davvero.

**Contesto.** Le storie precedenti hanno lasciato ciascuna una promessa di copertura: alcune «coprire ora», altre
«rimando» con questa storia come proprietaria. Qui si paga il conto. Il percorso end-to-end è quello dove si vede
se le parti si parlano: la macchina a stati, i fornitori simulati, la quota, il varco di conferma, l'archivio.
Il registro di copertura è sorvegliato da un controllo automatico: registro incoerente uguale suite rossa
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §11).

Va per ultima non per pigrizia ma perché un percorso end-to-end scritto a metà dell'applicazione copre metà
percorso e va riscritto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-EINVOICING.spec.ts`, e **ogni** test porta
   l'etichetta in testa al titolo: `test('[J-EINVOICING] …')`.
2. **RF-2** — Il percorso principale attraversa: accesso, creazione del soggetto emittente, creazione della
   controparte con verifica del recapito, inserimento di un documento, validazione **fallita** e correzione,
   trasmissione con **conferma umana**, arrivo della notifica, stato definitivo, comparsa in archivio,
   scarico del documento conservato.
3. **RF-3** — Un secondo percorso attraversa il caso **transfrontaliero** sulla rete a quattro angoli, perché è ciò
   che distingue l'app dai prodotti nazionali.
4. **RF-4** — Un terzo percorso attraversa il **blocco per quota**: si arriva al tetto e si verifica il `429` con
   il messaggio di rimedio.
5. **RF-5** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato
   con le voci di **tutte** le storie di questa applicazione: coperte, rimandate con motivo e storia proprietaria,
   oppure senza impatto.
6. **RF-6** — Il percorso gira sullo **stack locale reale** con i fornitori **simulati**, senza attese a tempo,
   con accesso programmatico e dati di prova deterministici e inventati.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso usa i due account dei dati di prova (storia `0005`) e
  include almeno un passo che verifica che un utente non veda i documenti dell'altro. L'isolamento non è provato
  solo dalle prove di unità: qui si verifica dall'interfaccia.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova.
- **RT-3 — Persistenza (§8).** Nessuna migrazione.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Il percorso verifica che le schermate esistenti
  funzionino in sequenza, non isolate.
- **RT-5 — Cinque lingue (§4).** Il percorso gira in **una** lingua; la copertura delle cinque lingue resta delle
  prove di unità del frontend e del controllo di completezza delle traduzioni. Va scritto, o sembrerà una lacuna.
- **RT-6 — Varchi e quota (§6, §7).** Il terzo percorso verifica il `429` a quota esaurita e il messaggio di
  rimedio; un quarto passo verifica il `402` con abbonamento `canceled` e che **l'esportazione resti accessibile**.
- **RT-7 — Esposizione conversazionale (§12).** ⚠️ **Gli strumenti conversazionali NON sono coperti da questo
  percorso**, perché il server non esiste (UC 0061-0063). Le voci corrispondenti del registro di copertura sono
  `da-coprire` con motivo «livello conversazionale non implementato» e storia proprietaria UC 0061. Va dichiarato,
  non taciuto: è la lacuna nota di questa applicazione.
- **RT-8 — Dati personali (§10).** I dati del percorso sono **inventati**: nomi di fantasia, identificativi
  fiscali formalmente validi ma non attribuiti, indirizzi su domini `*.test`. Mai dati veri.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Percorso principale verde**
- **Dato** lo stack locale avviato con i fornitori simulati
- **Quando** si esegue `[J-EINVOICING]`
- **Allora** il percorso completa dall'accesso allo scarico del documento conservato, senza attese a tempo

**CA-2 — Correzione dopo la validazione fallita**
- **Dato** il percorso principale
- **Quando** arriva al passo di validazione
- **Allora** la prima validazione **fallisce** con una diagnosi leggibile, l'utente corregge, e la seconda riesce:
  è il tratto che dimostra il valore dell'app

**CA-3 — Conferma umana**
- **Dato** il passo di trasmissione
- **Quando** il percorso lo attraversa
- **Allora** la trasmissione avviene **solo** dopo l'approvazione esplicita dall'interfaccia, e un tentativo senza
  approvazione non trasmette

**CA-4 — Percorso transfrontaliero**
- **Dato** un documento verso una controparte sulla rete a quattro angoli
- **Quando** si esegue il secondo percorso
- **Allora** il documento raggiunge lo stato `consegnato_al_destinatario` e **non** lo stato «accettato
  dall'autorità», che per quella famiglia non esiste

**CA-5 — Blocco per quota**
- **Dato** l'account di prova vicino al tetto
- **Quando** si esegue il terzo percorso
- **Allora** si raggiunge il `429` con il messaggio di rimedio, e nulla è stato trasmesso

**CA-6 — Registro coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** si esegue il controllo dell'area `tooling`
- **Allora** il controllo è verde: ogni voce punta a un test esistente e ogni test etichettato ha la sua voce

**CA-7 — Isolamento dall'interfaccia**
- **Dato** i due account dei dati di prova
- **Quando** il percorso accede con l'utente del primo
- **Allora** nell'elenco dei documenti non compare nulla del secondo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **intero**: è la storia in cui la suite completa deve essere verde, non
      solo le aree toccate;
- [ ] percorso `[J-EINVOICING]` scritto, con l'etichetta in testa a **ogni** titolo di test;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato con le voci di tutte le trenta storie, ciascuna in uno dei tre modi ammessi;
- [ ] le voci **rimandate** portano motivo e storia proprietaria, comprese quelle degli strumenti conversazionali
      (proprietaria: UC 0061);
- [ ] prova di **isolamento fra account** eseguita anche dall'interfaccia;
- [ ] controllo automatico di **accessibilità** sulle schermate principali attraversate;
- [ ] **traduzioni**: nessun testo nuovo; verificato che il percorso non dipenda da una lingua specifica per
      individuare gli elementi;
- [ ] **manifesto dei dati**: verificato che i dati del percorso siano inventati;
- [ ] **registro delle decisioni** compilato, con l'elenco delle lacune di copertura note e dichiarate.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Tutte le storie `0001`-`0029` | Il percorso attraversa l'app intera: è l'ultima per costruzione |
| `0005` | Servono i dati di prova e i fornitori simulati |
| UC 0061-0063, non implementati | Gli strumenti conversazionali non sono coperti: lacuna dichiarata, non nascosta |

## 7. Fuori ambito

- La prova contro un ambiente di prova **reale** dell'autorità fiscale: esclusa, come dichiarato nella storia
  `0005`. Richiede credenziali e un canale accreditato ed è una decisione contrattuale.
- La prova del livello conversazionale: non c'è il server.
- Le prove di carico e di prestazione: fuori ambito di questa applicazione.

## 8. Punti aperti

- **Se il percorso debba coprire anche la ricezione dei documenti passivi.** Sarebbe utile ma allunga molto il
  percorso principale; la proposta è un quarto percorso separato, più corto. Da decidere quando si scrive.
- **Quanto a lungo un percorso end-to-end resta manutenibile** man mano che l'app cresce. È il rischio noto di
  ogni percorso completo: la mitigazione è tenere i percorsi **pochi e distinti per scopo**, non uno lungo che
  copre tutto.
