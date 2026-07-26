# UC 0088 — Search globale dal workspace del backoffice

**Area**: 19-debito-tecnico · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0020 (shell SPA del backoffice), UC 0013 (core API della piattaforma)
**Fonte**: R8 (Tabella dei residui in `_INDEX.md`); `docs/_BACKLOG.md` §"Feature deprioritizzate (note)"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Descrivere in dettaglio la **ricerca globale** avviabile dal menu del workspace del backoffice — la casella di ricerca
già presente nei mockup del design frontend, tipicamente in cima alla barra laterale (sidebar) o nella barra superiore.
L'obiettivo è consentire all'utente, da un unico punto, di **saltare rapidamente** a un'app, a una sezione o a
un'entità del proprio account senza navigare a mano nei menu.

Questo use case appartiene all'epica **Debito tecnico & feature deprioritizzate** ed è **deliberatamente non
prioritario al lancio**: l'utente non è interessato a questo caso d'uso adesso (annotazione del 2026-06-21). La casella
resta nei mockup ma a bassa priorità; va **rivalutata** quando il numero di app e di sezioni per account cresce al punto
da rendere davvero utile una scorciatoia di ricerca. Il file gemello dell'epica è
[UC 0089 — Rimozione legacy-peer-deps nel frontend](0089-rimozione-legacy-peer-deps.md).

