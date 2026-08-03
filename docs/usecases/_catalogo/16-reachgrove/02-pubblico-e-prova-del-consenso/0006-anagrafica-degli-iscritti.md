# 0006 — Anagrafica degli iscritti

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che risponde di quello che il suo archivio contiene
> voglio vedere le persone iscritte alle mie liste con il loro recapito, la loro lingua e il loro stato
> così da sapere in ogni momento **a chi** posso scrivere, prima ancora di scrivere qualcosa.

**Contesto.** È la prima entità dell'app e quella che decide il carattere del prodotto. La tentazione, in questo
punto, è di fare come fanno quasi tutti: una tabella di indirizzi con una casella «iscritto sì/no» che qualcuno
spunta. Quella casella è esattamente ciò che in un accertamento non vale niente, perché non dice *quando*, *come* e
*con quale testo* la persona ha detto di sì ([application-description.md](../application-description.md) §2.3
punto 1). Questa storia crea quindi la scheda dell'iscritto **senza** dare a nessuno il modo di dichiararlo
contattabile: lo stato è una conseguenza delle registrazioni di consenso (storia 0007) e degli eventi di recapito,
mai un campo che si sceglie da un elenco a discesa. Va fatta adesso perché tutte le altre storie dell'epica le
scrivono sopra.

## 2. Requisiti funzionali

