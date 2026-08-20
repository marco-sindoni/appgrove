# UC 0107 — Menu, rotte e visibilità per ruolo

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.3 Esperienza per ruolo](../epic/E22-03-esperienza-per-ruolo.md)
**Dipendenze**: UC 0098 (modello dati), UC 0099 (lettura «dove posso entrare»), UC 0020 (shell del backoffice), UC 0077 (diritti d'accesso reali)
**Piano di lavoro**: [task/0107](../task/0107-menu-rotte-visibilita-per-ruolo.md)
**Prototipi**: [owner](../prototype/owner.html) · [admin](../prototype/admin.html) · [editor](../prototype/editor.html) · [viewer](../prototype/viewer.html)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Rendere il backoffice coerente con chi lo guarda: il collaboratore vede **solo le applicazioni a cui è
abilitato** e **non vede** le sezioni di governo dell'account.

**Incluso**: la nuova regola del menu laterale (intersezione a tre); la sparizione di Account, Billing e
Members per i non-owner; la stretta delle guardie di rotta da «owner o admin» a «solo owner»; il ruolo
nel contratto fra shell e moduli; il comportamento quando il ruolo non è ancora noto o non è leggibile.

**Escluso**: il cruscotto → UC 0108; il catalogo → UC 0109; «I miei dati» → UC 0110; la schermata di
gestione utenti → UC 0111.

## 2. Attori & ruoli

- **Owner**: vede tutto, tutte le applicazioni dell'account comprese.
- **Collaboratore** (`member` di piattaforma, con qualunque ruolo sulle applicazioni): vede il menu
  ridotto e solo le sue applicazioni.
- **Amministratore di piattaforma**: fuori da questo meccanismo; ha la sua console.

## 3. Precondizioni

- Esiste la lettura «dove posso entrare e con che ruolo» (UC 0099).
- Il menu laterale già fa l'intersezione fra registro dei moduli e diritti dell'account
  ([Sidebar.tsx](../../../../frontend/apps/backoffice/src/shell/Sidebar.tsx)).

## 4. Flusso principale

1. La persona entra. La shell legge, in parallelo, i **diritti dell'account** (che applicazioni ha
   comprato) e i **propri accessi** (su quali di quelle può entrare, con che ruolo).
2. Il menu «Le tue applicazioni» mostra l'**intersezione a tre**: registro dei moduli ∩ diritti
   dell'account ∩ accessi della persona. Per l'owner il terzo insieme è, per definizione, tutto il
   secondo.
3. Il menu «Piattaforma» mostra:

   | Voce | Owner | Collaboratore |
   |---|---|---|
   | Dashboard | sì | sì (in forma informativa, UC 0108) |
   | App catalog | sì | sì (in sola lettura, UC 0109) |
   | Account | sì | **assente** |
   | Billing | sì | **assente** |
   | Members | sì | **assente** |
   | I miei dati | sì (completo) | sì (ridotto, UC 0110) |
   | Supporto | sì | sì |
   | Impostazioni | sì | sì (preferenze personali) |

4. Aperta una applicazione, la shell passa al modulo il **ruolo della persona su quella applicazione**,
   che il modulo usa per abilitare o disabilitare i propri comandi (UC 0101).
5. Le rotte riservate rimandano alla pagina di rifiuto se raggiunte per indirizzo diretto.

## 5. Flussi alternativi / edge / errori

- **Edge — ruolo non ancora noto** (letture in corso): la shell mostra lo stato di caricamento e **non**
  abilita nulla. Mai il contrario: un comando abilitato «in attesa» produce un rifiuto dal servizio e una
  brutta impressione.
- **Errore — accessi non leggibili**: è un **guasto**, non un diniego. Si mostra l'errore con la
  possibilità di riprovare, esattamente come si è già fatto per i diritti d'accesso; dire a un
  collaboratore «non hai accesso a nulla» per un guasto di rete è il difetto che quella storia ha chiuso e
  che non va reintrodotto.