**Incluso**: cosa si cerca (app, sezioni, entità cross-app dell'account), come si presentano i risultati, gli invarianti
di multi-tenancy e i permessi per ruolo. **Escluso**: ricerca full-text a livello di singola app (è responsabilità del
modulo app, non del workspace), ricerca amministrativa cross-account (è dominio della console admin, UC 0021), e ogni
motore di indicizzazione esterno.

## 2. Attori & ruoli
- **Utente autenticato del workspace** (owner / admin / member dell'account B2B, oppure utente B2C singolo): digita nella
  casella e naviga ai risultati.
- **Sistema (shell del backoffice)**: raccoglie la stringa digitata, la inoltra al core, presenta i risultati raggruppati.
- **Core API della piattaforma (UC 0013)**: risolve la ricerca lato server applicando gli invarianti multi-tenancy.

Nessun attore esterno (nessun fornitore di pagamento, nessun servizio terzo di ricerca).

## 3. Precondizioni
- L'utente ha una sessione valida: il token verificato (JWT) porta `tenant_id` (= account) e `sub` (= user_id).
- La shell SPA del backoffice (UC 0020) è avviata e ha già risolto la sidebar "YOUR APPS" (manifesto ∩ entitlement
  dell'account).
- Il core espone un endpoint di ricerca tenant-scoped (da introdurre: oggi non esiste — vedi Punti aperti).
- L'utente ha almeno un'app abilitata e/o dati nel proprio account, altrimenti la ricerca è utile ma restituisce poco.

## 4. Flusso principale
1. L'utente apre la casella di ricerca dal menu del workspace (scorciatoia da tastiera o clic).
2. Digita almeno un numero minimo di caratteri (es. 2-3), con attesa breve prima dell'invio (debounce) per non
   interrogare il server a ogni tasto.
3. La shell invia la stringa al core, che cerca **solo dentro il perimetro dell'account** dell'utente, su tre famiglie di
   oggetti:
   - **App**: i moduli abilitati per l'account (intersezione manifesto ∩ entitlement) il cui nome/etichetta combacia.
   - **Sezioni**: le voci di navigazione interne al workspace e alle app abilitate (es. "Abbonamenti", "Impostazioni",
     "Fatture emesse"), utili come scorciatoie di navigazione.
   - **Entità cross-app dell'account**: record appartenenti alle app abilitate (es. una fattura per numero/cliente),
     limitati a ciò che l'utente può vedere.
4. Il core restituisce i risultati **raggruppati per famiglia**, ciascuno con etichetta, tipo, app di provenienza e la
   rotta di destinazione nel workspace.
5. La shell mostra i risultati raggruppati; l'utente seleziona un risultato (clic o tastiera) e viene portato alla rotta
   corrispondente (app, sezione o dettaglio entità).

## 5. Flussi alternativi / edge / errori
- **Edge — pochi caratteri**: sotto la soglia minima non si interroga il server; la casella mostra un suggerimento
  ("Continua a digitare…").
- **Edge — nessun risultato**: stato vuoto esplicito ("Nessun risultato per «…»"), senza errore.
- **Edge — molti risultati**: si limita il numero per famiglia (es. i primi N) con un "vedi tutti" verso la sezione
  pertinente, per non scaricare l'intero account.
- **Edge — entità di un'app non più abilitata**: se l'entitlement è decaduto, l'entità non deve comparire; la ricerca
  rispetta lo stesso perimetro della sidebar.
- **Errore — chiamata al core fallita**: la casella mostra uno stato d'errore non bloccante ("Ricerca non disponibile,
  riprova"); il resto del workspace resta usabile. Le risposte d'errore del core seguono il formato problem+json della
  piattaforma.
- **Edge — permessi per ruolo**: un member non deve vedere fra i risultati sezioni/entità riservate a owner/admin (es.
  gestione abbonamento o membri): il filtro per ruolo si applica **lato server**, non nascondendo solo in interfaccia.

## 6. Schermate & stati
- **Casella di ricerca (chiusa)**: campo compatto nel menu del workspace, con segnaposto e scorciatoia da tastiera
  suggerita.
- **Casella aperta / in digitazione**: pannello a comparsa sotto la casella.
  - *Stato caricamento*: indicatore leggero mentre il core risponde.
  - *Stato vuoto (sotto soglia)*: suggerimento a continuare a digitare.
  - *Stato risultati*: elenco raggruppato per famiglia (App / Sezioni / Entità), ogni voce con icona di tipo, etichetta,
    app di provenienza; navigazione da tastiera (frecce + invio).
  - *Stato nessun risultato*: messaggio esplicito con la stringa cercata.
  - *Stato errore*: messaggio non bloccante con azione "riprova".
- **Selezione**: alla scelta la casella si chiude e il workspace naviga alla rotta di destinazione.

## 7. Dati toccati
Nessuna nuova entità persistente propria: la ricerca **legge** dati già esistenti nel perimetro dell'account (metadati
delle app abilitate, catalogo delle sezioni, entità delle app). Se in futuro alcune entità cercabili contengono dati
personali (es. nominativo cliente in una fattura), la ricerca **non introduce** un nuovo trattamento: rispetta il
manifesto dati e la classificazione dell'app di provenienza. Non si crea un indice separato con copie di dati personali
senza una decisione dedicata (vedi Punti aperti). Eventuali query restano soggette al filtro row-level
`WHERE tenant_id = :tid`.

## 8. Permessi & gate
- **Invariante multi-tenancy**: la ricerca usa esclusivamente il `tenant_id` del token verificato; mai un identificativo
  di account preso dal corpo o dai parametri della richiesta. Ogni query sottostante applica il filtro row-level
  `WHERE tenant_id = :tid`.
- **Entitlement**: compaiono solo le app abilitate all'account (stesso perimetro della sidebar, manifesto ∩ entitlement).
- **Ruolo** (owner / admin / member): sezioni ed entità riservate sono escluse lato server per chi non ha il ruolo; il
  filtro non è mai solo cosmetico in interfaccia.
- **Quota**: non pertinente (operazione di sola lettura); eventualmente un limite tecnico di frequenza per evitare abuso.

## 9. Requisiti di test
- **Unit (frontend)**: comportamento della casella — soglia minima, attesa prima dell'invio (debounce), stati
  vuoto/caricamento/errore/risultati, navigazione da tastiera.
- **Integration (core, con Testcontainers)**: la ricerca restituisce solo oggetti del perimetro dell'account; il filtro
  per ruolo esclude ciò che non spetta.
- **Sicurezza — isolamento cross-account**: un utente dell'account A non trova mai app/sezioni/entità dell'account B,
  nemmeno con stringhe che combacerebbero; test anti-leak sistematico.
- **End-to-end (Playwright)**: dal workspace, digitare, ottenere risultati raggruppati, selezionare e atterrare sulla
  rotta attesa.
- Tutte le aree toccate devono essere verdi via `run-tests.sh` prima del merge.

## 10. Riferimenti & Definition of Done
- **Dipendenze**: UC 0020 (shell SPA del backoffice) per la casella e le rotte; UC 0013 (core API) per l'endpoint di
  ricerca tenant-scoped.
- **Fonte**: R8 nella tabella dei residui di `_INDEX.md`; `_BACKLOG.md` §"Feature deprioritizzate (note)".
- **DoD**: casella funzionante nel workspace; ricerca su app + sezioni + entità cross-app dentro il perimetro
  dell'account; invarianti multi-tenancy e filtro per ruolo verificati lato server; stati d'interfaccia completi; test di
  isolamento cross-account verdi; nessun nuovo trattamento di dati personali introdotto senza classificazione.

## Punti aperti / decisioni differite
- **Deprioritizzazione per scelta** (2026-06-21, proprietario di questo UC): la ricerca globale **non** entra nel lancio.
  Va rivalutata quando il numero di app/sezioni per account la rende davvero utile. Fino ad allora resta solo nei mockup.
- **Sorgente dei risultati "entità cross-app"**: da decidere se la ricerca interroga on-demand ogni app abilitata
  (interpellare tutte le app) oppure si appoggia a un indice dedicato. La seconda via, se contiene copie di dati
  personali, richiede una classificazione nel manifesto dati e una finalità/base giuridica dedicate (#13): decisione
  differita a quando l'UC verrà ripreso.
- **Contratto dell'endpoint di ricerca nel core**: oggi il core (UC 0013) non espone un endpoint di ricerca
  tenant-scoped; il suo contratto (forma della richiesta/risposta, raggruppamento, limiti) va definito insieme al team
  core quando l'UC verrà implementato.
