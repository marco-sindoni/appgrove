# 0031 — Strumenti di lettura

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0031` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0013`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha il telefono in mano e una persona davanti
> voglio poter chiedere a parole «quando posso mettere la signora Bianchi per un colore, giovedì o venerdì?»
> così da rispondere in tre secondi invece di aprire il programma e incrociare a mano.

**Contesto.** Il catalogo pone a tutte le sessanta applicazioni un requisito trasversale: ogni funzione dev'essere
comandabile da una chat. Nel repository il livello conversazionale **non esiste ancora** — è l'epica
`12-ready-for-ai-mcp` (UC 0061-0066), scritta e non implementata. Quello che questa storia fa è dichiarare il
**contratto** degli strumenti di lettura e tenerlo dentro il servizio dell'app, versionato con essa. Per
BookGrove è la parte del catalogo che dà il guadagno più evidente: la domanda sulla disponibilità è la più
frequente della giornata, è di sola lettura, e quindi non ha bisogno di nessuna conferma.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati quattro strumenti di lettura: `verifica_disponibilita`, `elenca_prenotazioni`,
   `cerca_cliente`, `riepilogo_mancate_presentazioni`.
2. **RF-2** — Ogni strumento porta nome stabile, descrizione in lingua naturale, schema dei parametri, schema del
   risultato, marcatura **lettura** e dichiarazione di idempotenza.
3. **RF-3** — Le risposte sono **minimizzate**: `elenca_prenotazioni` restituisce ora, servizio, risorsa e stato,
   e il nome del cliente solo se richiesto esplicitamente; i contatti mai, se non con una richiesta apposita su
   un singolo cliente.
4. **RF-4** — Gli strumenti rispettano la stessa catena dei varchi delle interfacce: chi non è abilitato o non ha
   il ruolo non ottiene i dati per il fatto di chiederli a parole.
5. **RF-5** — Il contratto è versionato insieme al servizio e cambia con le regole di compatibilità dichiarate:
   un nome di strumento non si riusa per un'altra cosa.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Gli strumenti sono dichiarati **dentro il servizio dell'app**; il
  server conversazionale è di piattaforma e non è compito di questa storia. Dipendenza dichiarata: UC 0061-0063
  (architettura, autenticazione delegata, mappatura operazioni → strumenti), **non ancora implementati**.
- **RT-2 — Isolamento fra account (§1).** Ogni strumento riceve il contesto dell'account dal livello di
  piattaforma, con la stessa origine di un token verificato: **mai** un `tenant_id` fra i parametri dello
  strumento. È il punto in cui un'esposizione conversazionale fatta male romperebbe l'invariante numero uno.
- **RT-3 — Interfaccia di programmazione (§2).** Gli strumenti riusano le rotte esistenti: nessuna logica
  duplicata, altrimenti le due strade divergono. Errori mappati in messaggi comprensibili a un assistente, non
  codici tecnici.
- **RT-4 — Cinque lingue (§4).** Descrizioni e messaggi degli strumenti disponibili in `en, it, fr, es, de`.
- **RT-5 — Dati personali (§10).** La minimizzazione è un requisito, non una raccomandazione: un assistente che
  riceve un elenco di clienti con i contatti li porta fuori dal perimetro senza che nessuno se ne accorga.
  L'esposizione conversazionale va dichiarata nel manifesto come **canale** attraverso cui i dati escono.
- **RT-6 — Registrazione eventi (§14).** Ogni chiamata di strumento è registrata con `tenant_id`, `app_id`,
  `user_id`, nome dello strumento e correlazione — mai i parametri, che possono contenere un nome.

## 4. Criteri di accettazione

**CA-1 — Disponibilità a parole**
- **Dato** il contratto degli strumenti · **Quando** si invoca `verifica_disponibilita` per un servizio e due
  giorni · **Allora** si ottengono gli stessi intervalli che darebbe l'interfaccia, e nient'altro

**CA-2 — Minimizzazione**
- **Dato** `elenca_prenotazioni` sulla giornata · **Quando** si guarda il risultato · **Allora** non contiene
  contatti dei clienti

**CA-3 — Nessun account nei parametri**
- **Dato** lo schema dei parametri di ogni strumento · **Quando** lo si esamina · **Allora** non esiste nessun
  parametro che indichi l'account

**CA-4 — Varchi rispettati**
- **Dato** un utente senza il ruolo necessario · **Quando** invoca uno strumento · **Allora** riceve lo stesso
  rifiuto che riceverebbe dall'interfaccia

**CA-5 — Contratto stabile**
- **Dato** il contratto versionato · **Quando** si aggiunge uno strumento · **Allora** i nomi esistenti non
  cambiano significato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`);
- [ ] prove di **unità** sugli schemi e di **integrazione** sul riuso delle rotte esistenti;
- [ ] prova di **isolamento fra account** sull'invocazione degli strumenti;
- [ ] **prova end-to-end**: *rimando* — il livello conversazionale non esiste ancora (UC 0061-0063); motivo e
      storia proprietaria dichiarati in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** delle descrizioni in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con l'esposizione conversazionale come canale di uscita dei dati;
- [ ] **registro delle decisioni** compilato: elenco degli strumenti di lettura e regole di minimizzazione;
- [ ] contratto degli **strumenti conversazionali** dichiarato e versionato dentro il servizio;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0010`, `0013`, `0026` | sono le funzioni che gli strumenti espongono |
| UC 0061-0063 (livello conversazionale di piattaforma) | non ancora implementati: qui si dichiara il contratto e ci si ferma |

## 7. Fuori ambito

- gli strumenti di scrittura: storia `0032`;
- la costruzione del server conversazionale: è di piattaforma.

## 8. Punti aperti

**Fin dove arriva la lettura sui dati dei clienti finali.** Un assistente che può cercare un cliente per nome è
comodissimo e allo stesso tempo è un canale attraverso cui l'anagrafica esce. La proposta è: ricerca sì, elenco
completo no, contatti solo su richiesta esplicita di un singolo cliente. Da confermare quando il livello
conversazionale esisterà davvero, insieme alle sue regole di consenso delegato.
