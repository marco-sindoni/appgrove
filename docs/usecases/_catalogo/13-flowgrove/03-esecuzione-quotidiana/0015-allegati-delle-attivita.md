# 0015 — Allegati delle attività

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 03 — Esecuzione quotidiana
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come tecnico che ha fotografato il quadro elettrico prima di intervenire
> voglio appendere la foto all'attività
> così da non doverla cercare nel telefono quando il cliente chiede com'era prima.

**Contesto.** Gli allegati sono la funzione più banale da descrivere e la più insidiosa da costruire: introducono
un archivio di file, limiti di dimensione, tipi ammessi, e soprattutto un contenuto che l'app **non controlla** e
che può contenere qualunque dato personale. La scelta di perimetro è netta: gli allegati stanno nell'archivio
della piattaforma, non presso un fornitore esterno (§2.4 della descrizione), e sono file **di lavoro**, non
conservazione a norma.

## 2. Requisiti funzionali

1. **RF-1** — Su un'attività si caricano file, con nome, dimensione, tipo, autore del caricamento e data.
2. **RF-2** — Esistono limiti espliciti e dichiarati all'utente **prima** del caricamento: dimensione massima per
   file e numero massimo di allegati per attività.
3. **RF-3** — I tipi ammessi sono elencati (immagini, documenti, fogli, testo); gli eseguibili e gli archivi
   compressi sono rifiutati con una spiegazione.
4. **RF-4** — Il file si scarica solo da un collegamento a scadenza breve, generato al momento e valido per chi
   ha diritto di leggere quell'attività.
5. **RF-5** — L'allegato si cancella; la cancellazione rimuove il file dall'archivio, non solo la riga.
6. **RF-6** — La schermata avvisa che i file caricati sono visibili a tutte le persone dell'account che vedono
   l'attività.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `attachment` filtra per `tenant_id` dal
  token verificato; il percorso del file nell'archivio comprende l'account, e il collegamento di scarico è
  verificato contro l'account del token: un collegamento non deve poter attraversare i confini nemmeno se
  indovinato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/progetti/v1/tasks/{id}/attachments`,
  `GET /api/progetti/v1/attachments/{id}/download-url`, `DELETE /api/progetti/v1/attachments/{id}`; errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V8__allegati.sql`: `attachment` con `tenant_id`, `task_id`,
  `storage_key`, `uploaded_by`, dimensione, tipo, colonne di controllo e cancellazione logica. Attenzione: la
  cancellazione della riga è logica, ma il **file** si cancella fisicamente.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro degli allegati nella scheda dell'attività, con l'avanzamento del
  caricamento e i limiti scritti prima; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Limiti, tipi ammessi, messaggi di rifiuto e avviso di visibilità in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Gli allegati **non consumano la metrica di quota** (`seats`): i limiti sono
  tecnici e uguali per tutti i piani. È una scelta da annotare, perché la tentazione di farne una seconda metrica
  è forte e la piattaforma ne ammette una sola.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di caricamento da chat. In lettura, il conteggio
  degli allegati compare in `get_project_progress`; il contenuto dei file non viene mai restituito a uno
  strumento.
- **RT-8 — Dati personali (§10).** `attachment.uploaded_by` è un dato personale; **il contenuto del file è un
  dato personale potenziale e non ispezionabile**: va dichiarato come tale nel manifesto in italiano e inglese, e
  la tabella e l'archivio devono comparire in `exportData` (i file, non solo i nomi) e in `purgeData`
  (cancellazione fisica del file).
- **RT-9 — Registrazione eventi (§14).** «Allegato caricato», «allegato cancellato», «caricamento rifiutato» con
  `tenant_id`, `app_id`, `user_id`, dimensione e tipo; **mai** il nome del file, che può contenere di tutto.

## 4. Criteri di accettazione

**CA-1 — Caricamento**
- **Dato** un'attività e un file ammesso sotto il limite
- **Quando** la persona lo carica
- **Allora** compare nell'elenco degli allegati con nome, dimensione e autore

**CA-2 — Rifiuto**
- **Dato** un file oltre la dimensione massima o di tipo non ammesso
- **Quando** si tenta il caricamento
- **Allora** la risposta è `422` con il motivo e il limite, e nulla viene scritto né nell'archivio né nel database

**CA-3 — Collegamento di scarico a scadenza**
- **Dato** un allegato
- **Quando** si genera il collegamento di scarico e si attende oltre la scadenza
- **Allora** il collegamento non funziona più e ne serve uno nuovo

**CA-4 — Cancellazione fisica**
- **Dato** un allegato caricato
- **Quando** lo si cancella
- **Allora** il file non è più presente nell'archivio, non solo nascosto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` usa un collegamento di scarico di un allegato di `B`
- **Allora** riceve `404` e non ottiene il file

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla validazione di tipo e dimensione e di **integrazione** su caricamento, scarico e
      cancellazione fisica;
- [ ] prova di **isolamento fra account** sui collegamenti di scarico;
- [ ] **prova end-to-end**: nessun impatto — `[J-PROGETTI]` non carica file; motivo registrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `attachment`, con il contenuto del file dichiarato dato personale
      potenziale e l'archivio incluso in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché gli allegati non sono una seconda metrica di
      quota;
- [ ] controllo automatico di **accessibilità** verde sul riquadro degli allegati;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | L'allegato vive su un'attività |
| Archivio dei file della piattaforma | I file non stanno nel database: serve l'archivio comune, con i dati a riposo in regioni europee |

## 7. Fuori ambito

- il collegamento a un archivio esterno (Drive, OneDrive, Dropbox): escluso in questa stesura, perché
  introdurrebbe un fornitore che tratta dati per nostro conto (§2.4 della descrizione);
- le versioni successive dello stesso file: si ricarica, non si versiona;
- l'anteprima dei documenti dentro l'app.

## 8. Punti aperti

- **Limite di spazio complessivo per account**: la piattaforma ammette una sola metrica di quota, che qui è
  `seats`. Se l'occupazione dell'archivio diventasse un costo rilevante, servirebbe una politica — probabilmente
  un limite tecnico uguale per tutti, non una seconda metrica. Decisione da prendere quando ci saranno numeri
  veri, non ora.
