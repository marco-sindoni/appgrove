# 0012 — Sorgente nativa appgrove

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come cliente appgrove che ha appena attivato AuditGrove
> voglio che le azioni che l'assistente compie sulle mie app appgrove finiscano nel registro **senza che io debba
> collegare niente**
> così da vedere il prodotto funzionare dal primo minuto, invece di dover prima scrivere un adattatore per un mio
> agente.

**Contesto.** È il vantaggio strutturale descritto al §0 della
[descrizione dell'applicazione](../application-description.md): appgrove è nativamente conversazionale, quindi ogni
sua app espone strumenti, quindi la piattaforma è **una sorgente già cablata**. Mentre i concorrenti chiedono di
mettere un intermediario di rete davanti ai propri agenti prima di vedere una riga, qui basta un interruttore.
Una precisazione che va fatta subito e ripetuta: la piattaforma **ha già** l'obbligo di emettere un evento di
audit per ogni invocazione dell'assistente
([UC 0065](../../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md)). AuditGrove quel registro lo
**riceve**, non lo rifà: duplicarlo sarebbe costruire due volte la stessa cosa e ottenere due verità.

## 2. Requisiti funzionali

1. **RF-1** — Nella sezione Sorgenti esiste la sorgente **nativa appgrove**, che si attiva e si disattiva con un
   interruttore, **senza chiavi da emettere né da distribuire**.
2. **RF-2** — Attivata la sorgente, gli eventi di audit delle invocazioni dell'assistente sulle app appgrove
   dell'account diventano azioni del registro, senza altra configurazione.
3. **RF-3** — La ricezione è **asincrona a eventi**: AuditGrove non chiama nessuna altra app e nessun'altra app
   chiama AuditGrove sul percorso caldo.
4. **RF-4** — Ogni riga proveniente dalla sorgente nativa porta la **natura della sorgente**, perché per queste
   righe il titolare del trattamento è appgrove e non il cliente (§6.1 della descrizione dell'applicazione): due
   nature che convivono nella stessa tabella e che devono restare distinguibili.
5. **RF-5** — Finché il livello conversazionale della piattaforma non esiste, la storia consegna **il contratto di
   ricezione** e una **sorgente nativa simulata** utilizzabile in locale, così che il percorso sia costruito,
   provato e pronto ad accendersi.
6. **RF-6** — La disattivazione della sorgente ferma l'arrivo di nuove righe e **non tocca** quelle già ricevute.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'evento in arrivo porta il proprio `tenant_id`, che è quello del token
  verificato al bordo dal quale è nata l'invocazione: la riga finisce nella catena di quell'account e in nessun
  altro. Un evento il cui account non corrisponde a una sorgente nativa attiva viene scartato, non «attribuito al
  meglio».
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna chiamata sincrona fra app: la comunicazione è **solo**
  asincrona a eventi, come impone l'architettura. Rotte per attivare e disattivare la sorgente nativa; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V9__sorgente_nativa.sql` sullo schema `app_agentaudit`: la natura della
  sorgente sulle righe delle azioni e lo stato della sorgente nativa, con `tenant_id`, chiave primaria UUID
  versione 7 e colonne di controllo. Nessuna interrogazione verso schemi di altre app: vietata dalla piattaforma e
  qui sarebbe anche concettualmente sbagliata.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione Sorgenti, la sorgente nativa compare in cima, con
  l'interruttore e la spiegazione di che cosa comprende e che cosa no. Le sue righe sono riconoscibili nella
  Cronologia. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi della sorgente nativa — nome, descrizione, avviso sulla diversa natura
  dei dati — passano dallo spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Le righe della sorgente nativa **consumano** la metrica `actions` come tutte
  le altre: sono azioni registrate a tutti gli effetti. ⚠️ Se ciò sia opportuno è però una domanda di prodotto —
  vedi punti aperti.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Vale però una simmetria che va scritta:
  quando esisterà il livello conversazionale, **le chiamate dell'assistente ad AuditGrove stesso** saranno a loro
  volta azioni registrate (storia 0036): è l'unico caso del catalogo in cui il registro contiene le proprie
  interrogazioni.
- **RT-8 — Dati personali (§10).** ⚠️ Voce nuova nel manifesto in italiano e inglese con una particolarità da
  dichiarare esplicitamente: per queste righe **il titolare è appgrove**, mentre per tutte le altre righe della
  stessa tabella il titolare è il cliente. La distinzione va scritta nel manifesto e nel registro dei
  trattamenti, non lasciata implicita — è il genere di cosa che sembra un dettaglio e poi genera una
  contestazione.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «sorgente nativa attivata», «evento di piattaforma ricevuto»,
  «evento scartato per account non corrispondente» sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Si accende con un interruttore**
- **Dato** un account abilitato ad AuditGrove e ad almeno un'altra app appgrove
- **Quando** attiva la sorgente nativa
- **Allora** la sorgente compare attiva, senza che sia stata emessa nessuna chiave, e le invocazioni successive
  dell'assistente diventano righe del registro

**CA-2 — La natura della riga è dichiarata**
- **Dato** un registro con righe di sorgente nativa e righe di un agente del cliente
- **Quando** si apre la Cronologia
- **Allora** le due nature sono distinguibili in modo esplicito, e la scheda di una riga nativa dichiara che per
  essa il titolare del trattamento è appgrove

**CA-3 — Nessuna chiamata sincrona fra app**
- **Dato** il servizio in esecuzione
- **Quando** si esaminano le sue dipendenze in uscita
- **Allora** non esiste nessuna chiamata di rete sincrona verso un'altra app appgrove, né in ingresso da un'altra
  app sul percorso caldo

**CA-4 — Spegnere non cancella**
- **Dato** una sorgente nativa attiva con cinquanta righe ricevute
- **Quando** viene disattivata
- **Allora** non arrivano più righe nuove e le cinquanta esistenti restano invariate nel registro

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con la sorgente nativa attiva
- **Quando** arriva un evento dell'account `B`
- **Allora** finisce solo nella catena di `B`, e un utente di `A` non lo vede nemmeno forzando l'identificativo
  dell'altro account nella richiesta

**CA-6 — La sorgente simulata regge il percorso**
- **Dato** lo stack locale, dove il livello conversazionale non esiste
- **Quando** si usa la sorgente nativa simulata
- **Allora** le righe entrano nel registro attraverso lo stesso contratto di ricezione che userà la piattaforma
  vera

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul contratto di ricezione e sullo scarto degli eventi non attribuibili, e di
      **integrazione** sulla ricezione asincrona, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla ricezione degli eventi di piattaforma;
- [ ] **prova end-to-end**: risposta «rimando» — la sorgente nativa entra nel percorso `[J-AGENTAUDIT]` alla
      storia 0037, proprietaria della copertura, e comunque **non è provabile end-to-end in modo completo** finché
      l'epica 12 non esiste; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata con
      questo motivo scritto per esteso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la doppia natura del titolare, e il registro dei
      trattamenti allineato;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: ricevere e non rifare l'audit di
      piattaforma (UC 0065), e la doppia natura del titolare nella stessa tabella;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto; la simmetria della storia 0036 è
      dichiarata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali, con la sorgente nativa
      simulata disponibile in locale.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | La sorgente nativa è una sorgente: ne condivide stato, scheda e ciclo di vita, senza la chiave |
| storia `0008` | Il contratto dell'azione e l'accodamento sono gli stessi |
| **Epica 12 della piattaforma (UC 0061-0066)** | È **scritta e non implementata**. Senza il livello conversazionale non esistono invocazioni dell'assistente da ricevere: nel frattempo si consegna il contratto di ricezione e la sorgente simulata |
| [UC 0065](../../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md) | Definisce l'evento di audit di piattaforma: è la sorgente di ciò che qui si riceve, e non va rifatto |

## 7. Fuori ambito

- **costruire il livello conversazionale**: è di piattaforma (UC 0061-0063), non di un'app;
- **rifare l'audit di piattaforma**: UC 0065 lo prevede già; qui lo si riceve;
- le regole e l'approvazione applicate alle invocazioni dell'assistente: i varchi dell'assistente sono UC 0064, e
  ciò che AuditGrove può aggiungere è materia dell'epica 04 — ma solo dopo che l'epica 12 esista;
- la vendita separata della sorgente nativa: è **inclusa**, per la ragione scritta al §0 della descrizione
  dell'applicazione (non si fa pagare ciò che è già dovuto).

## 8. Punti aperti

- ⚠️ **Le righe della sorgente nativa devono consumare quota?** Argomento a favore: sono azioni registrate e
  costano come le altre. Argomento contro: sono generate dalla piattaforma stessa, il cliente non le controlla, e
  fargliele pagare significa fargli pagare l'uso di un'altra app appgrove. Propongo di **non** farle consumare, o
  di farle consumare con un tetto separato — ma è una decisione di prezzo, cioè una **fermata di escalation dello
  sviluppatore**.
- ⚠️ **La doppia natura del titolare nella stessa tabella** (§6.1 della descrizione) è materia di **revisione
  legale**: se le due nature comportassero regimi di conservazione o diritti diversi, potrebbe servire una
  separazione più netta di una colonna. Non si implementa prima di quella revisione.
- **L'intera epica 12 è direzione di prodotto non ancora decisa** (collocazione del server, modello di consenso,
  formato del contratto degli strumenti): questa storia si aggancia a una struttura-obiettivo, non a un formato
  scelto. Se l'epica 12 non arrivasse, AuditGrove resta un prodotto valido ma perde il proprio fossato — è il
  rischio dichiarato al §11 della descrizione dell'applicazione.