1. **RF-1** — Un iscritto ha un indirizzo di posta elettronica (obbligatorio, univoco nell'account), un numero di
   telefono facoltativo, nome e cognome facoltativi, una lingua fra le cinque dell'interfaccia e un insieme di
   campi personalizzati definiti dal cliente.
2. **RF-2** — L'iscritto ha uno **stato per canale** con cinque valori possibili: `in attesa di conferma`,
   `attivo`, `in quarantena`, `disiscritto`, `soppresso`. Lo stato è **calcolato** e di sola lettura: nessuna
   rotta, nessun campo del modulo e nessuna importazione lo possono impostare.
3. **RF-3** — La regola di calcolo è unica e scritta in un solo posto: `soppresso` se il recapito è nell'elenco di
   soppressione (storia 0011); altrimenti il risultato dell'ultima registrazione di consenso per quel canale
   (storia 0007); in assenza di registrazioni, `in quarantena`.
4. **RF-4** — Un iscritto creato a mano dall'interfaccia nasce `in quarantena` finché non gli si allega una prova
   di consenso: crearlo non è dichiararlo contattabile.
5. **RF-5** — L'elenco degli iscritti si cerca per recapito e per nome, si filtra per stato, lingua e origine, e
   mostra in testa **quanti** iscritti sono davvero contattabili sul canale della posta elettronica, non solo
   quanti ce ne sono.
6. **RF-6** — La scheda dell'iscritto mostra, accanto a ogni recapito, lo stato con il **motivo** in una riga
   («in quarantena: importato senza prova di consenso il 12/07/2026»), non un'etichetta muta.
7. **RF-7** — Un iscritto si può eliminare; l'eliminazione **non** rimuove le sue registrazioni di consenso né la
   sua eventuale soppressione, che restano perché sono la prova di ciò che è successo prima.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `subscriber` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato.
  L'unicità del recapito è **dentro l'account**: due account diversi possono avere lo stesso indirizzo e non
  devono saperlo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/subscribers`,
  `GET|PATCH|DELETE /api/campaigns/v1/subscribers/{id}`. Il corpo del `PATCH` **non accetta** il campo di stato:
  se arriva, la richiesta è respinta con `400` in `application/problem+json` e un messaggio che dice dove si
  registra il consenso. Definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `subscriber` già creata dalla storia 0002 sullo schema `app_campaigns`; qui
  si aggiungono l'indice univoco su (`tenant_id`, recapito normalizzato) e l'indice di ricerca. Chiave primaria
  UUID versione 7, colonne di controllo, cancellazione logica. La normalizzazione del recapito (minuscolo, spazi
  tolti) serve a impedire che lo stesso indirizzo entri due volte e sfugga a una soppressione.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Iscritti» del modulo `campaigns`: elenco con ricerca e filtri,
  scheda di dettaglio, modulo di creazione. Dati letti con il client generato dalla definizione delle interfacce;
  solo token del sistema di design, colore-categoria `violet`; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresi i nomi dei cinque stati e i motivi — passano
  dallo spazio-nomi `campaigns` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: gli iscritti archiviati **non** consumano
  `messages_sent`, ed è una scelta di posizionamento del listino (§5 della descrizione), non una dimenticanza.
  Con abbonamento `canceled` la sezione risponde `402`; l'esportazione dei dati resta comunque accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia rende possibile lo strumento `stato_iscritto`
  (`(recapito) → contattabile sì/no, canale per canale, con il motivo`), marcato **lettura**, che si dichiara
  nella storia 0034. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non è
  ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** È la storia che rende vere le voci `subscriber.email`, `subscriber.phone`,
  `subscriber.name` e `subscriber.custom_fields` del manifesto `docs/compliance/manifests/campaigns.yaml`, in
  italiano e inglese; i campi Java sono annotati `@PersonalData`; la tabella `subscriber` entra in `exportData` e
  in `purgeData`. La cancellazione è **fisica**: sostituire l'indirizzo con un codice non è cancellare.
- **RT-9 — Registrazione eventi (§14).** «Iscritto creato», «iscritto eliminato», «stato ricalcolato» con
  `tenant_id`, `app_id` (`campaigns`), `user_id` e identificativo di correlazione. **Mai** il recapito, mai il
  nome: nei registri vanno identificativi.

## 4. Criteri di accettazione

**CA-1 — Un iscritto nuovo non è contattabile**
- **Dato** un account senza iscritti
- **Quando** l'utente crea a mano un iscritto con indirizzo e nome
- **Allora** la scheda lo mostra `in quarantena`, con il motivo «nessuna prova di consenso», e l'elenco conta
  **zero** contattabili

**CA-2 — Lo stato non si scrive**
- **Dato** un iscritto `in quarantena`
- **Quando** si invia un aggiornamento che contiene il campo di stato con valore `attivo`
- **Allora** il servizio risponde `400` in `application/problem+json`, l'iscritto resta `in quarantena` e il
  messaggio indica che il consenso si registra dalla sezione dedicata

**CA-3 — La soppressione vince**
- **Dato** un iscritto con un consenso valido registrato e il suo recapito presente nell'elenco di soppressione
- **Quando** si apre la sua scheda
- **Allora** lo stato è `soppresso`, con il motivo della soppressione, e non `attivo`

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con un iscritto che ha lo stesso indirizzo di posta
- **Quando** un utente di `A` cerca quell'indirizzo e poi tenta di aprire l'iscritto di `B` forzandone
  l'identificativo
- **Allora** trova solo il proprio e riceve `404` sull'altro; nulla rivela che l'indirizzo esista anche altrove

**CA-5 — L'eliminazione non cancella la prova**
- **Dato** un iscritto con due registrazioni di consenso e una disiscrizione
- **Quando** l'utente lo elimina
- **Allora** l'iscritto sparisce dall'elenco, ma le registrazioni e l'eventuale soppressione restano e continuano a
  impedire nuovi invii verso quel recapito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla regola di calcolo dello stato (compresa la precedenza della soppressione) e di
      **integrazione** sulla risorsa, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sugli iscritti, compreso il caso dello stesso indirizzo in due account;
- [ ] **prova end-to-end**: rimando — il percorso `[J-CAMPAIGNS]` nasce nella storia 0037 e comprenderà la creazione
      dell'iscritto come primo passo; motivo: qui non esiste ancora nessun percorso da estendere;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i nomi degli stati e i motivi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per le quattro voci di `subscriber`, campi annotati,
      tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché lo stato è
      calcolato e non scrivibile;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto qui; `stato_iscritto` è dichiarato nella
      storia 0034 e usa questa regola di calcolo;
- [ ] controllo automatico di **accessibilità** verde sull'elenco e sulla scheda;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Le tabelle esistono già; qui si aggiungono indici e comportamento |
| Storia `0003` | Serve il guscio del modulo per avere una sezione dove mettere l'elenco |
| Storie `0007` e `0011` | La regola di calcolo dello stato le presuppone: fino a quando non esistono, ogni iscritto è `in quarantena` — che è il comportamento giusto e va provato così |

## 7. Fuori ambito

- la registrazione del consenso e il suo storico: è la storia 0007;
- l'elenco di soppressione: è la storia 0011;
- l'importazione da file: è la storia 0010;
- l'unione di due iscritti duplicati: rimandata, perché con recapito univoco per account il duplicato nasce solo
  da varianti dello stesso indirizzo, e la normalizzazione ne copre la parte utile. Se emergerà come esigenza
  reale sarà una storia a sé.

## 8. Punti aperti

- **Campi personalizzati e categorie particolari.** Un campo chiamato «patologia» o «sindacato» trasforma un
  archivio ordinario in un archivio di dati particolari, e non abbiamo modo tecnico di impedirlo
  ([application-description.md](../application-description.md) §6, avvertenza 1). Questa storia introduce il
  contenitore; l'avviso nell'interfaccia al momento di creare un campo personalizzato è una proposta da validare
  con la revisione legale, che chiude anche se serva altro.
- **Termine di conservazione dei dati dell'iscritto.** La proposta del manifesto è «fino alla cancellazione
  dell'iscritto o dell'account»: non ho trovato un fondamento di legge per un termine diverso. Chiude lo
  sviluppatore in sede di compilazione del manifesto.
