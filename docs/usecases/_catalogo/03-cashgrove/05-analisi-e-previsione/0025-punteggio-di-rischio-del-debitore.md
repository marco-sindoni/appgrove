# 0025 — Punteggio di rischio del debitore

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 05 — Analisi e previsione
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha venti clienti in ritardo e un'ora di tempo
> voglio sapere su quali conviene insistere per primi
> così da usare quell'ora dove serve, invece di partire dal primo nome dell'elenco.

**Contesto.** La scheda di catalogo chiede la «prioritizzazione dei morosi ad alto rischio». Il punteggio nasce **solo**
dal comportamento osservato dentro l'account — quanto paga in ritardo, quante promesse ha mancato, quante contestazioni
apre, quanto denaro ha fermo — e non da banche dati esterne o centrali rischi, che sono fuori dal perimetro dichiarato
dell'app ([documento capofila](../application-description.md) §1). Va detto subito che questo è **profilazione**: un
trattamento che descrive il comportamento di una persona o impresa. Le conseguenze sono di prodotto, non solo di
conformità: il punteggio deve essere spiegabile, non deve mai produrre da solo un effetto verso il debitore, e serve a
ordinare il lavoro di una persona.

## 2. Requisiti funzionali

1. **RF-1** — Ogni debitore ha un punteggio da 0 a 100 e una fascia (basso, medio, alto), ricalcolati dalla lavorazione
   quotidiana.
2. **RF-2** — Il punteggio si compone di fattori dichiarati, ciascuno con il proprio peso: ritardo medio, ritardo
   massimo, promesse mancate, contestazioni aperte e chiuse, importo attualmente scaduto, anzianità del rapporto.
3. **RF-3** — La scheda del debitore mostra **il calcolo**: quali fattori, con quale valore e quale contributo al
   punteggio.
4. **RF-4** — Un debitore con storico insufficiente non riceve un punteggio, ma l'etichetta «storico insufficiente»:
   non si inventa un numero.
5. **RF-5** — Il punteggio ordina gli elenchi e alimenta la vista «su chi insistere oggi», ma **non** cambia da solo le
   sequenze, non blocca nulla e non viene mai comunicato al debitore.
6. **RF-6** — I pesi dei fattori sono configurabili per account entro limiti dichiarati, perché ogni settore ha una
   normalità di pagamento diversa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il punteggio si calcola **solo** sui dati dell'account, con filtro
  `WHERE tenant_id = :tid`. Lo stesso debitore in due account ha due punteggi indipendenti: nessuna aggregazione
  attraversa gli account, mai — sarebbe una banca dati di comportamento di pagamento, che è un'altra cosa e un'altra
  responsabilità.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/debitori/{id}/rischio` e
  `PUT /api/crediti/v1/impostazioni/pesi-rischio`; errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `punteggio_di_rischio` sullo schema `app_crediti` (debitore,
  punteggio, fascia, componenti del calcolo, istante) con `tenant_id`, chiave UUID versione 7, colonne di controllo e
  cancellazione logica. Si conserva uno storico limitato nel tempo, non uno storico perpetuo.
- **RT-4 — Modulo frontend (§3, §5).** Indicatore nella scheda del debitore con il dettaglio del calcolo apribile,
  ordinamento per rischio negli elenchi; solo token del sistema di design; tema chiaro e scuro. Le fasce si distinguono
  per etichetta oltre che per colore.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i nomi dei fattori e la loro spiegazione, passano
  dallo spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Non consuma quota. Modificare i pesi richiede ruolo `owner` o `admin`.
- **RT-7 — Esposizione conversazionale (§12).** `punteggio_rischio_debitore(debitore) → punteggio, fascia e componenti`
  è dichiarato qui come strumento di **lettura**, raccolto nel contratto della storia `0028`. Il risultato porta
  **sempre** con sé i componenti: un punteggio nudo restituito a un assistente è esattamente il modo in cui un numero
  diventa una decisione senza che nessuno l'abbia presa.
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese, con la categoria dichiarata come
  **profilazione sul comportamento di pagamento**, base «legittimo interesse» e finalità «ordinare le priorità di
  lavoro». Va scritto esplicitamente che **non** si prendono decisioni automatizzate verso il debitore. Tabella presente
  in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «punteggi ricalcolati» (con il conteggio) e «pesi modificati» sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza punteggi individuali e senza
  nomi.

## 4. Criteri di accettazione

**CA-1 — Calcolo e spiegazione**
- **Dato** un debitore con ritardo medio di 45 giorni e due promesse mancate
- **Quando** si apre la sua scheda
- **Allora** compaiono punteggio, fascia e l'elenco dei fattori con il contributo di ciascuno, in modo che la somma sia
  verificabile a mano

**CA-2 — Storico insufficiente**
- **Dato** un debitore con una sola fattura e nessun incasso · **Quando** si apre la sua scheda · **Allora** compare
  «storico insufficiente» e nessun numero

**CA-3 — Nessun effetto automatico**
- **Dato** un debitore in fascia alta · **Quando** matura un passo di sequenza · **Allora** il passo è quello previsto
  dalla sequenza: il punteggio non ha cambiato nulla da solo

**CA-4 — Indipendenza fra account**
- **Dato** lo stesso debitore presente in due account, con comportamenti diversi
- **Quando** si guardano i due punteggi
- **Allora** sono calcolati separatamente e nessuno dei due risente dei dati dell'altro

**CA-5 — Pesi configurabili entro limiti**
- **Dato** i pesi dei fattori · **Quando** si tenta di azzerarli tutti o di superare il limite dichiarato · **Allora**
  la richiesta è respinta con la spiegazione del limite

**CA-6 — Ricalcolo quotidiano**
- **Dato** una promessa mancata ieri · **Quando** la lavorazione quotidiana viene eseguita · **Allora** il punteggio del
  debitore è aggiornato e la variazione è visibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** su ciascun fattore e sulla somma, di **integrazione** sul ricalcolo quotidiano;
- [ ] prova di **isolamento fra account**, con particolare attenzione al caso dello stesso debitore in due account;
- [ ] **prova end-to-end**: *nessun impatto* — il punteggio è una lettura derivata, coperta da prove di integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce di **profilazione**, tabella presente in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di non usare fonti esterne e sull'assenza di
      decisioni automatizzate;
- [ ] contratto degli **strumenti conversazionali**: `punteggio_rischio_debitore` dichiarato come lettura, con i
      componenti sempre inclusi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | Le promesse mantenute e mancate sono il fattore più informativo |
| storia `0024` | I ritardi medi sono già calcolati lì e non si duplicano |

## 7. Fuori ambito

- **Fonti esterne di merito creditizio** (centrali rischi, banche dati camerali, punteggi commerciali): fuori dal
  perimetro dichiarato dell'app. Aggiungerebbero un fornitore, un trattamento nuovo e una responsabilità che il
  segmento non chiede.
- La condivisione dei punteggi fra account: **esclusa in modo categorico**. Sarebbe una banca dati di comportamento di
  pagamento, cioè un prodotto diverso con un regime giuridico diverso.
- L'uso del punteggio per cambiare automaticamente la sequenza: rimandato; se emergerà, va progettato con la garanzia
  che resti una proposta all'utente e non un automatismo.

## 8. Punti aperti

**I pesi predefiniti dei fattori** sono una proposta ragionevole ma non fondata su dati: nessuna fonte consultata
pubblica un modello di riferimento per il segmento. Li conferma lo sviluppatore, sapendo che sono anche una scelta di
prodotto (dicono che cosa l'app considera «rischio»).
