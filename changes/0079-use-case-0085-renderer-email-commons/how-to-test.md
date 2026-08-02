# Come verificare a mano la change 0079 — Renderer email unico in `services/commons`

Questa change **non aggiunge niente di nuovo da guardare**: unisce in una sola classe la logica con cui due servizi
diversi compongono le email. Il criterio di riuscita è quindi al contrario del solito — **non deve cambiare niente**.
La verifica consiste nel far partire davvero tutte le email della piattaforma e guardarle con i propri occhi in
Mailpit: devono arrivare, essere nella lingua giusta, avere il collegamento funzionante e la stessa identica forma
grafica di prima.

Tutti i comandi partono dalla radice del repository, sul branch `change/0079-use-case-0085-renderer-email-commons`.

Le email toccate sono **quattro**, ed è bene provarle tutte perché prima erano rese da **due programmi diversi**:

| Email | Servizio che la spedisce | Da dove si scatena |
|---|---|---|
| Verifica indirizzo | `auth` | registrazione dal backoffice |
| Reimpostazione password | `auth` | «Password dimenticata» |
| Invito in un workspace | `auth` | pagina Membri → invita |
| Conferma iscrizione newsletter | `core` | modulo del sito vetrina |

---

## 0. Avvio dello stack locale

**Azione**

```bash
./app-start.sh
```

**Risultato atteso** — al termine il riepilogo elenca il backoffice su `https://app.local.appgrove.app`, il sito
vetrina su `http://localhost:4321` (la porta esatta è nel riepilogo) e **Mailpit su `http://localhost:8025`**, che è
la casella dove finiscono tutte le email in locale. Gli utenti di prova hanno password `Password1!`
(`owner@acme.test`, `admin@acme.test`, …).

> Se il browser protesta per il certificato di `app.local.appgrove.app`, è il proxy locale: accetta l'eccezione una
> volta sola.

**Perché conta per questa change**: se i template non finissero più nell'artefatto di un servizio, quel servizio
fallirebbe **all'avvio**. Uno stack che parte pulito è già la prima prova.

**Azione** — apri `http://localhost:8025` e lascia la finestra aperta di fianco: è lì che guarderai ogni email.
Comincia da una casella vuota (in alto a destra, «Delete all»), così non confondi le email vecchie con le nuove.

---

## 1. Email di verifica indirizzo — in inglese

**Azione** — apri `https://app.local.appgrove.app/signup`, lascia l'interfaccia in **inglese** e registra un indirizzo
nuovo, per esempio `prova-en@test.local` con password `Password1!`.

**Risultato atteso in Mailpit** — arriva **una** email a quell'indirizzo. Aprendola devi vedere, **con gli occhi**:

- **oggetto**: `Confirm your email address`;
- nella scheda **HTML** (quella che Mailpit mostra per prima): il marchio **appgrove** in testa, un titolo, un
  paragrafo di benvenuto, un **pulsante** con scritto `Confirm your email address` e, sotto, la frase «If the button
  does not work…» seguita dall'indirizzo per esteso;
- il **pulsante deve essere cliccabile e portare da qualche parte**: cliccalo. Il browser deve aprire il backoffice
  sulla pagina di verifica e l'account deve risultare verificato (entri e vedi la dashboard).

**Perché questo è IL controllo importante di questa change**: il collegamento di verifica contiene una **e
commerciale** fra i parametri. Se la protezione dei caratteri speciali nella versione grafica si fosse persa nella
rifattorizzazione, il pulsante porterebbe a un indirizzo **troncato** e la verifica fallirebbe. Un pulsante che
funziona è la prova che quella protezione c'è ancora.

**Azione** — nella stessa email passa alla scheda **Text** di Mailpit.

**Risultato atteso** — la versione testuale ha lo stesso contenuto in forma semplice e porta l'indirizzo **in chiaro,
non trasformato**: nessun `&amp;` deve comparire qui (le e commerciali si scrivono così solo nella versione grafica).
Copiando quell'indirizzo nella barra del browser, la verifica deve funzionare ugualmente.

---

## 2. La stessa email in italiano

