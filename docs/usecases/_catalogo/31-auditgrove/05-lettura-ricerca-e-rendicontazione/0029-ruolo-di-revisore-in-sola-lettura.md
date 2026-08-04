# 0029 — Ruolo di revisore in sola lettura

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 05 — Lettura, ricerca e rendicontazione
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve far guardare il registro a qualcuno di esterno — un consulente, un revisore, il tecnico
> di un cliente che chiede conto
> voglio poter dare un accesso che vede tutto e non può cambiare niente
> così da non dovergli passare le mie credenziali né dovergli concedere il potere di modificare le regole che sto
> facendo verificare.

**Contesto.** Fino a questa storia i ruoli dell'app sono quelli comuni della piattaforma: proprietario,
amministratore, membro. Manca la figura che questa app, più di ogni altra, richiede: **chi guarda senza toccare**.
Il motivo non è di comodo, è strutturale — far verificare il proprio registro da qualcuno a cui si è dato il potere
di riscrivere le regole del registro svuota la verifica. Se il revisore può cambiare la regola di uno strumento,
allora il fatto che quella regola fosse attiva nel periodo verificato non è più un fatto.

C'è anche una ragione commerciale: il piano più alto della proposta di listino (§5 della descrizione
dell'applicazione) ha proprio in questo ruolo una delle sue tre ragioni d'essere.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il ruolo **revisore**, che può: consultare la cronologia (storia 0024), aprire le schede
   delle azioni (storia 0025), eseguire la verifica di integrità (storia 0014), consultare e generare rapporti
   (storia 0028), produrre esportazioni (storia 0027) e pacchetti di prova (storia 0015), vedere le regole e la
   loro storia (storia 0019), vedere gli avvisi (storia 0026).
2. **RF-2** — Il revisore **non può**: cambiare o creare regole, decidere approvazioni, registrare o revocare
   sorgenti, cambiare le soglie degli avvisi, attivare la conservazione dei contenuti, rivelare un contenuto
   conservato, cambiare la programmazione dei rapporti, gestire l'abbonamento.
3. **RF-3** — Esiste una **matrice dei ruoli** esplicita e verificata da una prova automatica, che elenca per
   ciascuna operazione dell'app quali fra proprietario, amministratore, membro e revisore possono compierla.
4. **RF-4** — Un tentativo di operazione non consentita risponde `403` con un messaggio che dice quale ruolo
   servirebbe, senza rivelare dati che il richiedente non potrebbe vedere.
5. **RF-5** — L'attribuzione e la revoca del ruolo di revisore sono **righe del registro**: chi ha dato accesso al
   registro a chi, e quando, fa parte della storia dei fatti.
6. **RF-6** — L'interfaccia mostra al revisore soltanto ciò che può fare: i comandi che non gli competono non
   compaiono disabilitati, non compaiono affatto — un comando disabilitato è un invito a chiedere perché.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il ruolo è per account: un revisore di `A` non è revisore di `B`, e il
  `tenant_id` continua ad arrivare solo dal token verificato. Il ruolo **non** amplia mai l'insieme visibile oltre
  l'account: è una restrizione, mai un'estensione.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova di dominio: la storia applica i controlli di
  ruolo alle rotte esistenti, in modo dichiarativo e non sparso nel codice. Errori in `application/problem+json`
  con il codice `403`; definizione OpenAPI aggiornata per dichiarare i ruoli richiesti da ciascuna operazione.
- **RT-3 — Persistenza (§8).** Nessuna tabella di dominio nuova se il ruolo è di piattaforma; se invece è un
  permesso interno all'app (vedi §8), serve la migrazione `V…__ruoli_app.sql` sullo schema `app_agentaudit` con la
  tabella dell'attribuzione, con `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica.
  La scelta fra i due impianti va fatta **prima** di scrivere codice, perché cambia dove vive il dato.
- **RT-4 — Modulo frontend (§3, §5).** Il modulo `agentaudit` compone le sezioni e i comandi in base al ruolo
  ricevuto dal contesto della shell: il modulo **non** decide da sé chi è l'utente e non legge il token. Solo token
  del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Il nome del ruolo, la sua descrizione e i messaggi di rifiuto passano dallo
  spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Il ruolo è il **quarto** varco della catena comune (`401` senza token, `403`
  ad app spenta, `402` senza abbonamento, `403` per ruolo insufficiente, `429` a quota esaurita): questa storia
  riempie quel varco per tutte le operazioni dell'app. Il revisore **non consuma** quota, perché non registra
  azioni: consuma solo la riga di registro della propria attribuzione (RF-5).
- **RT-7 — Esposizione conversazionale (§12).** I controlli di ruolo valgono **identici** per le chiamate che
  arrivano da un assistente: il livello conversazionale non è una porta di servizio. La verifica sistematica è la
  storia 0036; qui si stabilisce che la matrice è una sola e vale per tutti i canali.
