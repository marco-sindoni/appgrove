# Change 0085: Skill `new-blog-post` — scaffolding dei post del blog nelle 5 lingue

**Branch**: `change/0085-use-case-0084-skill-new-blog-post`
**Aree**: `tools/new-blog-post` (nuovo strumento Node), `.claude/skills/new-blog-post` (nuova skill), `run-tests.sh`, documentazione
**Data**: 2026-08-02
**Autore**: Platform Engineering (modalità fast)
**Use case sorgente**: [docs/usecases/17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md](../../docs/usecases/17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md)
**Tocca dati personali?**: No — genera contenuti pubblici del sito vetrina, nessuna tabella a runtime, nessun manifesto dati.

## Problema / Obiettivo

Il motore del blog del sito vetrina esiste (UC 0042): registro dei contenuti, tipi che impongono la parità
delle 5 lingue a tempo di compilazione, validazione a rete e pagine che li rendono. Quello che **non** esiste
è il modo di aggiungerci un articolo senza lavoro a mano: oggi bisogna creare a mano una cartella con sei
file, ricopiare la forma da un post esistente, appendere due righe al registro e ricordarsi di agganciare i
riferimenti reciproci fra il pilastro e il suo articolo cluster. Sono esattamente i passi in cui un umano
sbaglia in silenzio — un riferimento reciproco dimenticato non rompe la compilazione, rompe solo i
collegamenti interni, cioè proprio la ragione per cui il blog esiste.

Obiettivo: rendere l'aggiunta di un articolo un'operazione **ripetibile e verificata**, separando nettamente
ciò che una macchina fa meglio (creare i file, appendere al registro, agganciare pilastro e cluster, rifiutare
uno slug in conflitto) da ciò che solo una persona assistita può fare (scegliere il pilastro, decidere il
taglio, scrivere la copy on-brand nelle 5 lingue). Al termine, chi scrive un articolo invoca
`/new-blog-post`, risponde all'intervista editoriale, rilegge la copy e ottiene un branch pronto: nessuna
riga incollata a mano nel registro.

## Scope

**1. Generatore deterministico `tools/new-blog-post/`** (Node, stessa forma di `tools/finalize-landing` e
`tools/pricing-change`, provato da `node --test`):

- prende in ingresso **una specifica in formato JSON** scritta dal co-pilota — uno o più post, ciascuno con
  identità (chiave, tipo pilastro/articolo, data, app di destinazione, eventuale pilastro di appartenenza) e
  il contenuto editoriale nelle 5 lingue;
- **valida prima di scrivere**: forma della specifica completa nelle 5 lingue, nessuna stringa vuota, data nel
  formato `AAAA-MM-GG`, slug ben formati (minuscolo, cifre e trattini), non riservati, non già usati da un
  altro post nella stessa lingua, chiave non già presente, pilastro dichiarato esistente e davvero di tipo
  pilastro, app di destinazione collegata a una landing **pubblicata**;
- **scrive** la cartella `site/src/content/blog/<chiave>/` con i cinque file-lingua e il file di identità,
  appende importazione ed entry nel registro `site/src/content/blog/index.ts`, e **aggancia i riferimenti
  reciproci**: la chiave del nuovo articolo entra nella lista dei cluster del pilastro, il pilastro viene
  scritto sull'articolo;
- **crea il pilastro se assente**: una specifica può contenere il nuovo pilastro e il suo primo articolo, e
  vengono materializzati nella stessa esecuzione, già coerenti fra loro;
- **rifiuta in modo pulito**: se un solo controllo fallisce non scrive nulla, e se la scrittura fallisce a
  metà ripristina lo stato di partenza — rieseguire su una chiave o uno slug esistente lascia il registro
  identico;
- offre l'**inverso** (rimozione di un post) per il ripristino e per il collaudo andata-ritorno, e un comando
  di **elenco** dei pilastri e degli articoli esistenti, che è ciò che il co-pilota legge per proporre dove
  collocare il pezzo.

**2. Skill `new-blog-post`** (`.claude/skills/new-blog-post/`): il co-pilota, sulla falsariga di
`finalize-landing` — file principale più i passi. Conduce l'intervista editoriale (pilastro esistente o nuovo,
taglio del pezzo, domanda-guida, app di destinazione), redige la copy on-brand nelle 5 lingue partendo
dall'inglese, risolve il collegamento interno alla landing giusta, invoca il generatore, verifica con
`./run-tests.sh site` e chiude via `new-change`. Include i presidi previsti dallo use case: **si ferma e
chiede** quando la scelta è aprire un nuovo pilastro (è una linea editoriale, quindi direzione di prodotto) e
quando la copy va approvata prima della pubblicazione.

