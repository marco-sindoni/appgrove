# 0032 — Cancellazione tramite distruzione della chiave

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 06 — Dati delle persone e diritti
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0030`, `0031`
**Ultimo aggiornamento**: 2026-08-03

> 🛑 **Avviso in testa — il presupposto giuridico di questa storia non è verificato.** L'intera soluzione poggia
> su un'affermazione che ho trovato in **fonti secondarie e di parte** e che **non ho potuto verificare su
> documenti primari** (§2.7 della [descrizione dell'applicazione](../application-description.md)): che la
> distruzione irreversibile della chiave di cifratura valga come **cancellazione** ai sensi della normativa sui
> dati personali. Le fonti citano linee guida del Comitato europeo per la protezione dei dati e delle autorità
> britannica e francese; **quei documenti non li ho letti**. Se il presupposto non regge, questa storia non regge,
> e il conflitto fra prova e cancellazione torna aperto senza una risposta tecnica. **Escalation bloccante alla
> revisione legale**, prima di scrivere codice.

## 1. Narrazione

> Come titolare del trattamento che riceve da una persona una richiesta di cancellazione dei propri dati
> voglio poter rendere illeggibili per sempre i contenuti che la riguardano senza distruggere la catena di prova
> così da poter rispondere alla persona e conservare al tempo stesso ciò che ho il dovere di conservare.

**Contesto.** È il punto in cui i due doveri si scontrano davvero: la persona chiede che il suo dato sparisca; il
cliente ha bisogno che la prova di ciò che è successo resti. La storia 0030 ha separato gli strati proprio per
questo momento — la catena non dipende dalla leggibilità del contenuto — e questa storia usa quella separazione:
si distrugge la chiave, il testo cifrato diventa rumore, la catena resta intera e verificabile.

C'è una seconda cosa che la storia deve fare, e che è meno ovvia della prima: **dimostrare di aver cancellato**.
Una cancellazione senza prova mette il cliente nella posizione di dover essere creduto sulla parola, che è
esattamente la condizione da cui questa applicazione esiste per liberarlo.

## 2. Requisiti funzionali

1. **RF-1** — Su richiesta si possono rendere illeggibili per sempre i contenuti allegati di un **periodo**
   (l'ambito coperto da una chiave, storia 0030), distruggendo la chiave corrispondente.
2. **RF-2** — La distruzione è **irreversibile e completa**: dopo l'operazione nessuno — né il cliente, né chi
   amministra la piattaforma, né chi ha accesso al deposito o alle copie di sicurezza — può leggere quei contenuti.
3. **RF-3** — La cancellazione è **essa stessa un evento del registro**, con la formula «il contenuto delle azioni
   dal … al … è stato reso illeggibile su richiesta, il …», chi l'ha chiesta, chi l'ha eseguita e il riferimento
   alla richiesta — **senza conservare ciò che si è cancellato**.
4. **RF-4** — Dopo la cancellazione, la verifica di integrità (storia 0014) dell'intervallo continua a rispondere
   «integra» e dichiara quanti contenuti non sono più leggibili.
5. **RF-5** — Le azioni interessate restano nella cronologia e nelle schede, con l'indicazione «contenuto reso
   illeggibile il …» al posto del comando di rivelazione.
6. **RF-6** — L'operazione richiede un ruolo alto e una conferma esplicita che dica, prima di procedere, quante
   azioni e quale intervallo saranno interessati e che l'operazione **non si può annullare**.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La distruzione agisce solo su chiavi dell'account, con `tenant_id`
  preso dal token verificato. Una richiesta che indicasse una chiave di un altro account risponde `404`. Un caso
  di prova verifica che la distruzione in `A` non renda illeggibile nulla in `B`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/agentaudit/v1/content-keys/{id}/destroy` con
  corpo validato (conferma esplicita, riferimento alla richiesta, motivo); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. L'operazione è **idempotente**: ripeterla su una chiave già
  distrutta risponde con lo stesso esito e non genera un secondo evento.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si aggiorna lo stato della chiave nella tabella introdotta
  dalla storia 0030 e si cancella il materiale crittografico. La riga di registro (RF-3) si accoda nella catena in
  sola aggiunta della storia 0002. **Punto critico**: la distruzione deve raggiungere anche le copie di sicurezza
  del materiale crittografico, altrimenti non è una distruzione — ed è il punto aperto principale della storia.
- **RT-4 — Modulo frontend (§3, §5).** Comando nella sezione dei contenuti e nella scheda dello strumento, con
  finestra di conferma che riporta i numeri e l'irreversibilità; solo token del sistema di design; tema chiaro e
  scuro. Il comando usa il colore funzionale d'allarme e **non** è raggiungibile per errore da un percorso comune.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`, compresi il testo della conferma e la formula «contenuto reso illeggibile il …» — che
  compare nelle schede e quindi anche nelle esportazioni destinate a chi legge in un'altra lingua.
- **RT-6 — Varchi e quota (§6, §7).** Richiede ruolo alto (`403` altrimenti). **Resta accessibile anche quando
  l'app è disabilitata o l'abbonamento è scaduto**: i diritti dell'interessato non dipendono dallo stato del
  contratto commerciale — è una regola di piattaforma e qui è particolarmente vera, perché una richiesta di
  cancellazione non aspetta il rinnovo. Consuma una unità della metrica `actions` per la riga di registro, e la
  consuma **anche a quota esaurita**, come previsto dalla storia 0004 per le righe che non si possono perdere.
- **RT-7 — Esposizione conversazionale (§12).** **Non viene esposta** come strumento a un assistente, in nessuna
  forma: è un effetto irreversibile su dati di terzi, ed è il genere di operazione che deve avere una persona
  identificata all'origine. Il divieto va scritto nel contratto degli strumenti (storia 0035).
