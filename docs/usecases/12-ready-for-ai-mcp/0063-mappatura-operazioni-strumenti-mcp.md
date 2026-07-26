# UC 0063 — Mappatura operazioni app → strumenti MCP (contratto per-app)

**Area**: 12-ready-for-ai-mcp · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0061 (architettura server MCP), UC 0051 (app #1 "fatture"), UC 0046 (skill new-application) — devono esistere prima: la collocazione del server che espone gli strumenti, un'app reale con operazioni da mappare, e il generatore che scaffolda ogni nuova app (per industrializzare il contratto).
**Fonte**: R2 (Tabella dei residui, _INDEX.md) · docs/_BACKLOG.md §"Ready for AI"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Definire il **contratto per-app** che dichiara quali operazioni dell'applicazione diventano **strumenti** (tool)
richiamabili dall'assistente AI, con quale schema di parametri, quali descrizioni, e con quali garanzie (idempotenza,
lettura vs scrittura). L'assistente traduce il linguaggio naturale dell'utente in un'invocazione di strumento: la
qualità del contratto determina se lo fa bene. **Fuori scope**: consenso/scope (UC 0062), conteggio quota (UC 0064),
audit/privacy (UC 0065). Qui si definisce *cosa è esposto e come è descritto*, non *chi può chiamarlo*.

## 2. Attori & ruoli
- **Sviluppatore dell'app**: scrive il contratto degli strumenti dentro il proprio `services/<app>`.
- **Assistente AI**: legge la descrizione degli strumenti e sceglie quale invocare, con quali parametri.
- **Server MCP** (UC 0061): pubblica gli strumenti dichiarati e instrada l'invocazione al codice dell'app.
- **Skill new-application** (UC 0046): scaffolda il contratto di base per ogni nuova app (industrializzazione, UC 0066).

## 3. Precondizioni
- Il server MCP è collocato e sa esporre strumenti (UC 0061).
- L'app ha operazioni di dominio già implementate e testate (es. "crea fattura", "elenca clienti" in UC 0051).
- Esistono le convenzioni di scoperta locale per rendere l'app avviabile senza cablaggi (invariante avvio locale).

