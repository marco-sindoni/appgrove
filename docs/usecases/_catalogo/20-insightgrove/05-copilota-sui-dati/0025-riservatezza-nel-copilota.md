# 0025 — Riservatezza nel copilota

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 05 — Copilota sui dati
**Storia**: `0025` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha aperto il copilota a tutta la squadra
> voglio essere certo che chi non deve vedere il fatturato non lo ottenga facendo la domanda in un altro modo
> così da poter dare a tutti uno strumento potente senza dare a tutti gli stessi dati.

**Contesto.** Una chat è la superficie più facile da aggirare: basta riformulare. «Quanto abbiamo fatturato» può
diventare «quanto vale in media un ordine, per il numero di ordini». Il presidio non può quindi stare nel testo:
sta nel **piano**, che nomina metriche, e nel filtro che il livello di accesso ai dati applica prima ancora che
il piano sia formulato. E c'è un secondo punto, più sottile e più importante: quando una metrica riservata
concorre a un calcolo, **la risposta non è un numero più piccolo** — è un rifiuto. Un aggregato filtrato è un
numero sbagliato, non un numero parziale (§4.3 della [descrizione](../application-description.md), regola 3).

## 2. Requisiti funzionali

1. **RF-1** — Al modello viene passato **soltanto** il catalogo delle metriche che il ruolo di chi chiede può
   vedere: una metrica economica **non esiste** nel vocabolario di un `member`.
2. **RF-2** — Se un piano nomina comunque una metrica riservata — perché il modello l'ha inventata o perché la
   richiesta arriva da uno strumento conversazionale — la validazione lo rifiuta con `403` e il motivo «ruolo
   insufficiente».
3. **RF-3** — Se una metrica **derivata** visibile compone una metrica riservata, l'intera derivata è riservata
   (storia 0014): non si risponde con la parte visibile.
4. **RF-4** — Il rifiuto per riservatezza dice **che l'indicatore esiste e che il ruolo non basta**, e non
   finge che non esista. Nascondere l'esistenza è una forma di inganno che l'utente scopre subito e che riduce
   la fiducia; dirlo è anche l'unico modo perché possa chiedere a chi amministra.
5. **RF-5** — Il rifiuto per riservatezza **non consuma quota**.
6. **RF-6** — La regola si applica **identica** alla chat interna e agli strumenti conversazionali (storia 0033):
   un solo punto di applicazione, nessuna porta di servizio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il ruolo viene dal gettone verificato insieme al `tenant_id`; nessuna
  parte della domanda può influenzarlo.
- **RT-2 — Interfaccia di programmazione (§2).** Il filtro per classe si applica nel **livello di accesso ai
  dati**, prima della composizione del contesto per il modello: non è un controllo nell'interfaccia. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Il messaggio di rifiuto per ruolo è distinto dagli altri rifiuti e
  contiene l'indicazione di chiedere a chi amministra; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Il messaggio esiste in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** È il quarto varco della catena: `403`. Non consuma quota (quinto varco non
  raggiunto).
- **RT-8 — Dati personali (§10).** Nessuna voce nuova.
- **RT-14 — Registrazione eventi (§14).** «Domanda rifiutata per ruolo» con `tenant_id`, `app_id`, `user_id`,
  chiave della metrica richiesta e identificativo di correlazione; **mai** il testo della domanda.
- **RT-11 — Prove (§11).** Matrice dei ruoli sul copilota, con almeno un caso di **tentativo di aggiramento**:
  una domanda che chiede di ricostruire una metrica riservata componendo metriche visibili.

## 4. Criteri di accettazione

**CA-1 — La metrica riservata non è nel vocabolario**
- **Dato** un utente `member` e la metrica `fatturato_emesso` di classe `economica`
- **Quando** chiede «quanto abbiamo fatturato a luglio»
- **Allora** riceve il rifiuto per ruolo insufficiente, con l'indicazione di chiedere a chi amministra, e nessun
  numero

**CA-2 — Aggiramento per composizione**
- **Dato** lo stesso utente `member`, che ha accesso a «numero di documenti» (operativa) ma non a «valore medio
  del documento» (economica)
- **Quando** chiede «quanti documenti per il loro valore medio»
- **Allora** il piano che nomina la metrica economica è rifiutato; non viene prodotto alcun valore economico

**CA-3 — Derivata riservata**
- **Dato** una derivata che compone una metrica operativa e una economica
- **Quando** un `member` la chiede
- **Allora** riceve il rifiuto: **non** riceve il risultato calcolato sulla sola parte operativa

**CA-4 — Il rifiuto non consuma quota**
- **Dato** un `member` con quota disponibile
- **Quando** riceve un rifiuto per ruolo
- **Allora** il contatore delle domande non aumenta

**CA-5 — Un `owner` ottiene la risposta**
- **Dato** la stessa domanda posta da un `owner`
- **Quando** viene elaborata
- **Allora** riceve il numero con la sua scheda

**CA-6 — Stessa regola dagli strumenti conversazionali**
- **Dato** una chiamata a `interroga_metrica` fatta da un assistente esterno per conto di un `member`
- **Quando** nomina una metrica economica
- **Allora** riceve lo stesso rifiuto con lo stesso codice

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul filtro del catalogo per ruolo e sull'ereditarietà nelle derivate; prove di
      **integrazione** sui tentativi di aggiramento;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli completa** sul copilota e sugli strumenti
      conversazionali;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «un `member` chiede il fatturato
      e riceve un rifiuto»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la regola «rifiuto, non calcolo parziale» e la scelta di
      dichiarare l'esistenza della metrica riservata;
- [ ] contratto degli **strumenti conversazionali**: il filtro per ruolo è parte del contratto, non della
      superficie (storie 0031 e 0033);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | la classe di riservatezza e le sue regole |
| storia `0022` | il piano è il punto in cui la regola si applica |

## 7. Fuori ambito

- permessi più fini di `owner`/`admin`/`member`: non esistono in piattaforma e non si inventano
  (punto aperto 3 della descrizione);
- il rilevamento di tentativi ripetuti di aggiramento come segnale di sicurezza: sarebbe materia dell'app
  31 AuditGrove, non di questa.

## 8. Punti aperti

- **Dire o non dire che la metrica esiste?** Dirlo aiuta l'utente e insieme rivela che l'azienda misura quella
  cosa. In un'azienda da 1 a 50 addetti l'informazione non è segreta. Raccomandazione: **dirlo**, come descritto.
  Chiude: **sviluppatore**.
- **Un utente potrebbe ricostruire un dato riservato combinando dati operativi** su periodi e dimensioni
  diverse. È un problema noto e non risolvibile con due classi: non lo si nasconde, si dichiara. Il rimedio
  vero sarebbe un modello di permessi più fine (punto aperto 3 della descrizione). Chiude: **piattaforma**.