**3. Cancello automatico**: il nuovo strumento entra nell'area `tooling` di `run-tests.sh`, insieme agli altri
collaudi di skill. Fra i suoi test c'è un **allarme di deriva del contratto**: i campi che il generatore
scrive vengono confrontati con quelli dichiarati dai tipi del blog, così se il contratto di UC 0042 evolve la
suite diventa rossa invece di lasciare un generatore che scrive post di forma vecchia.

**4. Documentazione**: stato dello use case 0084 aggiornato con l'esito, voce del backlog delle skill chiusa,
registro delle epiche allineato.

## Fuori scope

- **Scrivere un articolo vero** con questa change: la copy è contenuto di marketing, e sceglierne il tema è
  direzione editoriale, non lavoro di piattaforma. Lo strumento e la skill si collaudano su cartelle di prova.
- **Il motore del blog** (registro, tipi, validazione, pagine, indice, dati strutturati): esiste già ed è di
  UC 0042. Questa change lo **consuma**, non lo modifica.
- **La pubblicazione**: la fa l'integrazione continua al merge (UC 0036). Né lo strumento né la skill fanno
  distribuzione.
- **Le landing per-app** (UC 0057) e i contenuti istituzionali del sito (UC 0037).
- **Traduzione automatica delle 5 lingue da parte dello strumento**: la copy è del co-pilota, sempre; lo
  strumento rifiuta una specifica incompleta invece di inventare una lingua mancante.

## Criteri di accettazione

- [ ] Data una specifica valida, `tools/new-blog-post` crea la cartella del post con i 5 file-lingua e il file
      di identità, appende l'entry al registro del blog e aggancia i riferimenti reciproci pilastro↔cluster;
      con una specifica che contiene pilastro e articolo, entrambi nascono coerenti nella stessa esecuzione.
- [ ] Una specifica non valida (lingua mancante, stringa vuota, slug malformato o riservato, slug o chiave già
      usati, pilastro inesistente o di tipo sbagliato, app senza landing pubblicata) viene **rifiutata prima di
      scrivere**, con un messaggio che dice cosa correggere; il registro resta identico byte a byte.
- [ ] Il ciclo andata-ritorno è simmetrico: generare un post e poi rimuoverlo riporta i file toccati allo stato
      esatto di partenza.
- [ ] Esiste la skill `new-blog-post` che conduce l'intervista, redige la copy nelle 5 lingue con il
      collegamento interno alla landing corretta, chiama il generatore e chiude via `new-change`; si ferma e
      chiede prima di aprire un nuovo pilastro e prima di pubblicare la copy.
- [ ] `run-tests.sh` esegue i test del nuovo strumento nell'area `tooling`, e la suite **completa** è verde.
- [ ] Se i campi dei tipi del blog cambiano senza che il generatore sia riallineato, la suite diventa rossa.

## Invarianti appgrove toccati

Nessuno degli invarianti multi-tenancy si applica: lo strumento genera **contenuti statici** del sito vetrina,
non esegue query, non ha richieste con token e non scrive log applicativi. L'invariante di piattaforma che
questa change deve rispettare è un'altra, e vale in pieno: la **fonte unica**. Il generatore non reintroduce
regole proprie di contenuto o di stile — i colori e la resa restano del brand kit condiviso (UC 0086) via il
template del blog, la validazione autorevole resta in `site/src/lib/blog.ts` (UC 0042), e lo strumento
controlla solo ciò che gli serve per non corrompere il registro.

## Requisiti di test

- Validazione della specifica: un caso per ogni motivo di rifiuto elencato nei criteri di accettazione.
- Scrittura su una copia di prova del sito: file creati, registro aggiornato, riferimenti reciproci coerenti.
- Rifiuto senza effetti collaterali: dopo un rifiuto, confronto byte a byte dei file del registro.
- Andata-ritorno genera → rimuovi: i file tornano identici.
- Allarme di deriva: i campi scritti dal generatore corrispondono a quelli dichiarati dai tipi di UC 0042.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì — il generatore **consuma** il contratto dei contenuti blog di UC 0042 (tipi + registro); la dipendenza è presidiata dall'allarme di deriva |
| Version bump | nessuno |
