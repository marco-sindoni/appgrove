# 0025 — Recapito e sospensione degli avvisi

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 05 — Budget, avvisi e anomalie
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che ha impostato tre budget
> voglio ricevere un avviso quando serve, una volta sola, e poter zittire quello che ho già capito
> così da continuare a leggere gli avvisi invece di imparare a ignorarli.

**Contesto.** Un sistema di avvisi si rovina in un modo solo: mandandone troppi. Dopo la terza notifica dello
stesso sforamento nella stessa mattina, chi la riceve crea una regola di posta che li archivia tutti, e da quel
momento l'app ha perso la sua funzione principale senza che nessuno se ne accorga. La contromisura è duplice: **un
avviso per soglia superata** — non uno per ogni valutazione — e una sospensione esplicita e tracciata, perché
zittire un avviso significa nascondere uno sforamento che continua a esistere.

## 2. Requisiti funzionali

1. **RF-1** — Quando una soglia viene superata nasce **un** avviso; finché la situazione non rientra, la stessa
   soglia non ne genera altri.
2. **RF-2** — L'avviso è recapitato nell'app e per posta elettronica ai destinatari del budget, e contiene le
   quattro cose che servono: quale budget, quanto si è consumato, la previsione, e il collegamento alla scomposizione
   di dove sta andando la spesa.
3. **RF-3** — Un avviso si può **sospendere** fino a una data, con un motivo scritto obbligatorio. La sospensione
   è visibile nella scheda del budget e nel registro: non è un modo per far sparire il problema, ma per dire «lo so
   già».
4. **RF-4** — Il registro degli avvisi mostra tutti gli avvisi del periodo con stato, istante, destinatari,
   esito del recapito ed eventuale sospensione con il suo motivo.
5. **RF-5** — Se il recapito per posta fallisce, l'avviso resta comunque nell'app e il fallimento è visibile: un
   avviso perso in silenzio è peggio di nessun avviso.
6. **RF-6** — Un tetto giornaliero al numero di avvisi per account protegge dal caso patologico (per esempio
   cinquanta budget che sforano insieme per un difetto): oltre il tetto si manda un unico riepilogo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Avvisi e registri sono per `tenant_id` preso dal gettone verificato;
  nessun destinatario esterno all'account può essere aggiunto.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `avviso` con `tenant_id`,
  budget, soglia, istante, valore osservato, previsione, stato, destinatari, esito del recapito, sospensione con
  motivo e scadenza, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET /api/spesa_modelli/v1/avvisi` e
  `POST /api/spesa_modelli/v1/avvisi/{id}/sospensione`; motivo obbligatorio; errori in `problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-4 — Recapito.** Si usa il fornitore di posta già in uso dalla piattaforma: **nessun fornitore esterno
  nuovo**. Il recapito è idempotente rispetto all'avviso, così che un nuovo tentativo non produca una seconda
  posta.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Budget», scheda «Avvisi»; la sospensione richiede il motivo nella
  stessa finestra in cui si conferma. Solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Il testo dell'avviso, che è il messaggio più letto dell'app, è presente in
  `en, it, fr, es, de`; la lingua del recapito è quella dell'utente destinatario, non quella di chi ha creato il
  budget.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `sospendi_avvisi(budget, fino_a, motivo) → bozza`,
  marcato **scrittura con conferma** (storia `0033`): zittire un avviso è un'azione che può nascondere uno
  sfondamento vero e non si esegue senza che una persona la confermi.
- **RT-8 — Dati personali (§10).** I destinatari e i loro indirizzi sono già dichiarati (storia `0023`); qui si
  aggiunge la tabella `avviso` a `exportData` e `purgeData` perché conserva i destinatari di ogni recapito.
- **RT-9 — Registrazione eventi (§14).** Eventi «avviso emesso», «recapito fallito», «avviso sospeso» con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza gli indirizzi di posta**.

## 4. Criteri di accettazione

**CA-1 — Un avviso per soglia**
- **Dato** un budget la cui soglia dell'80% è stata superata
- **Quando** la valutazione gira altre dieci volte nella stessa giornata con la soglia ancora superata
- **Allora** esiste **un solo** avviso e una sola posta recapitata

**CA-2 — Contenuto dell'avviso**
- **Dato** un avviso emesso
- **Quando** lo si legge nell'app o nella posta
- **Allora** contiene budget, consumato, previsione e il collegamento alla scomposizione della spesa

**CA-3 — Sospensione con motivo**
- **Dato** un avviso attivo
- **Quando** si tenta di sospenderlo senza motivo
- **Allora** l'operazione è respinta; con il motivo, la sospensione è registrata con chi, quando, fino a quando e
  perché

**CA-4 — Recapito fallito**
- **Dato** un indirizzo di posta non raggiungibile
- **Quando** l'avviso viene recapitato
- **Allora** l'avviso resta visibile nell'app e il fallimento del recapito è mostrato nel registro

**CA-5 — Tetto giornaliero**
- **Dato** un account in cui trenta budget superano la soglia nello stesso momento e un tetto di dieci avvisi al
  giorno
- **Quando** la valutazione gira
- **Allora** vengono recapitati i primi dieci e un unico riepilogo per i restanti

**CA-6 — Isolamento fra account**
- **Dato** due account con budget omonimi
- **Quando** entrambi superano la soglia
- **Allora** ciascun avviso va solo ai destinatari del proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla non ripetizione dell'avviso e sul tetto giornaliero, e di **integrazione** sul
      recapito con fornitore di posta simulato;
- [ ] prova di **isolamento fra account** sui destinatari;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «soglia superata, arriva
      un avviso e uno solo», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con il recapito nella lingua del destinatario;
- [ ] **manifesto dei dati** aggiornato: `avviso` in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sul motivo obbligatorio della sospensione;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `sospendi_avvisi`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0024` | L'avviso preventivo si basa sulla previsione |
| Fornitore di posta di piattaforma | Il recapito non introduce fornitori nuovi |

## 7. Fuori ambito

- il recapito su messaggistica di squadra: rimandato, perché introdurrebbe un fornitore esterno nuovo con le
  relative conseguenze sui dati (§2.4 del documento capofila). Se emergesse come bisogno, sarebbe una storia
  propria e passerebbe dalla valutazione del fornitore;
- gli avvisi su anomalie non legate a un budget: sono la storia `0026`.

## 8. Punti aperti

Nessuno.