## 4. Flusso principale
1. Lo sviluppatore dichiara, nel proprio servizio, l'elenco degli strumenti: per ciascuno un **nome stabile**, una
   **descrizione in linguaggio naturale** (che è ciò su cui l'assistente ragiona), lo **schema dei parametri**
   (tipi, obbligatorietà, valori ammessi) e lo **schema del risultato**.
2. Ogni strumento è marcato come **lettura** o **scrittura** e, se scrittura, dichiara se è **idempotente** (ripetere
   la stessa chiamata non produce effetti doppi — es. tramite una chiave di deduplicazione fornita dall'assistente).
3. Il contratto vive **dentro `services/<app>`** (coeso col dominio), non in un registro centrale: è versionato con l'app.
4. Il server MCP legge il contratto e pubblica gli strumenti che il tenant può vedere (in base a entitlement e scope, UC 0062/0064).
5. All'invocazione, i parametri sono **validati contro lo schema** prima di toccare il dominio; solo poi si esegue
   l'operazione con filtro `WHERE tenant_id = :tid`.
6. Il risultato è restituito nella forma minimizzata prevista dal contratto (nessun campo personale non necessario, UC 0065).

## 5. Flussi alternativi / edge / errori
- **Parametri non validi**: l'assistente manda dati che non rispettano lo schema → errore di validazione descrittivo,
  così l'assistente può correggere e riprovare; nessuna operazione eseguita.
- **Operazione ambigua**: se la descrizione non basta a scegliere i parametri (es. cliente omonimo), lo strumento
  restituisce un risultato che invita a disambiguare, invece di indovinare.
- **Operazione distruttiva**: le operazioni irreversibili o ad alto impatto (es. cancellazioni, invii verso l'esterno)
  o **non** vengono esposte come strumento, o richiedono una conferma esplicita nel contratto — scelta consapevole per app.
- **Idempotenza mancante**: uno strumento di scrittura non idempotente ripetuto dall'assistente rischia doppioni → va
  reso idempotente (chiave di deduplicazione) o marcato in modo che l'assistente sappia che non deve ripeterlo.
- **Edge — evoluzione del contratto**: cambiare lo schema di uno strumento è un cambio di interfaccia pubblica; va
  versionato con attenzione per non rompere gli assistenti già collegati.

## 6. Risorse & runbook
- **Dove vive**: file di dichiarazione degli strumenti dentro `services/<app>`, versionato con il codice dell'app.
- **Scaffolding**: la skill `new-application` (UC 0046) genera un contratto minimo (almeno gli strumenti di lettura
  di base) così ogni nuova app nasce AI-ready; il dettaglio dell'industrializzazione è in UC 0066.
- **Convenzioni**: nomi stabili, descrizioni chiare orientate all'intento dell'utente, schemi stretti (meglio pochi
  parametri ben tipati che un parametro libero), separazione netta lettura/scrittura.
- **Runbook**: aggiunta/modifica di uno strumento passa dalla revisione del contratto + test; nessuna esposizione
  automatica di operazioni non dichiarate.

## 7. Dati toccati
Il contratto **descrive** operazioni su dati di dominio ma non è esso stesso un deposito di dati personali. Il **punto
chiave** è la minimizzazione a livello di schema: lo schema del risultato deve restituire all'assistente **solo** i campi
necessari all'operazione richiesta, escludendo per default i dati personali non indispensabili (es. non restituire indirizzo
e codice fiscale completo del cliente se all'assistente serve solo confermare l'emissione). Ogni campo personale esposto va
allineato al manifesto dati dell'app (UC 0030) e alla postura di UC 0065.

## 8. Permessi & gate
- La dichiarazione di uno strumento **non** ne autorizza l'uso: l'accesso passa sempre da entitlement, ruolo, scope (UC 0062)
  e quota (UC 0064). Il contratto dice *cosa esiste*, i gate dicono *chi può*.
- Tenant ID solo dal token verificato; filtro row-level `WHERE tenant_id = :tid` nell'operazione servita.
- Gli strumenti di scrittura rispettano gli stessi controlli di ruolo (`@RolesAllowed` o equivalente) delle rotte normali.

## 9. Requisiti di test
- **Validazione schema**: parametri non conformi → errore descrittivo, nessun effetto sul dominio.
- **Idempotenza**: uno strumento di scrittura marcato idempotente, ripetuto, non crea doppioni.
- **Minimizzazione risultato**: il risultato non contiene campi personali fuori da quelli dichiarati necessari.
- **Coerenza col dominio**: lo strumento produce lo stesso effetto della rotta normale corrispondente (parità comportamentale).
- **Isolamento cross-tenant** sull'operazione servita dietro lo strumento.

## 10. Riferimenti & Definition of Done
- **Riferimenti**: UC 0061 (architettura), UC 0051 (app #1), UC 0046 (new-application);
  file fratelli: 0062-auth-consenso-delegato-ai.md, 0064-enforcement-quota-entitlement-ai.md, 0065-sicurezza-audit-invocazioni-ai.md,
  0066-industrializzazione-mcp-newapp.md.
- **DoD**: formato del contratto definito (nome, descrizione, schema parametri/risultato, lettura/scrittura, idempotenza);
  contratto collocato in `services/<app>`; minimizzazione a livello di schema; test di validazione/idempotenza/parità/isolamento verdi.

## Punti aperti / decisioni differite
- **Formato del contratto strumenti da decidere.** Come si dichiarano gli strumenti (linguaggio, posizione esatta nel
  servizio, meccanismo di generazione dallo schema esistente) è un nodo **non ancora deciso**, da chiudere in sessione
  dedicata insieme alla collocazione (UC 0061). Proprietaria: **epica 12**.
- **L'intera epica 12 è direzione di prodotto non ancora decisa**: questo contratto è descritto come struttura-obiettivo,
  non come formato scelto.
