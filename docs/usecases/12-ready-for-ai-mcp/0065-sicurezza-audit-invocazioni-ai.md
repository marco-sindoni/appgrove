# UC 0065 — Sicurezza & audit delle invocazioni AI + postura privacy

**Area**: 12-ready-for-ai-mcp · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0006 (observability / logging strutturato), UC 0030 (manifesti dati + registro dei trattamenti, RoPA), UC 0061 (architettura server MCP) — devono esistere prima: la piattaforma di log strutturati dove tracciare le invocazioni, i manifesti dati/registro dei trattamenti su cui allineare la postura privacy, e la collocazione del server MCP che genera gli eventi.
**Fonte**: R2 (Tabella dei residui, _INDEX.md) · docs/_BACKLOG.md §"Ready for AI"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Rendere **tracciabile e difendibile** ogni invocazione arrivata via assistente AI: un registro di audit di *chi/cosa/quando*
per ogni chiamata, e una **postura privacy** esplicita per cui verso l'assistente non esce alcun dato personale oltre il
necessario. Include l'analisi dei rischi propri di questo canale (istruzioni malevole iniettate nel linguaggio naturale —
"prompt injection", cioè testo che tenta di dirottare il comportamento dell'assistente — ed esfiltrazione di dati) e le
misure che li contengono. **Fuori scope**: la definizione dei gate (UC 0064) e del consenso (UC 0062), qui presupposti.

## 2. Attori & ruoli
- **Piattaforma appgrove**: emette gli eventi di audit e applica la minimizzazione in uscita.
- **Assistente AI**: destinatario dei risultati minimizzati; potenziale vettore di istruzioni malevole.
- **Utente/tenant**: titolare dei dati la cui minimizzazione va garantita.
- **Chi presidia sicurezza e conformità**: legge l'audit, aggiorna il registro dei trattamenti (UC 0030), reagisce agli incidenti.

## 3. Precondizioni
- Il logging strutturato di piattaforma (UC 0006) è attivo e riceve `tenant_id`, `app_id`, `user_id` su ogni log.
- I manifesti dati e il registro dei trattamenti (UC 0030) esistono e vanno tenuti coerenti con il nuovo canale.
- I gate (UC 0064) e il consenso (UC 0062) sono applicati a monte dell'operazione.

## 4. Flusso principale
1. All'apertura di sessione e a ogni invocazione, il server MCP emette un **evento di audit strutturato**: tenant,
   utente delegante, assistente, strumento invocato, esito (successo/negato/errore), timestamp, esito dei gate.