- **Edge — persona senza alcuna applicazione**: il menu delle applicazioni è vuoto, con un testo che
  spiega («nessuna applicazione ti è stata abilitata; chiedi all'owner») e un rimando al catalogo (dove
  potrà chiedere l'installazione, UC 0109). Non è un errore.
- **Edge — accesso revocato mentre la persona sta lavorando**: alla lettura successiva l'applicazione
  scompare dal menu e la rotta rimanda al rifiuto. Il lavoro in corso non viene salvato per magia: il
  servizio rifiuterà la scrittura con il messaggio giusto.
- **Edge — applicazione disattivata dalla piattaforma**: comportamento già esistente (decade il diritto
  dell'account); si somma a questa regola senza casi speciali.
- **Edge — l'owner apre un'applicazione su cui non ha righe di accesso**: entra con il ruolo massimo, per
  costruzione.

## 6. Schermate & stati

- **Menu laterale**: le voci riservate non compaiono; nessuna voce disabilitata (la disabilitazione è per
  i comandi dentro le applicazioni, non per la navigazione di ambito).
- **Scheda utente nel menu**: mostra il ruolo di piattaforma solo se è `owner` («Titolare
  dell'account»); per i collaboratori non mostra alcun ruolo, perché non ne hanno uno globale — mostrarne
  uno sarebbe la contraddizione che questa epica vuole togliere.
- **Pagina di rifiuto**: resta quella esistente, con un testo che dice **chi** può darti accesso.
- Stati: caricamento, pronto, errore di lettura degli accessi, elenco vuoto.

## 7. Dati toccati

Nessuno nuovo. Si consuma la lettura di UC 0099. Il contratto fra shell e moduli
([types.ts](../../../../frontend/apps/backoffice/src/registry/types.ts)) cambia: il campo dei ruoli, oggi
un elenco di ruoli di piattaforma, diventa il **ruolo sull'applicazione corrente** più il ruolo di
piattaforma. È una modifica che tocca ogni modulo esistente e va fatta in una volta.

## 8. Permessi & gate

- **Difesa a due livelli**: il menu nasconde, la guardia di rotta rifiuta, il servizio nega. Nascondere una
  voce non è protezione.
- **Guardie da stringere**: quella di «Members» ammette oggi `owner` **o** `admin` e va portata a **solo
  owner**; Account e Billing oggi non hanno guardia e vanno protette
  ([routes.tsx](../../../../frontend/apps/backoffice/src/routing/routes.tsx)).
- **Nessuna decisione di autorizzazione nel frontend**: la shell rispecchia ciò che il core dice, non lo
  deduce.

## 9. Requisiti di test

- **Componente sul menu**: per ognuno dei quattro ruoli, l'insieme esatto delle voci e delle applicazioni
  visibili. È il collaudo più utile della storia, e va scritto come tabella dei casi.
- **Componente sulle guardie**: un collaboratore che raggiunge `/members`, `/billing`, `/account` finisce
  sulla pagina di rifiuto.
- **Componente sull'errore**: accessi non leggibili → messaggio di guasto, non elenco vuoto.
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/shell.spec.ts` (esistente, da
  estendere) per il menu del collaboratore.
- **Percorso di piattaforma**: parte del percorso «stessa applicazione vista dai quattro ruoli» (UC 0113).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [Sidebar.tsx](../../../../frontend/apps/backoffice/src/shell/Sidebar.tsx),
  [guards.tsx](../../../../frontend/apps/backoffice/src/routing/guards.tsx),
  [registry.ts](../../../../frontend/apps/backoffice/src/registry/registry.ts),
  [UC 0077](../../15-supporto-e-piattaforma/0077-provider-entitlement-reale.md) per la lezione sugli stati
  di errore; i quattro prototipi.
- **Definition of Done**:
  1. il menu è l'intersezione a tre e per l'owner resta completo;
  2. Account, Billing e Members sono assenti per i collaboratori e protetti da guardia;
  3. il ruolo sull'applicazione arriva ai moduli attraverso il contratto della shell;
  4. un guasto di lettura non si traveste da diniego;
  5. il collaudo a tabella copre i quattro ruoli;
  6. `run-tests.sh frontend` verde più i percorsi end-to-end aggiornati.

## Punti aperti / decisioni differite

- **Nessuna etichetta di ruolo nell'interfaccia di piattaforma** (decisione presa rivedendo i prototipi):
  il ruolo è **per applicazione**, quindi una etichetta globale nell'intestazione sarebbe falsa appena una
  persona è abilitata a più di una applicazione — «Admin del Mini-CRM, Viewer delle Note, Editor di
  Teams…» non è un'informazione, è un elenco. Il ruolo si legge dove è vero: sulla **scheda
  dell'applicazione** nel cruscotto e in **testa alle schermate** di quell'applicazione. Nulla da
  aggiungere a `shell/Topbar.tsx`: è una cosa da **non** fare, e per questo va scritta.
- **Selettore dell'account nella barra laterale**, sotto il marchio (da
  [UC 0117](0117-account-attivo-e-selettore.md)): l'account è il contesto del lavoro, come il menu che gli
  sta sotto, non un comando accessorio dell'intestazione. Tocca `shell/Sidebar.tsx`, che è materia di questo
  use case. Proprietario: UC 0117.

- **Impostazioni**: la pagina contiene oggi preferenze personali e potrebbe in futuro contenere
  impostazioni dell'account. Resta visibile a tutti; se un giorno accogliesse impostazioni dell'account,
  quella parte andrà riservata all'owner. Proprietario: questa storia.
- **Supporto**: resta visibile a tutti (un collaboratore deve poter chiedere aiuto). Da verificare che la
  richiesta di assistenza non riveli dati di fatturazione al collaboratore. Proprietario: UC 0075.
- **Ricerca globale** (UC 0088, non implementata): quando nascerà, dovrà rispettare gli accessi per
  applicazione. Annotato là.