**Azione** — torna su `/signup`, cambia la lingua dell'interfaccia in **italiano** con il selettore in alto, e registra
un altro indirizzo nuovo (`prova-it@test.local`).

**Risultato atteso in Mailpit**

- **oggetto**: `Conferma il tuo indirizzo email`;
- il corpo è **tutto in italiano** (nessuna frase rimasta in inglese, incluso il pulsante e la riga di chiusura);
- **nessuna parentesi graffa doppia** da nessuna parte: se vedessi scritto `{{qualcosa}}` nel testo, sarebbe un buco
  del modello rimasto senza valore — la guardia dovrebbe impedirlo, ma è esattamente ciò che si controlla a occhio.

**Azione** — prova anche una lingua che non copriamo: registra un terzo indirizzo con l'interfaccia impostata su una
lingua diversa da inglese e italiano (per esempio **Deutsch**, se presente nel selettore).

**Risultato atteso** — l'email arriva comunque, **in inglese**. È il ripiego voluto: un'email non spedita bloccherebbe
una registrazione.

---

## 3. Email di reimpostazione password

**Azione** — esci, vai su `https://app.local.appgrove.app/login`, clicca «Password dimenticata» e chiedi la
reimpostazione per `owner@acme.test`.

**Risultato atteso in Mailpit** — arriva l'email di reimpostazione (oggetto sul tema «reset»/«reimposta», secondo la
lingua dell'interfaccia). Deve avere **la stessa identica impaginazione** dell'email di verifica: stesso marchio in
testa, stesso stile di pulsante, stessa riga di chiusura. Cliccando il pulsante si apre la pagina di reimpostazione e
la nuova password funziona all'accesso successivo.

---

## 4. Email di invito in un workspace

**Azione** — entra come `owner@acme.test`, vai nella pagina **Membri** del workspace e invita un indirizzo nuovo
(`invitato@test.local`) scegliendo un ruolo.

**Risultato atteso in Mailpit** — arriva l'email di invito e nel testo **compare il ruolo che hai scelto** (è un valore
dinamico inserito nel modello: se leggessi `{{role}}` invece del ruolo, sarebbe un difetto). Il pulsante porta alla
pagina di accettazione dell'invito, dove l'invitato può scegliere la password ed entrare.

---

## 5. Email di conferma della newsletter — l'altro servizio

Fin qui tutte le email venivano dal servizio di autenticazione. Questa viene dal **core**, cioè dall'altro programma
che prima aveva la sua copia della logica: è la metà del confronto che rende questa change interessante.

**Azione** — apri il **sito vetrina** (l'indirizzo è nel riepilogo di `app-start.sh`, tipicamente
`http://localhost:4321`), scorri fino al modulo di iscrizione alla newsletter e iscrivi `newsletter-it@test.local`
**dalla versione italiana del sito**.

**Risultato atteso in Mailpit** — arriva l'email di conferma, **in italiano**, e — questo è il punto — con **la stessa
impaginazione** delle email di autenticazione: stesso marchio, stesso pulsante, stessa riga di chiusura. Metti le due
finestre di Mailpit una accanto all'altra (email di verifica e email di newsletter): devono sembrare la stessa
famiglia, perché ora le compone lo stesso codice.

**Azione** — clicca il pulsante di conferma.

**Risultato atteso** — si apre una pagina del backend che conferma l'iscrizione **in italiano**.

**Azione** — ripeti l'iscrizione da un'altra lingua del sito (per esempio **français**, con un indirizzo nuovo).

**Risultato atteso** — l'email arriva **in inglese**: la newsletter copre inglese e italiano, e tutto il resto ripiega
sull'inglese. Anche la pagina di conferma è in inglese.

---

## 6. Controlli che gli occhi non possono fare

### 6.1 Le email sono nel database e i servizi non hanno registrato errori

**Azione**

```bash
docker exec -i appgrove-dev-postgres-1 psql -U appgrove -d appgrove \
  -c "select email, status, locale from platform.newsletter_subscriber order by created_at desc limit 5;"
```