2. L'evento **non** registra il contenuto dei dati personali trattati: annota *che* un'operazione è avvenuta, non il
   dettaglio del dato (es. "emessa fattura", non l'anagrafica completa del cliente).
3. Prima di restituire il risultato all'assistente, si applica la **minimizzazione in uscita**: solo i campi dichiarati
   necessari dal contratto strumenti (UC 0063), con i dati personali non indispensabili omessi o ridotti.
4. Gli input dall'assistente sono trattati come **non fidati**: validati contro lo schema (UC 0063) e mai interpretati
   come comandi privilegiati; l'assistente non può ampliare i propri scope né aggirare i gate parlando in linguaggio naturale.
5. L'audit alimenta gli strumenti di osservabilità (UC 0006) e resta disponibile per revisione e per l'eventuale gestione
   di una violazione di dati (raccordo con la skill/runbook di risposta alle violazioni).

## 5. Flussi alternativi / edge / errori
- **Istruzioni malevole nel linguaggio naturale**: l'assistente potrebbe ricevere dall'utente (o da contenuti esterni)
  testo che tenta di far invocare strumenti fuori intento. Misura: i gate (entitlement/ruolo/scope/quota) restano l'ultima
  parola, indipendenti dal testo; nessuno strumento distruttivo è esposto senza conferma (UC 0063).
- **Tentativo di esfiltrazione**: un'invocazione che cerca di estrarre più dati del necessario è limitata dalla
  minimizzazione in uscita (UC 0063/qui) e dal filtro per tenant; l'audit registra il tentativo per revisione.
- **Volume anomalo di invocazioni**: raffiche automatiche → contenute dalla quota (UC 0064); l'audit evidenzia il picco.
- **Errore — evento di audit non scrivibile**: l'operazione va trattata fail-closed se l'audit è un requisito di
  conformità (meglio negare che eseguire non tracciato), secondo la criticità dell'operazione.
- **Edge — richiesta di cancellazione/diritti dell'interessato**: le tracce di audit seguono la retention definita e
  non ostacolano l'esercizio dei diritti previsti dalla normativa sui dati personali.

## 6. Risorse & runbook
- **Formato evento di audit**: campi strutturati (tenant, utente, assistente, strumento, esito gate, esito operazione,
  timestamp), coerenti con il logging di UC 0006; nessun dato personale di dettaglio nel corpo dell'evento.
- **Cruscotti/allarmi**: viste per volume di invocazioni AI per tenant, tasso di errori di limite, tentativi negati;
  soglie di allarme su picchi anomali.
- **Runbook privacy**: aggiornare il registro dei trattamenti (UC 0030) per includere il canale AI come *modalità di
  accesso* alle operazioni esistenti; verificare che non introduca nuove categorie di dati o nuovi destinatari.
- **Runbook incidente**: in caso di sospetto abuso, revoca delle deleghe (UC 0062) e consultazione dell'audit; raccordo
  con il processo di risposta alle violazioni di dati.

## 7. Dati toccati
- **Registro di audit delle invocazioni AI**: contiene identificatori (`tenant_id`, `user_id`, assistente) collegabili a
  una persona → **dati personali**. Finalità = sicurezza, tracciabilità e conformità; base giuridica = legittimo interesse
  alla sicurezza del servizio e obblighi di rendicontazione; retention = periodo definito per audit/sicurezza, poi
  cancellazione; allineare al manifesto dati e al registro dei trattamenti (UC 0030).
- **Punto chiave — minimizzazione verso l'assistente**: è il cuore di questo use case. Verso l'assistente AI esce **solo**
  il minimo indispensabile all'operazione richiesta; nessun dato personale non necessario. L'audit registra l'accaduto,
  non i contenuti sensibili.

## 8. Permessi & gate
- I gate di UC 0064 (entitlement/ruolo/quota) e il consenso di UC 0062 restano l'autorità: il linguaggio naturale non li aggira.
- Tenant ID solo dal token verificato; filtro row-level `WHERE tenant_id = :tid` sia sull'operazione sia sulle letture di audit.
- L'audit è tenant-scoped: un tenant/amministratore non vede le invocazioni di un altro tenant.
- Postura fail-closed anche sull'audit per le operazioni dove la tracciabilità è requisito.

## 9. Requisiti di test
- **Completezza audit**: ogni invocazione (successo, negata, errore) genera un evento con i campi attesi.
- **Assenza di dati personali nell'audit**: gli eventi non contengono dati personali di dettaglio.
- **Minimizzazione in uscita**: il risultato verso l'assistente contiene solo i campi dichiarati necessari.
- **Input non fidato**: testo malevolo non amplia scope né aggira i gate (test di sicurezza).
- **Isolamento cross-tenant** su operazione e su letture di audit.

## 10. Riferimenti & Definition of Done
- **Riferimenti**: UC 0006 (observability), UC 0030 (manifesti dati / registro dei trattamenti), UC 0061 (architettura);
  file fratelli: 0062-auth-consenso-delegato-ai.md, 0063-mappatura-operazioni-strumenti-mcp.md, 0064-enforcement-quota-entitlement-ai.md.
- **DoD**: formato evento di audit definito e integrato con UC 0006; minimizzazione in uscita verificata; registro dei
  trattamenti (UC 0030) aggiornato per il canale AI; rischi (istruzioni malevole, esfiltrazione) analizzati con misure;
  test di completezza audit, minimizzazione, input non fidato e isolamento verdi.

## Punti aperti / decisioni differite
- **L'intera epica 12 "Ready for AI (MCP)" è direzione di prodotto non ancora decisa**: collocazione del server (UC 0061),
  modello di consenso (UC 0062), formato strumenti (UC 0063) da chiudere in sessioni dedicate. Proprietaria: **epica 12**.
- **Da valutare (non deciso)**: se il canale AI vada dichiarato nel registro dei trattamenti come semplice *modalità di
  accesso* o se, a seconda del modello di consenso scelto (UC 0062), configuri un nuovo destinatario/responsabile esterno.
  Classificazione privacy potenzialmente ambigua → decisione dedicata, tracciata qui, non forzata.
