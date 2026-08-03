# 0021 — Registro e sospensione degli avvisi

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 04 — Cruscotti e avvisi
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che riceve lo stesso avviso da tre settimane perché so già come stanno le cose
> voglio poterlo far tacere fino a fine mese senza cancellarlo
> così da non dovermi ricordare di ricrearlo, e da non prendere l'abitudine di ignorare le notifiche.

**Contesto.** Il modo più comune in cui un sistema di avvisi muore è l'assuefazione: arriva sempre lo stesso
messaggio, si smette di leggerlo, e il giorno in cui ne arriva uno importante finisce nel mucchio. Il rimedio non
è mandare meno avvisi: è dare all'utente un modo **legittimo** di zittirne uno a tempo, e insieme lasciargli
vedere la storia di ciò che è scattato — che è anche il modo per capire se la soglia era giusta.

## 2. Requisiti funzionali

1. **RF-1** — La sezione Avvisi mostra, per ogni avviso, la sua storia: quando è stato valutato, con che valore,
   se è scattato, se è stato recapitato o se non era valutabile e perché.
2. **RF-2** — Un avviso si può **sospendere fino a una data**: fino a quel momento viene valutato ma non
   recapitato, e la valutazione resta nel registro.
3. **RF-3** — Alla scadenza della sospensione l'avviso torna attivo da solo, e chi l'aveva sospeso riceve un
   messaggio che glielo dice.
4. **RF-4** — Il registro mostra, accanto a ogni scatto, il collegamento alla **scheda del numero** di quel
   momento: si può capire *perché* era scattato anche mesi dopo.
5. **RF-5** — Dal registro si vede **quante volte** un avviso è scattato negli ultimi tre mesi: se sono troppe,
   l'interfaccia lo dice e propone di rivedere la soglia.
6. **RF-6** — La conservazione del registro è di dodici mesi, coerente con le tracce del calcolo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura di `scatto_avviso` filtra per `tenant_id` preso dal
  gettone verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/insights/v1/avvisi/{id}/scatti` (paginata) e
  `POST /api/insights/v1/avvisi/{id}/sospensione`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** La storia dell'avviso è una cronologia dentro il dettaglio dell'avviso;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe esistono in `en, it, fr, es, de`, comprese le date relative.
- **RT-6 — Varchi e ruoli (§6).** Sospendere richiede lo stesso ruolo che serve a modificare l'avviso; leggere il
  registro segue la classe di riservatezza della metrica. **Nessun consumo di quota**.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova: il registro contiene numeri e momenti, non destinatari.
  Va verificato che l'esito del recapito **non** registri l'indirizzo, ma il solo riferimento al destinatario.
- **RT-14 — Registrazione eventi (§14).** «Avviso sospeso», «sospensione scaduta» con `tenant_id`, `app_id`,
  `user_id`, identificativo dell'avviso e data di fine.

## 4. Criteri di accettazione

**CA-1 — La storia si legge**
- **Dato** un avviso valutato ogni settimana da tre mesi
- **Quando** l'utente apre il dettaglio dell'avviso
- **Allora** vede l'elenco delle valutazioni con momento, valore, soglia ed esito, e per ciascuna il
  collegamento alla scheda del numero di allora

**CA-2 — Sospensione a tempo**
- **Dato** un avviso attivo che scatta ogni settimana
- **Quando** l'utente lo sospende fino al 30 settembre
- **Allora** fino a quella data l'avviso continua a essere valutato e registrato, ma non viene recapitato

**CA-3 — Ritorno automatico**
- **Dato** una sospensione scaduta
- **Quando** arriva la valutazione successiva
- **Allora** l'avviso torna attivo, chi l'aveva sospeso riceve un messaggio, e se la condizione è soddisfatta
  l'avviso scatta

**CA-4 — Troppi scatti**
- **Dato** un avviso scattato dodici volte negli ultimi tre mesi
- **Quando** si apre il dettaglio
- **Allora** l'interfaccia segnala che la soglia potrebbe essere troppo bassa e propone di rivederla

**CA-5 — Isolamento fra account**
- **Dato** due account con avvisi omonimi
- **Quando** un utente di `A` chiede il registro di un avviso di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scadenza della sospensione con tempo controllato, e di **integrazione** sulle
      risorse;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sul registro;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: l'esito del recapito non conserva indirizzi;
- [ ] **registro delle decisioni** compilato, con la sospensione a tempo invece della cancellazione e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0020` | il registro raccoglie ciò che la valutazione produce |
| storia `0016` | il collegamento alla scheda del numero di allora |

## 7. Fuori ambito

- il silenziamento «per sempre»: è la disattivazione, che esiste già come stato dell'avviso;
- statistiche aggregate sugli avvisi di tutti gli account: sarebbe uso secondario dei dati dei clienti, vietato
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10).

## 8. Punti aperti

Nessuno.