- **RT-8 — Dati personali (§10).** L'attribuzione del ruolo collega un identificativo di utente a un account: è un
  dato personale già trattato dalla piattaforma se il ruolo è di piattaforma, oppure una voce nuova nel manifesto
  `docs/compliance/manifests/agentaudit.yaml` in italiano e inglese se il ruolo è interno all'app — con il campo
  annotato e la tabella presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Attribuzione, revoca e tentativi respinti per ruolo insufficiente sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il revisore vede tutto**
- **Dato** un utente con ruolo di revisore
- **Quando** apre cronologia, scheda di un'azione, verifica di integrità, rapporti ed esportazioni
- **Allora** vi accede regolarmente, con gli stessi dati che vedrebbe un amministratore, salvo i contenuti
  conservati

**CA-2 — Il revisore non tocca le regole**
- **Dato** lo stesso utente
- **Quando** tenta di cambiare la regola di uno strumento, di decidere un'approvazione o di registrare una
  sorgente
- **Allora** ogni tentativo riceve `403` con l'indicazione del ruolo necessario, e nulla viene modificato

**CA-3 — La matrice dei ruoli è verificata, non descritta**
- **Dato** l'elenco delle operazioni dell'app
- **Quando** si esegue la prova della matrice dei ruoli
- **Allora** per ciascuna operazione e ciascuno dei quattro ruoli il risultato atteso è verificato, e
  l'introduzione di una operazione nuova senza la sua riga nella matrice fa fallire la prova

**CA-4 — L'attribuzione lascia traccia**
- **Dato** un proprietario che attribuisce il ruolo di revisore a una persona e glielo revoca due settimane dopo
- **Quando** si guarda la cronologia
- **Allora** compaiono due righe, con chi, a chi e quando, e non si possono cancellare

**CA-5 — Isolamento fra account**
- **Dato** una persona che è revisore nell'account `A` e non ha alcun ruolo in `B`
- **Quando** tenta di leggere il registro di `B`
- **Allora** non vi accede, e il rifiuto non rivela l'esistenza dell'account `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione dei permessi e di **integrazione** sulle rotte protette, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account e matrice dei ruoli**, obbligatoria e mai esclusa dai filtri di percorso,
      con almeno due account e tutti e quattro i ruoli su ogni operazione;
- [ ] **prova end-to-end**: **rimando** — il percorso `[J-AGENTAUDIT]` della storia 0037 percorre l'app con il
      ruolo pieno; la variante con ruolo di revisore è dichiarata come voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia proprietaria `0037`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese se il ruolo è interno all'app; se è di piattaforma,
      il fatto è dichiarato invece che sottinteso;
- [ ] **registro delle decisioni** compilato, con la voce sulla scelta fra ruolo di piattaforma e permesso interno
      e il motivo;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che la matrice vale identica per il canale
      conversazionale;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | Non si può proteggere la modifica delle regole prima che le regole esistano |
| storia `0024` | Il revisore serve a guardare: senza cronologia non c'è niente da guardare |
| storie `0014`, `0015`, `0027`, `0028` | Sono le operazioni che il revisore deve poter compiere; se una non esiste ancora, la sua riga nella matrice si aggiunge con lei |
| Modello dei ruoli di piattaforma | La scelta fra ruolo di piattaforma e permesso interno all'app non la decide questa storia (vedi §8) |

## 7. Fuori ambito

- **l'accesso di un revisore esterno senza account** (per esempio con un collegamento a scadenza): sarebbe comodo
  e apre una superficie di sicurezza nuova; non ora;
- **i permessi per singola sorgente** («questo revisore vede solo l'agente di fatturazione»): granularità che
  nessuna fonte dell'analisi ha chiesto, rimandata;
- **la delega temporanea** del ruolo con scadenza automatica: utile, non essenziale al primo passo;
- **la rivelazione dei contenuti conservati**: il revisore non ce l'ha, e la decisione su quale ruolo ce l'abbia è
  della storia 0031 (attivazione) insieme alla 0025 (rivelazione).

## 8. Punti aperti

- **Se il revisore sia un ruolo di piattaforma nuovo o un permesso interno all'app.** È la decisione che questa
  storia **non** prende. Ruolo di piattaforma: coerente con proprietario/amministratore/membro, riutilizzabile da
  altre app, ma tocca il modello degli account e degli inviti, che è di piattaforma. Permesso interno: più veloce,
  ma crea una seconda nozione di ruolo e prima o poi un'altra app ne vorrà una terza. **La mia raccomandazione è
  il ruolo di piattaforma**, perché il bisogno «guarda e non tocca» non è peculiare di AuditGrove; ma è una scelta
  che eccede questa app. Chi chiude: piattaforma, insieme allo sviluppatore.
- **Se il revisore possa produrre esportazioni.** Gli serve davvero (deve portarsi via ciò che verifica) ed è
  anche il modo più semplice per portare fuori identificativi di persone. Propongo di consentirlo tracciando ogni
  esportazione (storia 0027, requisito 4), ma il titolare potrebbe volerlo escludere: potrebbe diventare una
  impostazione per account, e in quel caso è una decisione dello sviluppatore.
