# 0037 — Percorso end-to-end e registro di copertura

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0037` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`, `0020`, `0021`, `0024`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio un percorso automatico che, dall'inizio alla fine, colleghi una sorgente, faccia dichiarare un'azione
> rischiosa, la faccia approvare da una persona e verifichi l'integrità della catena
> così da sapere che la promessa centrale del prodotto continua a funzionare dopo ogni modifica, invece di
> scoprirlo da un cliente.

**Contesto.** Il registro di copertura end-to-end
([docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)) è sorvegliato da un controllo
automatico: registro incoerente significa suite rossa. Questa storia crea il percorso `[J-AGENTAUDIT]` e chiude
le voci lasciate aperte dalle storie precedenti. Per un'app che vende inalterabilità, il percorso end-to-end ha
un valore particolare: **la verifica di integrità in coda al percorso è la prova automatica che la promessa
regge**, e va eseguita a ogni giro, non a campione.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-AGENTAUDIT.spec.ts`, eseguito senza finestra sullo
   stack locale reale, con accesso programmatico e dati di prova inventati.
2. **RF-2** — Il percorso copre, in quest'ordine: attivazione dell'app, registrazione di una sorgente,
   dichiarazione di un'azione innocua, dichiarazione di un'azione che richiede nulla osta, approvazione da parte
   di una persona, dichiarazione dell'esito, lettura della cronologia, apertura della scheda dell'azione,
   verifica dell'integrità della catena.
3. **RF-3** — Il percorso verifica anche **due comportamenti negativi**, che valgono quanto quelli positivi: una
   richiesta di nulla osta lasciata scadere non concede nulla, e un'azione dichiarata senza il nulla osta
   necessario compare come scostamento.
4. **RF-4** — Ogni test del percorso porta l'etichetta `[J-AGENTAUDIT]` in testa al titolo, come prescritto dalla
   convenzione del registro di copertura.
5. **RF-5** — Il registro di copertura è aggiornato con le voci dell'app: le storie coperte ora, e quelle
   `da-coprire` con motivo e storia o epica proprietaria — in particolare tutta la parte conversazionale, che
   resta di proprietà dell'epica di piattaforma 12.
6. **RF-6** — Un controllo di accessibilità automatico gira sulle schermate principali del modulo (cronologia,
   scheda dell'azione, coda delle approvazioni, integrità).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso esegue almeno un passaggio con due account, verificando che
  la cronologia di uno non mostri mai righe dell'altro e che la verifica di integrità sia per account.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: il percorso usa quelle esistenti e la
  definizione OpenAPI come contratto.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. I dati di prova sono deterministici e **inventati**: nomi di
  fantasia, indirizzi nel dominio riservato alle prove, nessun dato riconducibile a una persona reale.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova: il percorso attraversa quelle esistenti in tema
  chiaro e verifica che il tema scuro non rompa le schermate principali.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; un controllo separato verifica che
  nessuna chiave di traduzione risulti mancante nelle altre quattro.
- **RT-6 — Varchi e quota (§6, §7).** Il percorso include un passaggio a quota esaurita: l'ingresso di una nuova
  azione riceve `429` con l'indicazione del rimedio, e il rifiuto risulta contato nel registro.
- **RT-7 — Esposizione conversazionale (§12).** Il percorso **non** copre il canale conversazionale, che non
  esiste: la mancanza è dichiarata come voce `da-coprire` con proprietaria l'epica 12, non lasciata implicita.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo. Il percorso verifica un requisito di conformità concreto:
  che nella cronologia e nella scheda **non compaia mai** il contenuto di un parametro quando la conservazione dei
  contenuti non è attiva.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo; il percorso verifica che l'esecuzione non produca
  righe di registro tecnico contenenti dati personali.

## 4. Criteri di accettazione

**CA-1 — Il percorso completo passa**
- **Dato** lo stack locale avviato e un account nuovo
- **Quando** si esegue il percorso `[J-AGENTAUDIT]`
- **Allora** tutti i passaggi risultano verdi e la verifica finale dell'integrità della catena risponde «integra»

**CA-2 — Il percorso rileva una manomissione**
- **Dato** il percorso in una variante che altera una riga del registro direttamente sulla base di dati
- **Quando** si esegue la verifica finale
- **Allora** il percorso fallisce indicando la prima riga divergente: è il collaudo che dimostra che la verifica
  non è decorativa

**CA-3 — La mancata approvazione non concede nulla**
- **Dato** una richiesta di nulla osta con scadenza breve, lasciata senza risposta
- **Quando** la scadenza passa
- **Allora** l'esito è «non concesso», il fatto compare nel registro e l'azione dichiarata dopo risulta come
  scostamento

**CA-4 — Isolamento fra account**
- **Dato** due account con azioni proprie
- **Quando** il percorso legge la cronologia di ciascuno
- **Allora** nessuna riga dell'uno compare nell'altro, anche forzando l'identificativo nella richiesta

**CA-5 — Il registro di copertura è coerente**
- **Dato** il registro aggiornato da questa storia
- **Quando** si esegue il controllo dell'area degli strumenti
- **Allora** il controllo è verde: ogni voce punta a un test che esiste, e ogni voce `da-coprire` dichiara motivo
  e proprietaria

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (l'intera suite, non solo le aree toccate: questa storia tocca il
      controllo che sorveglia il registro di copertura);
- [ ] prove di **unità** e di **integrazione**: nessuna nuova, e il fatto è dichiarato — questa storia costruisce
      il livello end-to-end;
- [ ] prova di **isolamento fra account** dentro il percorso;
- [ ] **prova end-to-end**: risposta «coprire ora» — è questa la storia che crea il percorso `[J-AGENTAUDIT]` e
      aggiorna [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), chiudendo le voci
      lasciate `da-coprire` dalle storie precedenti che ora sono coperte;
- [ ] **traduzioni**: controllo che nessuna chiave manchi nelle cinque lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il percorso verifica che i contenuti non compaiano;
- [ ] **registro delle decisioni** compilato, con l'elenco di ciò che il percorso copre e di ciò che resta
      scoperto e perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] `run-tests.sh` esegue il nuovo percorso come parte dell'area di piattaforma.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0006`, `0008` | Servono una sorgente e l'ingresso delle azioni |
| storie `0020`, `0021` | Il percorso attraversa la richiesta di nulla osta e la decisione di una persona |
| storia `0024` | Il percorso legge la cronologia dall'interfaccia |
| storia `0014` | La verifica di integrità è il passaggio finale, ed è il senso del percorso |

## 7. Fuori ambito

- il canale conversazionale, che non esiste: dichiarato come voce `da-coprire`;
- le prove di carico e di volume: non sono end-to-end e non appartengono a questa storia;
- il livello di prova sull'ambiente reale del fornitore di pagamento, che resta pre-rilascio come per ogni app.

## 8. Punti aperti

- **Quanto storico usare nel percorso.** Un percorso con poche righe non prova nulla sulle prestazioni della
  verifica; uno con molte righe rende il percorso lento. Propongo un percorso breve più una prova separata di
  verifica su un volume grande, che non gira a ogni giro. Da confermare con chi presidia il collaudo.
- **Come si simula la manomissione** (CA-2) senza lasciare nel repository uno strumento capace di scrivere sulla
  tabella del registro. Va risolto in fase di implementazione, e la soluzione non deve esistere fuori dal
  contesto di prova.
