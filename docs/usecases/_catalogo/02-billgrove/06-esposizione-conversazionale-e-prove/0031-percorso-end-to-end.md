# 0031 — Percorso end-to-end dell'app

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0011`, `0012`, `0016`, `0017`, `0021`, `0025`, `0026`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che deve poter cambiare BillGrove senza paura
> voglio un percorso automatico che ripercorra la giornata tipo del cliente, dal preventivo all'incasso
> così da sapere, a ogni modifica, che la catena che vende il prodotto continua a funzionare davvero e non solo
> nelle prove di unità.

**Contesto.** È l'ultima storia dell'app e chiude il cerchio. Il registro di copertura end-to-end è sorvegliato da
un controllo automatico: registro incoerente uguale suite rossa. Le storie precedenti hanno risposto alla domanda di
copertura in tre modi — *coprire ora*, *rimandare*, *nessun impatto* — e questa storia è la **proprietaria** di
tutti i rimandi dichiarati: qui si verifica che ognuno sia ancora giustificato, e si costruisce il percorso che
copre ciò che va coperto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-BILLING.spec.ts`, eseguito senza finestra sullo
   stack locale reale.
2. **RF-2** — Il percorso ripercorre la catena: accesso → l'app compare → crea cliente → crea preventivo → invia →
   accetta → converti in fattura → emetti → stampa → invia con conferma → registra incasso → verifica il riepilogo.
3. **RF-3** — Il percorso verifica anche i due comportamenti di protezione: il blocco a quota esaurita e
   l'impossibilità di cancellare un documento emesso.
4. **RF-4** — Ogni test porta l'etichetta del percorso in testa al titolo: `test('[J-BILLING] …')`.
5. **RF-5** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riporta la mappa
   storia → percorso → test per **tutte** le storie di BillGrove, comprese quelle esenti e quelle rimandate, con
   motivo e proprietaria.
6. **RF-6** — Il percorso non usa attese a tempo, accede in modo programmatico e parte da dati inventati
   deterministici (storia `0005`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso include un passo che verifica, dall'interfaccia, che un
  account non veda i documenti dell'altro: è la verifica dell'invariante numero uno sul prodotto assemblato, non
  sulle singole rotte.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova.
- **RT-4 — Modulo frontend (§3, §5).** Il percorso attraversa le schermate reali del modulo; comprende il controllo
  automatico di accessibilità sulle schermate principali e la verifica del tema scuro su almeno una schermata.
- **RT-5 — Cinque lingue (§4).** Il percorso gira in una lingua sola; la copertura delle cinque è delle prove di
  frontend. Va dichiarato, per non lasciare l'impressione che sia una dimenticanza.
- **RT-6 — Varchi e quota (§6).** Il percorso verifica il `429` a quota esaurita e il `402` con abbonamento non
  attivo, usando il fornitore di pagamento **simulato**: è **vietato** guidare con l'automazione la finestra di un
  fornitore di pagamento vero.
- **RT-7 — Esposizione conversazionale (§12).** Il percorso **non** copre gli strumenti conversazionali, perché il
  livello non esiste (UC 0061-0066). Il registro di copertura lo dichiara come rimando con motivo e proprietario, e
  le storie `0028`-`0030` restano coperte da prove di integrazione.
- **RT-8 — Dati personali (§10).** I dati del percorso sono **inventati**: nomi di fantasia e indirizzi di posta su
  domini `*.test`. Mai dati veri, nemmeno «realistici».
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — La catena completa passa**
- **Dato** lo stack locale con i dati di prova
- **Quando** si esegue il percorso `[J-BILLING]`
- **Allora** tutti i passi da preventivo a incasso passano, e il riepilogo finale mostra i valori attesi

**CA-2 — Quota esaurita**
- **Dato** un account portato al tetto della metrica `documenti`
- **Quando** il percorso tenta un'emissione
- **Allora** l'interfaccia mostra il blocco con il rimedio, e nessun documento risulta emesso

**CA-3 — Documento emesso non cancellabile**
- **Dato** una fattura emessa dal percorso
- **Quando** si tenta di cancellarla dall'interfaccia
- **Allora** l'operazione è impedita con la spiegazione

**CA-4 — Isolamento fra account dal prodotto assemblato**
- **Dato** due account con dati propri
- **Quando** il percorso accede con l'uno e poi con l'altro
- **Allora** nessun documento dell'uno compare all'altro

**CA-5 — Registro coerente**
- **Dato** una storia di BillGrove non presente nel registro di copertura
- **Quando** si esegue l'area `tooling` di `./run-tests.sh`
- **Allora** la suite è rossa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**: è la storia che non si accontenta delle aree toccate;
- [ ] il percorso `[J-BILLING]` gira senza finestra, senza attese a tempo, con dati deterministici;
- [ ] prova di **isolamento fra account** presente nel percorso;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) completo
      per tutte e 31 le storie dell'app, con i rimandi dichiarati e motivati;
- [ ] **traduzioni**: nessuna nuova; dichiarato che il percorso gira in una lingua sola;
- [ ] **manifesto dei dati**: nessuna modifica; dichiarato che i dati del percorso sono inventati;
- [ ] **registro delle decisioni** compilato, con l'elenco dei rimandi confermati e di quelli chiusi;
- [ ] contratto degli **strumenti conversazionali**: invariato; il rimando sulla copertura è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] `run-tests.sh` aggiornato se l'area `platform` cambia comando.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| tutte le storie che il percorso attraversa (`0010`-`0012`, `0016`, `0017`, `0021`, `0025`, `0026`, `0027`) | Il percorso ripercorre ciò che esse costruiscono |
| storia `0005` | I dati di prova deterministici sono il punto di partenza |

## 7. Fuori ambito

- il percorso sugli strumenti conversazionali: rimandato alle use case di piattaforma 0061-0066;
- le prove di carico: non previste in questa stesura;
- la suite di livello 3 sul fornitore di pagamento reale: è pre-rilascio e resta fuori dal cancello.

## 8. Punti aperti

Nessuno.
