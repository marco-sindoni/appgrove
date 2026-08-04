# RepGrove — estensioni della console di amministrazione

**Applicazione**: 17 — RepGrove (`recensioni`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

RepGrove dipende da **deleghe verso piattaforme di terzi** che scadono, vengono revocate o smettono di funzionare
senza che il cliente se ne accorga: quando succede, le recensioni smettono di arrivare e la prima notizia è una
richiesta di assistenza che dice «non funziona più niente». Senza una vista sullo stato dei collegamenti, chi
risponde deve indovinare. Servono poi due cose minori e una importante: la deroga temporanea sul tetto delle sedi
durante una migrazione, la diagnostica delle lavorazioni programmate, e il **registro dei rifiuti delle pratiche
vietate** — che non serve a punire nessuno, ma a sapere se un cliente sta insistendo per fare qualcosa che gli
costerebbe la sospensione del profilo, e a raggiungerlo prima che ci riesca per altre vie.

Nessuna estensione dà accesso ai contenuti dell'account: non si leggono recensioni, non si leggono recapiti di
clienti finali, non si vedono i testi delle risposte.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `piattaforme_abilitate` | quali piattaforme l'account può collegare (`google`, `trustpilot`) | solo `google` | amministratore di piattaforma | dipende dall'accesso che **noi** abbiamo ottenuto dal fornitore e dalla quota che ci è stata concessa (descrizione §11): non è una scelta del cliente, ed esporla come impostazione farebbe promettere ciò che non possiamo mantenere |
| `raccolta_intervallo_minuti` | ogni quanto la lavorazione periodica interroga le piattaforme per quell'account | `60` | amministratore di piattaforma | è una leva sul **nostro** consumo di quota verso le interfacce esterne; un cliente che la mettesse al minimo danneggerebbe gli altri |
| `perimetro_sanitario_consentito` | se l'account può dichiarare sedi di settori sanitari e assimilati (dentisti, fisioterapisti, veterinari, psicologi) | `no` | amministratore di piattaforma, **dopo** la decisione dello sviluppatore | discende dall'avviso sull'articolo 9 (descrizione §6 e punto aperto §11.7): finché la decisione non è presa, il perimetro resta chiuso, e non è una preferenza del cliente |
| `invii_sospesi` | interruttore che ferma l'invio degli inviti per l'account | `no` | amministratore di piattaforma | è una misura di protezione (per esempio a fronte di una segnalazione di abuso o di un tasso di rifiuto anomalo del fornitore di recapito) e non può stare nelle mani di chi va fermato |

## 3. Quote e deroghe

- **Metrica governata**: `sedi_monitorate` (natura `stock`).
- **Serve una deroga manuale?** **Sì.** Il caso è concreto: una piccola catena in prova collega tutte le sedi per
  vedere se lo strumento le serve, e supera il tetto del piano prima di aver deciso quale piano prendere. Senza
  deroga, l'unica via è farle comprare il piano più alto per due settimane — che è esattamente il comportamento
  che il mercato rimprovera ai concorrenti (descrizione §2.5).
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria**; alla scadenza il tetto torna
  quello del piano e le sedi eccedenti passano in stato `sospesa` (non si cancellano mai: cancellare dati per una
  quota scaduta sarebbe sproporzionato).
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, per quale motivo e a quale
  richiesta di assistenza si riferisce.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha stabilmente bisogno di più
  sedi, cambia piano; se ne ha bisogno di più di cinque, siamo fuori dal segmento dichiarato ed è il punto aperto
  n. 4 della descrizione §11.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato dei collegamenti** | per account e per sede: piattaforma, stato (`da_autorizzare`, `attivo`, `scaduto`, `revocato`), momento dell'ultima sincronizzazione riuscita, codice dell'ultimo errore | «Perché il cliente dice che non gli arrivano più recensioni?» — è la causa numero uno di assistenza | metadati: stato, orario, codice di errore. **Nessuna credenziale**, nemmeno oscurata, e nessun contenuto |
| **Lavorazioni di raccolta** | code, arretrato, durata media, ultimi errori per piattaforma, consumo della quota che abbiamo verso il fornitore | «C'è un accumulo?» «Stiamo per sbattere contro il limite dell'interfaccia?» | conteggi e codici |
| **Recapito degli inviti** | per account: inviti inviati, recapitati, respinti, e tasso di rifiuto del fornitore | «Questo account sta bruciando la reputazione del nostro dominio di invio?» | conteggi aggregati; **nessun destinatario** |
| **Modelli di messaggio respinti** | conteggio per categoria di pratica vietata (incentivo, contenuto specifico, selettività), per account e periodo | «Questo cliente sta insistendo per fare una cosa che gli costerà il profilo?» | categoria e conteggio; **mai il testo respinto** (storia 0029, RT-8) |
| **Rifiuti dal livello conversazionale** | stessa cosa vista dall'altra superficie: categorie di richiesta rifiutate agli strumenti | «Il divieto tiene anche da lì?» | categoria e conteggio |
| **Dichiarazioni di trasparenza** | quante sedi hanno il riquadro pubblico attivo e quante hanno la dichiarazione `da_riconfermare` oltre la tolleranza | «Ci sono clienti che stanno pubblicando recensioni con una dichiarazione non più vera?» | conteggi e stati |

Nessuna di queste viste mostra recensioni, risposte, nomi di clienti finali o recapiti: sono tutte fatte di stati,
codici e conteggi. Il divieto di impersonificazione vale come altrove — chi amministra la piattaforma non entra
nell'account del cliente.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Rilanciare una raccolta fallita** | il cliente segnala recensioni mancanti dopo un errore della piattaforma | sì | riga di controllo con operatore, account, sede e motivo | doppia importazione se la raccolta non fosse idempotente: lo è per il vincolo di unicità della storia 0009, e la prova va tenuta verde |
| **Marcare un collegamento come `da_autorizzare`** | la delega è rotta e il cliente non riesce a rifarla | sì (il cliente rifà l'autorizzazione) | operatore, motivo, momento | il cliente resta senza raccolta finché non riautorizza: va fatto d'accordo con lui |
| **Concedere una deroga di quota** | migrazione o prova di una catena | sì, e scade da sola | operatore, tetto, scadenza, motivo, richiesta di assistenza | dimenticare la scadenza: per questo la scadenza è obbligatoria |
| **Sospendere gli invii dell'account** | tasso di rifiuto anomalo, segnalazione di abuso, sospetto di uso non conforme | sì | operatore, motivo, momento, notifica al cliente | ferma una funzione che il cliente paga: richiede motivo scritto e va comunicata, non subita |
| **Riaprire il perimetro sanitario per un account** | solo dopo la decisione dello sviluppatore sull'articolo 9 | sì | operatore, riferimento alla decisione, motivo | apre il trattamento a possibili categorie particolari: **non è un'azione di assistenza ordinaria** e va concessa solo con la decisione alle spalle |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno o irreversibili
richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti dell'account.

**Azioni deliberatamente assenti**, ed è la parte che conta in questa app: nessuno dalla console può **pubblicare
una risposta**, **inviare una segnalazione**, **mandare un invito** o **cambiare la regola di equità** di un
cliente. Sono atti verso l'esterno compiuti a nome dell'azienda cliente: li fa il cliente, con la conferma umana
prevista dalle storie 0019, 0021 e 0028, oppure non li fa nessuno.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| stato e ultima sincronizzazione dei collegamenti per sede | metadato | no | è la diagnosi numero uno |
| codice dell'ultimo errore restituito dalla piattaforma | metadato | no (codice, non messaggio libero) | capire se è una delega scaduta o un guasto del fornitore |
| conteggio delle sedi collegate per account, e tetto in vigore | metrica | no | diagnosi delle quote e deroghe |
| conteggi di inviti inviati, recapitati, respinti | metrica | no | salute del canale di recapito |
| conteggi dei modelli respinti e dei rifiuti, per categoria | metrica | no | capire se un cliente sta insistendo su una pratica vietata |
| stato delle dichiarazioni di trasparenza per sede | metadato | no | rischio di conformità sul riquadro pubblico |
| arretrato e durata delle lavorazioni di raccolta | metrica | no | capacità e allarmi |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali, ed è una condizione di
progettazione, non una constatazione: se un giorno servisse esporre alla console un dato riferibile a una persona
(per esempio il recapito di un invito respinto, per capire un guasto), va **prima** dichiarato nel manifesto dei
dati dell'app con la finalità «assistenza» e va detto perché non basta il conteggio. L'accesso amministrativo è un
trattamento come gli altri.

## 7. Punti aperti

- **Il perimetro sanitario** (`perimetro_sanitario_consentito`) esiste come interruttore perché la decisione
  sull'articolo 9 non è stata presa (descrizione §6 e §11.7). Se la decisione fosse «settori sanitari fuori dal
  perimetro», l'interruttore sparisce e resta un controllo fisso nell'app; se fosse «dentro, con garanzie
  rafforzate», serve molto più di un interruttore. **Decide lo sviluppatore.**
- **Se la sospensione degli invii debba essere anche automatica** oltre una soglia di rifiuti del fornitore di
  recapito: automatizzarla protegge il dominio di invio di tutti, ma ferma un servizio pagato senza che una persona
  abbia guardato. Proposta: allarme automatico, sospensione manuale. **Da confermare.**
- **Quanto a lungo conservare i conteggi diagnostici** per account: non contengono persone, ma non servono per
  sempre. Da allineare alla politica comune di piattaforma, che non è materia di questa app.
