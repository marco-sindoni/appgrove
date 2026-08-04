# 0012 — Catalogo delle metriche pubblicate

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 03 — Catalogo delle metriche e tracciabilità
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda «fatturato» su due schermate diverse
> voglio che quella parola significhi la stessa cosa in tutte e due, e sapere che cosa significa
> così da non dovermi chiedere ogni volta se sto guardando l'emesso o l'incassato.

**Contesto.** È il fondamento della tracciabilità e insieme la scelta di prodotto più impegnativa dell'app: gli
indicatori sono un **catalogo chiuso e versionato**, non formule libere. La ragione sta al §2.5 e al §4.3 della
[descrizione](../application-description.md): gli strumenti che rispondono in modo affidabile a domande sui dati
non lasciano scrivere interrogazioni al modello, lo fanno scegliere dentro uno strato di metriche governate —
«definire metriche, dimensioni e permessi una volta sola, così che ogni consumatore restituisca lo stesso numero»
(fonte 8). Il prezzo è la rigidità; il guadagno è che il numero di oggi è confrontabile con quello di ieri.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `definizione_metrica` con: chiave stabile, **versione**, titolo e descrizione
   nelle cinque lingue, tipo di aggregazione (somma, media, conteggio, ultimo valore, minimo, massimo), chiave
   della misura su cui opera, unità, dimensioni ammesse, fonti richieste, classe di riservatezza, stato
   (`bozza`, `pubblicata`, `ritirata`).
2. **RF-2** — Una definizione in `bozza` non produce valori e non è visibile fuori dalla sezione Metriche. Solo
   una definizione `pubblicata` può essere usata da cruscotti, copilota, avvisi, rapporti ed esportazioni.
3. **RF-3** — Modificare una definizione **pubblicata** crea una **versione nuova**; la versione precedente
   resta e ogni valore già calcolato conserva il riferimento alla versione con cui è stato prodotto.
4. **RF-4** — L'app nasce con un insieme di **metriche predefinite** — fatturato emesso, incassato, crediti
   aperti, crediti scaduti, valore delle trattative aperte, valore di magazzino, spese approvate — già
   pubblicate, già classificate e già tradotte. Il cliente non deve costruire niente per cominciare.
5. **RF-5** — Una metrica dichiara **quali fonti le servono**: se una fonte richiesta non è collegata, la metrica
   esiste ma non produce valori e dice perché.
6. **RF-6** — La sezione **Metriche** elenca il catalogo con chiave, titolo, unità, versione, classe di
   riservatezza, fonti richieste e stato; per ogni metrica si legge, in lingua naturale, **che cosa significa**.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le metriche predefinite sono di sistema e uguali per tutti; le
  definizioni create o modificate dal cliente hanno `tenant_id` e si leggono con il filtro per account preso dal
  gettone verificato. Un account non vede né usa le definizioni di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/insights/v1/metriche`,
  `GET /api/insights/v1/metriche/{chiave}`, `POST /api/insights/v1/metriche`,
  `POST /api/insights/v1/metriche/{chiave}/pubblicazione`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__catalogo_delle_metriche.sql` sullo schema `app_insights`:
  tabella `definizione_metrica` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica; unicità su `(tenant_id, chiave, versione)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Metriche` del modulo `insights`; dati letti con il client
  generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I titoli e le descrizioni **delle metriche predefinite** sono in tutte e cinque
  le lingue; le metriche create dal cliente hanno il titolo che lui scrive, in una lingua sola, e questo va
  detto nell'interfaccia.
- **RT-6 — Varchi e ruoli (§6).** Creare, modificare e pubblicare una definizione richiede ruolo `owner` o
  `admin`; tutti possono leggere il catalogo delle metriche che il loro ruolo consente di vedere (storia 0014).
  Nessun consumo di quota: il catalogo è illimitato in ogni piano.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: una definizione è una formula, non un dato.
- **RT-14 — Registrazione eventi (§14).** «Metrica pubblicata», «metrica ritirata» con `tenant_id`, `app_id`,
  `user_id`, chiave e versione.

## 4. Criteri di accettazione

**CA-1 — Il catalogo nasce pieno**
- **Dato** un account appena abilitato a InsightGrove
- **Quando** apre la sezione Metriche
- **Allora** vede le metriche predefinite già pubblicate, ciascuna con la sua descrizione in lingua e le fonti
  che le servono

**CA-2 — Una metrica senza la sua fonte non produce valori**
- **Dato** la metrica «valore di magazzino», che richiede la fonte magazzino, e un account che non l'ha collegata
- **Quando** si prova a usarla su un cruscotto o nel copilota
- **Allora** non produce un valore: dice «richiede la fonte magazzino, non collegata» con il rimando alla
  sezione Fonti

**CA-3 — Modificare crea una versione**
- **Dato** la metrica `fatturato_emesso` in versione 2, usata da un riquadro
- **Quando** un `owner` ne cambia l'aggregazione e pubblica
- **Allora** esiste la versione 3, la versione 2 resta, e i valori già calcolati conservano il riferimento alla
  versione 2

**CA-4 — Una bozza non produce niente**
- **Dato** una definizione in stato `bozza`
- **Quando** si prova a usarla in un riquadro o in una domanda al copilota
- **Allora** non è selezionabile e la richiesta viene rifiutata con la spiegazione

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con proprie definizioni personalizzate
- **Quando** un utente di `A` legge il catalogo forzando l'identificativo di `B`
- **Allora** vede le metriche di sistema più le proprie, mai quelle di `B`

**CA-6 — Un `member` non pubblica**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di creare o pubblicare una definizione
- **Allora** riceve `403`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla creazione di versione e sullo stato di pubblicazione, e di **integrazione** sulle
      risorse, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle definizioni;
- [ ] prova sulla **matrice dei ruoli**;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** delle metriche predefinite presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la scelta del catalogo chiuso e versionato e il perché;
- [ ] contratto degli **strumenti conversazionali**: `elenca_metriche` (lettura) e `pubblica_metrica`
      (**scrittura con conferma obbligatoria**) dichiarati — contratto completo nelle storie 0031 e 0032;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve lo schema |
| storia `0006` | le chiavi di misura su cui una metrica opera vengono dal contratto delle fonti |

## 7. Fuori ambito

- le metriche costruite come formula fra altre metriche: storia 0013;
- la classe di riservatezza come meccanismo di controllo: storia 0014 — qui il campo esiste, là comanda;
- il calcolo del valore su un periodo: storia 0015;
- la scheda che spiega un valore: storia 0016.

## 8. Punti aperti

- **Il cliente può ridefinire una metrica di sistema?** Se `fatturato_emesso` viene ridefinita, il passato
  cambia per tutti quelli che la guardano. Raccomandazione: **il cliente può creare metriche proprie e non può
  modificare quelle di sistema**, ma può nasconderle. Chiude: **sviluppatore** (punto aperto 10 della
  descrizione).
- **Le metriche create dal cliente non sono tradotte.** Un account che lavora in due lingue avrà un catalogo
  misto. Non c'è rimedio ragionevole a questa scala; va detto nell'interfaccia, non risolto con una traduzione
  automatica che nessuno ha chiesto.
