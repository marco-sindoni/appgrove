# Modello — `estensioni-admin.md`

> **Istruzioni per l'agente-app (cancellare questo riquadro a stesura conclusa).**
> Copia tutto ciò che sta **sotto la riga** in `docs/usecases/_catalogo/NN-<slug>/estensioni-admin.md` e riempi i
> segnaposto `‹…›`.
>
> **Che cos'è la console di amministrazione.** È l'applicazione interna di appgrove (`frontend/apps/admin`, su un
> sottodominio separato dal backoffice dei clienti) da cui chi gestisce la piattaforma vede gli account, gli
> utenti, le abilitazioni, la fatturazione, la riconciliazione, le app, i diritti degli interessati e le richieste
> di assistenza. È **già** in grado di governare le cose comuni a tutte le app: se la tua non ha bisogno di nulla
> in più, questo documento si chiude in dieci righe — ed è l'esito migliore.
>
> **Due regole.**
> 1. **«Nessuna estensione oltre lo standard di piattaforma» è una risposta legittima**, purché motivata. Va
>    scritta nella sezione 1 e il documento finisce lì. Non inventare pannelli per riempire il modello.
> 2. **Vietata l'impersonificazione.** Chi amministra la piattaforma non entra nell'account del cliente e non
>    guarda i suoi dati: vede metadati, stati e conteggi. Ogni richiesta di «vedere cosa vede il cliente» va
>    riformulata come diagnostica su metadati, oppure dichiarata come punto aperto.

---

# ‹Nome dell'app› — estensioni della console di amministrazione

**Applicazione**: ‹NN — Nome dell'app› (`‹app_id›`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: ‹AAAA-MM-GG›
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

Scegliere **una** delle due e cancellare l'altra.

**A — Nessuna estensione oltre lo standard di piattaforma.**
‹Motivazione in due o tre righe: perché le viste comuni bastano. Per esempio: l'app non ha parametri di
configurazione per account, la sua quota si governa con il meccanismo comune dei piani, non ha lavorazioni
programmate né integrazioni esterne di cui sorvegliare lo stato, e l'assistenza si risolve con le informazioni
già presenti nella scheda dell'account.›

**B — Servono le estensioni descritte qui sotto.**
‹Riassunto in tre righe di cosa manca e perché la piattaforma da sola non basta.›

## 2. Parametri di configurazione per account

> Impostazioni che chi amministra la piattaforma deve poter vedere o cambiare **per un singolo account**, e che
> non sono esposte al cliente. Ogni riga deve avere un motivo operativo reale: se il parametro serve al cliente,
> va nell'app, non qui.

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `‹chiave›` | ‹effetto› | ‹valore› | ‹amministratore di piattaforma› | ‹motivo› |

‹Se non ce ne sono: «Nessun parametro per account: la configurazione dell'app è interamente nelle mani del
cliente».›

## 3. Quote e deroghe

> La quota ordinaria arriva dal piano ed è governata dal listino come codice: qui si descrive solo ciò che serve
> **oltre** a quello — tipicamente la deroga temporanea concessa a un cliente in assistenza.

- **Metrica governata**: `‹metrica›` (natura `‹flow | stock›`).
- **Serve una deroga manuale?** ‹sì/no› — ‹motivo: per esempio «una migrazione iniziale supera il tetto del piano
  per il solo primo mese».›
- **Forma della deroga**: ‹tetto alternativo con data di scadenza / sospensione temporanea del blocco›.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando e perché.
- **Limite**: ‹una deroga non è uno sconto e non cambia l'abbonamento: se il cliente ha bisogno stabilmente di
  più, passa di piano.›

‹Se non serve: «Nessuna deroga: la quota si governa cambiando piano».›

## 4. Viste operative e diagnostiche

> Cosa deve poter **vedere** chi amministra. Ricordare il divieto di impersonificazione: si guardano metadati,
> stati e conteggi, non i contenuti del cliente.

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| ‹Stato delle integrazioni› | ‹elenco delle connessioni per account, con esito dell'ultima sincronizzazione› | «Perché il cliente dice che non gli arrivano più i dati?» | ‹metadati: stato, orario, codice di errore — nessun contenuto› |
| ‹Lavorazioni programmate› | ‹code, arretrato, ultimi errori› | «C'è un accumulo?» | ‹conteggi› |

‹Se non servono: «Le viste comuni (account, abilitazioni, fatturazione, richieste di assistenza) bastano».›

## 5. Azioni di supporto

> Cosa deve poter **fare** chi amministra per sbloccare un cliente. Ogni azione va classificata: reversibile o
> irreversibile, e con quale prova resta tracciata.

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| ‹Ripetere una lavorazione fallita› | ‹il cliente segnala un documento non elaborato› | sì | ‹riga di controllo con operatore e motivo› | ‹doppia elaborazione se non è idempotente› |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni **irreversibili** o con effetti verso
l'esterno (invii, pagamenti, cancellazioni) richiedono una conferma esplicita e non sono mai automatiche;
nessuna azione dà accesso ai contenuti dell'account.

‹Se non servono: «Nessuna azione specifica: l'assistenza si risolve con gli strumenti comuni».›

## 6. Dati esposti alla console

> Elenco esplicito di ciò che l'app rende visibile alla console, per evitare che ci finisca dentro qualcosa che
> non dovrebbe.

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| ‹conteggio dei ‹X› per account› | ‹metrica› | no | ‹diagnosi delle quote› |

**Verifica obbligatoria.** ‹Se una riga contiene dati personali, va dichiarata nel manifesto dei dati dell'app e
va detto perché la console ne ha bisogno: l'accesso amministrativo è un trattamento come gli altri.›

## 7. Punti aperti

- ‹Cosa non sono riuscito a determinare, e chi lo chiude.›

‹Se non ce ne sono: «Nessuno».›