- **RT-8 — Dati personali (§10).** È **l'operazione di esercizio del diritto** e va dichiarata nel manifesto
  `docs/compliance/manifests/agentaudit.yaml` in italiano e inglese come tale, con la propria finalità. La
  tabella delle chiavi resta presente in `exportData` e `purgeData` del contratto dati dell'app: dell'evento di
  cancellazione si esporta il fatto, non il contenuto.
- **RT-9 — Registrazione eventi (§14).** La distruzione è registrata con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione. Nel registro tecnico non finisce nulla della chiave né dei contenuti: si scrive
  che è avvenuta, non che cosa conteneva.

## 4. Criteri di accettazione

**CA-1 — Il contenuto sparisce, la prova resta**
- **Dato** un intervallo con 500 azioni, di cui 120 con contenuto conservato, verificato integro
- **Quando** si distrugge la chiave di quel periodo
- **Allora** nessuno dei 120 contenuti è più leggibile, le 500 azioni restano tutte in cronologia, e la verifica
  di integrità risponde «integra» dichiarando 120 contenuti non più leggibili

**CA-2 — La cancellazione si dimostra**
- **Dato** la distruzione appena eseguita
- **Quando** si guarda la cronologia
- **Allora** compare una riga che dice quale intervallo è stato reso illeggibile, su richiesta di chi, eseguita da
  chi e quando — e quella riga non contiene nulla di ciò che è stato cancellato

**CA-3 — Non si torna indietro**
- **Dato** una chiave distrutta
- **Quando** si tenta di leggere un contenuto che dipendeva da lei, da qualunque percorso compresa la console di
  amministrazione
- **Allora** si ottiene «contenuto reso illeggibile il …» e nessun dato

**CA-4 — I diritti non dipendono dall'abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** un amministratore chiede la distruzione di una chiave
- **Allora** l'operazione è consentita ed eseguita, mentre le altre funzioni dell'app rispondono `402`

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con contenuti nello stesso periodo
- **Quando** `A` distrugge la propria chiave
- **Allora** i contenuti di `B` restano leggibili e nessuna sua riga risulta modificata

## 5. Definizione di fatto

- [ ] **la revisione legale ha confermato che la distruzione della chiave soddisfa la richiesta di
      cancellazione** — voce di sbarramento: se la risposta è no, la storia va riprogettata, non implementata
      lo stesso;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'irreversibilità e sull'idempotenza, e di **integrazione** sul percorso completo
      conservazione → distruzione → verifica, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla distruzione;
- [ ] prova che la **catena resta integra** dopo la distruzione, che è il criterio che dà senso all'intera epica;
- [ ] **prova end-to-end**: **coprire ora** — il percorso `[J-AGENTAUDIT]` (storia 0037) riceve il passo
      «cancellazione del contenuto e verifica che la catena resti integra», e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'operazione di esercizio del diritto;
- [ ] **registro delle decisioni** compilato, con le voci su: distruzione della chiave come forma di
      cancellazione, evento di cancellazione nella catena, accessibilità con abbonamento scaduto, e **il fatto che
      il presupposto giuridico proviene da fonti non verificate**;
- [ ] contratto degli **strumenti conversazionali**: dichiarato il divieto di esposizione, con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0030` | Le chiavi e la separazione dei due strati sono il presupposto tecnico |
| storia `0031` | Senza contenuti conservati non c'è niente da cancellare |
| storia `0014` | La dimostrazione che la catena resta integra passa dalla verifica |
| **Revisione legale** | Dipendenza vera e bloccante, dichiarata in testa |
| Gestione delle chiavi e copie di sicurezza di piattaforma | Una distruzione che non raggiunge le copie di sicurezza non è una distruzione |

## 7. Fuori ambito

- **la cancellazione di righe della catena**: non si fa, e non è una limitazione tecnica ma la ragion d'essere del
  prodotto (vedi §8, «ciò che non propongo»);
- **la cancellazione mirata a una singola persona** invece che a un periodo: la granularità della chiave non lo
  permette (storia 0030, punti aperti). È il limite più serio dell'impianto ed è dichiarato, non nascosto;
- **la risposta formale all'interessato**: la fa il cliente, che è il titolare; l'app gli dà lo strumento e la
  prova, non redige la risposta;
- **l'esportazione dei dati dell'interessato**: storia 0033.

## 8. Punti aperti

- **Ciò che deliberatamente non propongo, e perché va detto qui e non solo altrove.** Non propongo di sostituire
  gli identificativi con altri codici: sostituire nomi con codici **non è cancellare**
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10), e per giunta romperebbe la catena. Non propongo
  la cancellazione fisica delle righe della catena durante il periodo di conservazione: distruggerebbe l'unica
  cosa che questa applicazione vende, e per tutti i clienti insieme, perché una catena che si può bucare su
  richiesta non dimostra più niente per nessuno.
- **Che cosa si fa se la richiesta riguarda gli identificativi nella catena** (per esempio l'identificativo di un
  dipendente che compare come richiedente di diecimila azioni) e non i contenuti. Qui la tecnica non aiuta: o
  prevale il dovere di conservazione per la durata dichiarata, o la catena si buca. **È la domanda centrale della
  revisione legale** e questa storia non la chiude.
- **La distruzione nelle copie di sicurezza.** Va risolta con chi presidia l'infrastruttura: se il materiale
  crittografico sopravvive in una copia, la promessa di irreversibilità è falsa. Preferisco dichiararlo come punto
  aperto piuttosto che promettere una cosa che dipende da un livello che questa app non governa.
