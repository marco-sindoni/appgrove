# 0016 — Sospensione dei solleciti

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio essere certo che nessun sollecito parta verso chi ha già pagato, chi ha promesso di pagare o chi sta
> contestando
> così da poter lasciare acceso l'automatismo senza controllarlo ogni mattina.

**Contesto.** È la storia che decide se il prodotto verrà usato o spento. Un sollecito mandato a chi ha già pagato fa
più danno del ritardo che voleva risolvere: il cliente perde la fiducia nell'automatismo, lo disattiva e torna al foglio
di calcolo ([documento capofila](../application-description.md) §11). Le storie precedenti hanno costruito un motore che
manda; questa costruisce i freni, e lo fa in un punto solo — così che non ci sia mai un percorso di invio che li aggiri.

## 2. Requisiti funzionali

1. **RF-1** — Un credito non riceve solleciti automatici se il suo residuo è zero, se è `incassato`, `stralciato`,
   `sospeso` o `in_escalation`, o se è cancellato logicamente.
2. **RF-2** — L'utente può sospendere a mano i solleciti su un credito o su un intero debitore, indicando un motivo e,
   facoltativamente, una data fino alla quale la sospensione vale.
3. **RF-3** — Alla scadenza della sospensione i solleciti riprendono dal passo dovuto in quel momento, senza recuperare
   quelli saltati.
4. **RF-4** — La sospensione è visibile ovunque il credito compaia: elenco, scheda, coda degli invii, con il motivo.
5. **RF-5** — Il controllo di sospensione è eseguito **due volte**: quando l'invio viene messo in coda e di nuovo un
   istante prima della trasmissione — perché fra i due momenti può arrivare un pagamento.
6. **RF-6** — Un invio bloccato dal secondo controllo non sparisce: resta registrato come «annullato perché il credito
   non era più da sollecitare», con la ragione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La sospensione è per account: filtra per `tenant_id` preso dal token
  verificato e non può mai riguardare crediti o debitori di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/crediti/v1/crediti/{id}/sospensione`,
  `DELETE /api/crediti/v1/crediti/{id}/sospensione` e le corrispondenti a livello di debitore; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `sospensione` sullo schema `app_crediti` (ambito: credito o
  debitore; motivo; valida fino a; autore) con `tenant_id`, chiave UUID versione 7, colonne di controllo e
  cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Indicatore di sospensione con motivo nell'elenco, nella scheda e nella coda;
  azione di sospensione e di ripresa dalla scheda; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i motivi di sospensione, passano dallo
  spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Lo stato `sospeso` **resta monitorato**: non libera quota, perché il credito c'è
  ancora e prima o poi tornerà a essere sollecitabile. È una scelta consapevole e va detta all'utente.
- **RT-7 — Esposizione conversazionale (§12).** `sospendi_solleciti(credito, motivo, fino_a)` è dichiarato nella storia
  `0029` come scrittura con bozza e conferma. Qui si costruisce la funzione che quello strumento chiamerà, e si
  stabilisce che **anche** una chiamata dell'assistente passa dallo stesso punto di controllo.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo; la tabella `sospensione` è aggiunta a `exportData` e
  `purgeData` perché riferibile al debitore. Il campo motivo è a testo libero e porta l'avvertenza corrispondente.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «solleciti sospesi», «sospensione scaduta», «invio annullato al
  controllo finale» sono registrati con `tenant_id`, `app_id`, `user_id` (o «sistema»), motivo e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Pagamento fra la coda e l'invio**
- **Dato** un invio in coda per stasera e un incasso registrato questo pomeriggio che azzera il residuo
- **Quando** il motore arriva a trasmettere
- **Allora** l'invio è annullato, resta registrato come annullato con la ragione, e **nessun messaggio parte**

**CA-2 — Sospensione manuale con scadenza**
- **Dato** un credito scaduto · **Quando** l'utente sospende i solleciti per 15 giorni indicando «accordo verbale in
  corso» · **Allora** nessun invio viene programmato per 15 giorni, e la scheda mostra motivo e data di ripresa

**CA-3 — Ripresa senza recuperi**
- **Dato** una sospensione di 30 giorni durante i quali sarebbero maturati due passi
- **Quando** la sospensione scade
- **Allora** riprende **il passo dovuto oggi**, e i due saltati non vengono recuperati

**CA-4 — Sospensione a livello di debitore**
- **Dato** un debitore con cinque crediti scaduti · **Quando** si sospende il debitore · **Allora** nessuno dei cinque
  produce invii, e ciascuna scheda dichiara che la sospensione viene dal debitore

**CA-5 — La quota non cambia**
- **Dato** un credito sospeso · **Quando** si guarda il consumo della metrica · **Allora** il credito è ancora contato,
  e l'interfaccia lo spiega

**CA-6 — Isolamento fra account**
- **Dato** due account con debitori omonimi · **Quando** uno sospende il proprio debitore · **Allora** i solleciti
  dell'altro account proseguono senza alcuna interferenza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** su tutte le condizioni che bloccano un sollecito e di **integrazione** sul doppio controllo;
- [ ] prova di **isolamento fra account** sulla sospensione;
- [ ] **prova end-to-end**: *coprire ora* — «incassa e verifica che il sollecito non parta» è il passo che dà valore
      all'intero percorso `[J-CREDITI]`; si registra la voce nel registro di copertura con proprietaria la storia
      `0031`, che lo assembla;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `sospensione`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sul doppio controllo e sul fatto che i passi saltati non si
      recuperano;
- [ ] contratto degli **strumenti conversazionali**: la funzione è predisposta, il contratto si dichiara in `0029`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | Il primo controllo sta nella pianificazione |
| storia `0014` | Il secondo controllo sta immediatamente prima della trasmissione |

## 7. Fuori ambito

- La sospensione automatica per promessa di pagamento: storia `0018`, che introduce l'entità e chiama questa funzione.
- La sospensione automatica per contestazione: storia `0019`, per lo stesso motivo.

## 8. Punti aperti

Nessuno.