**Risultato atteso** — gli iscritti creati al punto 5 ci sono, con `status` passato a `confirmed` per quello che hai
confermato e la **colonna `locale` coerente** con la lingua del sito da cui ti sei iscritto (`it` per l'italiano, `en`
per il francese: la lingua non coperta viene ricondotta al ripiego, che è la stessa scelta usata per comporre
l'email).

> Se il nome del contenitore Postgres non corrisponde, ricavalo con `docker ps --format '{{.Names}}'`.

**Azione** — guarda i log del core mentre confermi l'iscrizione:

```bash
grep -i "newsletter.confirm" dev/.run/core.log | tail -5
```

**Risultato atteso** — righe `newsletter.confirm.sent subscriber_id=…`. **Non** deve comparire
`invio email fallito`: quel messaggio significherebbe che la resa dell'email ha sollevato un errore (l'invio è
volutamente indulgente e non blocca l'iscrizione, quindi il difetto sarebbe **invisibile dall'interfaccia** — è il
motivo per cui questo controllo non è facoltativo).

### 6.2 Nessun buco di modello è arrivato in casella

**Azione** — con Mailpit ancora aperto, usa la ricerca in alto e cerca `{{`.

**Risultato atteso** — **zero risultati**. Un segnaposto rimasto significherebbe che la guardia sui buchi non risolti
non funziona più.

### 6.3 I due servizi trovano davvero i template nel proprio artefatto

**Azione**

```bash
unzip -l services/auth/target/quarkus-app/app/*.jar | grep email-templates
unzip -l services/core/target/quarkus-app/app/*.jar | grep email-templates
```

**Risultato atteso** — in **entrambi** compaiono `email-templates/layout.html`, `email-templates/layout.txt`,
`email-templates/en.json` e `email-templates/it.json`. È la verifica che la scelta presa in questa change (i testi
restano copiati nell'artefatto di ciascun servizio, non dentro la libreria condivisa) sia rimasta vera.

### 6.4 La libreria condivisa NON si porta dietro i testi

**Azione**

```bash
unzip -l services/commons/target/commons-0.1.0.jar | grep -c email-templates || echo "0 — corretto"
```

**Risultato atteso** — **zero occorrenze**: in `services/commons` i template sono copiati solo fra le risorse di
collaudo, non nell'artefatto spedito. Se ne comparissero, esisterebbe una terza copia dei testi in circolazione — la
duplicazione che questa change esiste per eliminare.

---

## 7. La prova che davvero non è cambiato niente

Il collaudo di parità carattere per carattere è già stato eseguito **dentro la change**: 37 casi (le tre email di
autenticazione e la conferma newsletter, in inglese, italiano, in una lingua non coperta, nelle varianti regionali,
con la lingua assente) sono stati resi dal codice **prima** e dal codice **dopo**, e i due insiemi risultano identici.
L'esito è registrato in `decisions.json` alla voce 16.

Se vuoi rifare il confronto a mano dopo altre modifiche, il modo è quello: catturare l'output su un insieme di casi
prima di toccare il codice e riconfrontarlo dopo. Non esiste un test permanente che lo faccia, ed è voluto — file di
riferimento congelati diventerebbero rossi ogni volta che si corregge legittimamente una frase di un'email.

---

## Riepilogo: cosa deve essere vero alla fine

- [ ] Lo stack parte pulito: nessun servizio fallisce all'avvio per template mancanti.
- [ ] Le **quattro** email arrivano tutte in Mailpit: verifica, reimpostazione, invito, conferma newsletter.
- [ ] Ognuna è nella **lingua giusta**, con ripiego sull'inglese per le lingue non coperte.
- [ ] Ogni **pulsante funziona**: il collegamento non è troncato dalla e commerciale.
- [ ] La versione testuale porta l'indirizzo in chiaro; la versione grafica lo porta protetto.
- [ ] Il **ruolo** compare davvero nell'email di invito.
- [ ] Le email dei **due servizi diversi** hanno la stessa identica impaginazione.
- [ ] Nessun `{{segnaposto}}` in nessuna email.
- [ ] Nessun `invio email fallito` nei log del core.
