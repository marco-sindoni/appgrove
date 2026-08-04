# 0005 — Avvio locale e dati di prova

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su BookGrove
> voglio avviare lo stack in locale e trovarci dentro un'attività finta già configurata
> così da poter provare una funzione in trenta secondi invece di passare mezz'ora a creare servizi e orari.

**Contesto.** Un'app di agenda è inutilizzabile a mano su un database vuoto: prima di vedere una schermata
sensata servono un servizio, una risorsa, degli orari e qualche prenotazione sparsa nella settimana. Se ognuno se
li crea a modo suo, ogni sviluppatore prova su dati diversi e i difetti si riproducono male. Questa storia mette
un salone finto — inventato, mai realistico al punto da sembrare dati veri di un cliente — a disposizione di tutti.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `prenotazioni` con porta `8107` e schema `app_prenotazioni`, e
   `./app-start.sh` la avvia senza modifiche manuali agli script.
2. **RF-2** — Esiste un comando dei dati di prova che popola un account locale con un'attività finta: due
   risorse, quattro servizi di durata diversa, orari settimanali, una chiusura e una manciata di prenotazioni
   distribuite fra passato e futuro.
3. **RF-3** — I dati di prova sono **deterministici**: due esecuzioni producono lo stesso risultato, e le date
   sono relative a oggi (non fisse) così che l'agenda abbia sempre senso.
4. **RF-4** — I dati di prova sono palesemente inventati: nomi di fantasia, indirizzi di posta su dominio
   `*.test`, numeri di telefono non assegnabili.
5. **RF-5** — Esiste anche il caso opposto: un account locale **vuoto**, per provare gli stati di primo avvio.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale (§15).** La scoperta automatica dei servizi deriva tutto dal solo
  `application.properties`; le rotte `/api/prenotazioni/v1/*` compaiono da sole nel proxy locale. Se viene voglia
  di modificare a mano uno script di avvio, è un difetto della scoperta, non un passo del lavoro.
- **RT-2 — Isolamento fra account (§1).** I dati di prova si creano dentro un `tenant_id` esplicito e non
  scavalcano il filtro: usano le stesse rotte dell'applicazione, non scritture dirette sul database.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova; i dati di prova non vivono in una migrazione, che è per
  la struttura, non per il contenuto.
- **RT-4 — Dati personali (§10).** I clienti finti sono dati personali per forma ma non per sostanza: la
  documentazione dice a chiare lettere che **non si usano mai dati veri**, nemmeno «presi da un cliente per fare
  una prova».
- **RT-5 — Prove (§11).** Il comando dei dati di prova è esercitato dalla prova di fumo locale: se si rompe, la
  suite lo dice.

## 4. Criteri di accettazione

**CA-1 — L'app parte da sola**
- **Dato** il monorepo appena clonato · **Quando** si esegue `./app-start.sh` · **Allora** `prenotazioni` è
  avviata sulla porta `8107` e risponde attraverso il proxy locale, senza che nessuno script sia stato toccato

**CA-2 — Il salone finto**
- **Dato** lo stack avviato · **Quando** si esegue il comando dei dati di prova · **Allora** entrando nell'app si
  vede un'agenda popolata della settimana corrente, con due risorse e prenotazioni in stati diversi

**CA-3 — Determinismo**
- **Dato** il comando eseguito due volte · **Quando** si confronta il risultato · **Allora** è lo stesso, senza
  duplicati

**CA-4 — Account vuoto**
- **Dato** il secondo account locale · **Quando** si apre l'app · **Allora** si vede lo stato di primo avvio, non
  una schermata rotta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `smoke`);
- [ ] prova di **integrazione** sul comando dei dati di prova;
- [ ] prova di **isolamento fra account**: i dati di prova finiscono solo nell'account indicato;
- [ ] **prova end-to-end**: *rimando* — i dati di prova sono il presupposto del percorso `[J-BOOKGROVE]` della
      storia `0033`, dove si aggiorna il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile, il comando non ha interfaccia;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: forma dei dati di prova e regola dei dati inventati;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] documentazione dello sviluppo aggiornata con il comando.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | servono le tabelle da popolare |
| storia `0003` | serve una schermata su cui vedere il risultato |

## 7. Fuori ambito

- i dati di prova delle epiche successive (clienti, promemoria, lista d'attesa): ogni epica estende il comando
  con ciò che introduce;
- l'ambiente di prova remoto: qui si parla solo di locale.

## 8. Punti aperti

Nessuno.
