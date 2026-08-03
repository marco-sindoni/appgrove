# 0034 — Contratto degli strumenti di lettura

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0017`, `0020`, `0023`, `0030` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che comanda la propria azienda da una chat
> voglio poter chiedere a voce chi devo richiamare, com'è messa la pipeline e come va il trimestre
> così da avere le risposte mentre guido, senza aprire il portatile.

**Contesto.** Il requisito trasversale del catalogo è che ogni funzione sia comandabile da una chat. Il livello
conversazionale **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066): il compito di
questa storia non è costruire il server, ma **dichiarare il contratto** degli strumenti di lettura dentro il
servizio, versionato con esso. Le letture vengono prima delle scritture perché sono libere: nessuna di esse può
rompere niente.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sette strumenti di **sola lettura**: `list_contacts`, `get_contact`,
   `get_pipeline`, `list_deals`, `list_activities`, `summarize_account` (la cui logica è la storia 0036),
   `conversion_report`.
2. **RF-2** — Ogni strumento dichiara nome stabile, descrizione in lingua naturale, schema dei parametri, schema
   del risultato, marcatura **lettura** e idempotenza.
3. **RF-3** — I risultati sono **minimizzati**: `list_contacts` restituisce nome, azienda, ruolo e identificativo,
   e **non** i recapiti se non richiesti espressamente con un parametro apposito.
4. **RF-4** — `get_contact` restituisce anche lo **stato delle preferenze di contatto**: un assistente che
   suggerisce di richiamare qualcuno deve sapere se si può.
5. **RF-5** — Ogni strumento rispetta la stessa matrice dei ruoli dell'interfaccia: un `member` che chiede i numeri
   della squadra ottiene solo i propri.
6. **RF-6** — I parametri degli strumenti coincidono con i filtri delle schermate corrispondenti, così che chat e
   interfaccia diano la stessa risposta alla stessa domanda.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli strumenti operano nell'account del **chiamante autenticato**: nessun
  parametro di account esiste nel loro schema, per costruzione. È il presidio più importante della storia, perché
  un parametro di account in uno strumento conversazionale è una porta aperta.
- **RT-2 — Interfaccia di programmazione (§2).** Il contratto vive in `app.appgrove.sales.tools`, versionato con
  il servizio; gli strumenti riusano i servizi applicativi esistenti, non interrogazioni proprie — altrimenti si
  duplicano le regole di autorizzazione.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata. Nel blocco «Dalla chat» della panoramica compaiono
  esempi di domande, come promemoria di cosa si può chiedere.
- **RT-5 — Cinque lingue (§4).** Le **descrizioni** degli strumenti sono in inglese, perché sono destinate a un
  modello linguistico e non a un utente; i testi visibili nell'interfaccia che le citano sono nelle cinque lingue.
  Questa distinzione va scritta, altrimenti sembra una dimenticanza.
- **RT-6 — Varchi e quota (§6, §7).** Ogni chiamata di strumento attraversa la stessa catena di varchi delle
  rotte: `401` se non autenticato, `402` senza abbonamento, `403` senza posto o senza ruolo. La lettura non
  consuma quota.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza il requisito. Dipendenza dichiarata:
  UC 0061-0063 (architettura del server, autenticazione e consenso delegato, mappatura operazioni → strumenti),
  non ancora implementati: finché non esistono, il contratto è verificabile con prove ma non richiamabile da una
  chat vera.
- **RT-8 — Dati personali (§10).** La minimizzazione di RF-3 **è** una misura di protezione dei dati, non una
  scelta di comodità: un elenco di contatti con tutti i recapiti passato a un modello linguistico è un'uscita di
  dati personali molto più grande di quanto la domanda richiedesse. Nessuna voce nuova nel manifesto, ma il
  principio va dichiarato nella sua descrizione.
- **RT-9 — Registrazione eventi (§14).** Ogni chiamata di strumento è registrata con nome dello strumento,
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e numero di elementi restituiti; **mai** i
  parametri, che possono contenere nomi.

## 4. Criteri di accettazione

**CA-1 — Contratto completo**
- **Dato** il servizio avviato
- **Quando** si chiede l'elenco degli strumenti dichiarati
- **Allora** compaiono i sette, ognuno con schema dei parametri, schema del risultato e marcatura «lettura»

**CA-2 — Minimizzazione**
- **Dato** una chiamata a `list_contacts` senza chiedere i recapiti
- **Quando** risponde
- **Allora** il risultato non contiene indirizzi di posta né numeri di telefono

**CA-3 — Nessun parametro di account**
- **Dato** lo schema di ciascuno dei sette strumenti
- **Quando** lo si ispeziona
- **Allora** non esiste alcun parametro che indichi l'account: l'account viene dal chiamante

**CA-4 — Matrice dei ruoli rispettata**
- **Dato** un utente con ruolo `member`
- **Quando** chiama `conversion_report` chiedendo i numeri di tutta la squadra
- **Allora** riceve solo i propri

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiama `get_contact` con l'identificativo di un contatto di `B`
- **Allora** riceve un esito «non trovato», identico a quello di un identificativo inesistente

**CA-6 — I registri non contengono parametri**
- **Dato** una chiamata a `list_contacts` con un termine di ricerca
- **Quando** si ispezionano le righe di registro
- **Allora** contengono il nome dello strumento e il numero di risultati, non il termine

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla minimizzazione dei risultati e di **integrazione** su ciascuno dei sette strumenti;
- [ ] prova di **isolamento fra account** su ogni strumento, con tentativi di indicare risorse altrui;
- [ ] prova della **matrice dei ruoli** applicata agli strumenti;
- [ ] **prova end-to-end**: rimando alla storia 0037 per la parte di superficie; qui la copertura è d'integrazione,
      con il motivo annotato nel registro;
- [ ] **traduzioni**: nessun testo visibile nuovo, con la nota sulla lingua delle descrizioni degli strumenti;
- [ ] **manifesto dei dati**: nessuna voce nuova; principio di minimizzazione dichiarato nella descrizione;
- [ ] **registro delle decisioni** compilato, con annotata l'assenza di un parametro di account per costruzione;
- [ ] contratto degli **strumenti conversazionali** dichiarato e versionato dentro il servizio;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0008`, `0017`, `0020`, `0023`, `0030` | Gli strumenti espongono funzioni che devono già esistere |
| UC 0061-0063 (livello conversazionale di piattaforma) | Non implementati: il contratto si dichiara e si prova, ma non è richiamabile da una chat vera finché non esistono |

## 7. Fuori ambito

- il server del livello conversazionale: è di piattaforma;
- gli strumenti di scrittura: storia 0035;
- la logica del riassunto: storia 0036.

## 8. Punti aperti

- **Come si comportano gli strumenti quando il livello conversazionale arriverà davvero.** Consenso delegato,
  limiti di frequenza per assistente e tracciamento delle chiamate sono di piattaforma (UC 0062, 0064, 0065): qui
  si dichiara il contratto, non le regole d'uso.
