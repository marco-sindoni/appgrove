# 0005 — Avvio locale e dati di prova

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su RepGrove
> voglio avviare l'app in locale con un comando e trovarla già piena di sedi, servizi erogati e recensioni finte
> così da poter lavorare sulle funzioni senza passare mezz'ora a costruirmi uno stato di partenza.

**Contesto.** RepGrove ha un problema che le altre app non hanno: **da sola non ha niente da mostrare**. Le
recensioni arrivano da piattaforme esterne a cui in locale non ci si collega, e i servizi erogati arrivano da
altre app della suite. Senza dati di prova la prima schermata è vuota per settimane, e nessuno si accorge degli
errori di visualizzazione. Questa storia risolve il problema alla radice e vale doppio anche per un'altra
ragione: **durante la prova gratuita di 14 giorni** un'attività piccola raccoglie pochissime recensioni vere
(descrizione §5), quindi il modo in cui l'app si presenta da vuota è materia di prodotto, non solo di sviluppo.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `recensioni` con la porta `8117` e lo schema `app_recensioni`, ricavati
   dal solo `services/recensioni/src/main/resources/application.properties`; `./app-start.sh` la avvia senza
   modifiche manuali agli script.
2. **RF-2** — Esiste un comando di popolamento (`dev seed recensioni` o equivalente della convenzione del
   repository) che riempie un account dimostrativo con: due sedi, una regola di equità, una ventina di servizi
   erogati distribuiti nelle ultime sei settimane, una quindicina di recensioni con voti distribuiti (comprese
   due a una stella e una senza testo), tre risposte già pubblicate e due bozze.
3. **RF-3** — I dati di prova sono **inventati e riconoscibili come tali**: nomi di fantasia, indirizzi di posta
   elettronica sul dominio riservato alle prove, nessun nome di persona o azienda reale, nessun testo copiato da
   una recensione vera.
4. **RF-4** — In locale **nessun collegamento verso le piattaforme è reale**: le recensioni di prova entrano da un
   finto collegamento che si comporta come quello vero (stessi stati, stessi errori possibili, stesso ritmo di
   sincronizzazione), così che le storie successive si possano provare senza credenziali.
5. **RF-5** — Il popolamento è **idempotente**: eseguirlo due volte non crea doppioni e non moltiplica le
   recensioni.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il popolamento crea dati per **due** account, non uno: serve a rendere
  visibile a occhio nudo un eventuale difetto di isolamento durante lo sviluppo.
- **RT-2 — Interfaccia di programmazione (§2).** Il popolamento passa dalle rotte del servizio dove esistono, non
  da inserimenti diretti nel database: se una rotta rifiuta un dato, il popolamento deve accorgersene.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova; le schermate esistenti smettono di essere vuote.
- **RT-5 — Cinque lingue (§4).** I dati di prova comprendono almeno una recensione in una lingua diversa
  dall'italiano, così da vedere subito come si comporta l'elenco con testi in lingue diverse.
- **RT-6 — Varchi e quota (§6, §7).** Uno dei due account dimostrativi nasce **al tetto della quota**, così che lo
  stato «quota esaurita» sia visibile senza doverlo costruire ogni volta.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento.
- **RT-8 — Dati personali (§10).** I dati di prova sono inventati: **nessun dato personale reale**, mai, nemmeno
  il proprio indirizzo di posta. Vale anche per le prove automatiche.
- **RT-9 — Registrazione eventi (§14).** Il popolamento registra quante righe ha creato per tabella, senza
  contenuti.

## 4. Criteri di accettazione

**CA-1 — L'app si avvia dalla sola scoperta automatica**
- **Dato** un ambiente locale pulito
- **Quando** si eseguono `./dev.sh services` e `./app-start.sh`
- **Allora** `recensioni` compare con porta e schema corretti e risponde, senza che nessuno script sia stato
  modificato a mano

**CA-2 — Il popolamento riempie l'app**
- **Dato** un database locale con le migrazioni applicate
- **Quando** si esegue il comando di popolamento
- **Allora** la *Panoramica* dell'account dimostrativo mostra un punteggio, un elenco di recensioni e almeno una
  recensione negativa da prendere in carico

**CA-3 — Il popolamento non si somma a sé stesso**
- **Dato** un popolamento già eseguito
- **Quando** lo si esegue una seconda volta
- **Allora** i conteggi non cambiano

**CA-4 — Due account, isolamento visibile**
- **Dato** il popolamento eseguito
- **Quando** si entra con l'utente del secondo account
- **Allora** si vedono solo le sedi e le recensioni del secondo account

**CA-5 — Nessuna rete verso le piattaforme**
- **Dato** l'ambiente locale senza connessione
- **Quando** si esegue il popolamento e si naviga l'app
- **Allora** tutto funziona: nessuna chiamata verso Google o Trustpilot parte in locale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prova di **unità** sull'idempotenza del popolamento;
- [ ] prova di **isolamento fra account**: i due account dimostrativi non si vedono a vicenda;
- [ ] **prova end-to-end**: *rimando* alla storia 0030, che userà proprio questi dati come stato di partenza;
- [ ] **traduzioni**: nessuna stringa nuova, ma i dati comprendono almeno una lingua straniera;
- [ ] **manifesto dei dati**: nessuna voce nuova; i dati di prova sono inventati;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] la documentazione dello sviluppo locale cita il comando di popolamento.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0001`-`0004` | il popolamento passa dalle rotte, che devono esistere e rispettare la quota |

## 7. Fuori ambito

- il collegamento vero alle piattaforme — storie 0007 e 0008;
- l'invio vero dei messaggi di invito — storia 0014.

## 8. Punti aperti

- **Quanto deve assomigliare il finto collegamento a quello vero.** Più gli assomiglia, più le storie successive
  si provano senza credenziali; ma un finto collegamento troppo gentile nasconde i modi in cui quello vero
  fallisce (delega scaduta, quota della piattaforma esaurita, sede non trovata). L'inclinazione è modellare
  **esplicitamente anche i guasti**, e va verificato con lo sviluppatore quanto vale la pena spingersi.
</content>
