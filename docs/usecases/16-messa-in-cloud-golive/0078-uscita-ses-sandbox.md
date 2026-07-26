# UC 0078 — Uscita di SES dalla modalità di prova (sandbox)

**Area**: 16-messa-in-cloud-golive · **Fase**: evo (messa in cloud) · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0018 (email transazionali via SES), UC 0079 (gestione rimbalzi/reclami SES)
**Fonte**: R12 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Recapito email SES"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Portare l'account **SES** (Simple Email Service, il servizio di posta elettronica di AWS) **fuori dalla modalità di prova**
("sandbox"). In modalità di prova SES consegna la posta **solo a indirizzi verificati a mano**: qualunque altro destinatario
riceve un errore di consegna. Finché SES resta in prova, un nuovo utente che si registra **non riceve** l'email di verifica
indirizzo né quella di reset password: resta bloccato fuori. Uscire dalla modalità di prova è quindi **bloccante per il
go-live**.
**Incluso**: la preparazione e l'invio della richiesta ad AWS, la sua argomentazione, il presidio della risposta, la verifica
a esito positivo. **Escluso**: la gestione dei rimbalzi/reclami in sé (UC 0079, che però è il prerequisito di sostanza della
richiesta) e la verifica del dominio/firma DKIM (UC 0018).

## 2. Attori & ruoli
- **Founder**: presenta la richiesta ad AWS, scrive la motivazione d'uso, risponde a eventuali domande di chiarimento.
- **Platform engineer**: predispone le prove tecniche a corredo (gestione rimbalzi già attiva, dominio verificato).
- **Sistema**: SES eroga la posta, applica il limite di invio e la quota giornaliera.
- **Terzi**: **AWS** (istruisce la richiesta e la approva/respinge), **SES** (il servizio).

## 3. Precondizioni
- Ambiente `test` acceso e dominio email verificato in SES con firma **DKIM** (DomainKeys Identified Mail, la firma
  crittografica che attesta che il mittente è autorizzato) attiva (UC 0018).
- **Gestione rimbalzi/reclami già predisposta** (UC 0079): la richiesta è molto più solida se, alla domanda "come gestite i
  rimbalzi?", si può rispondere con un meccanismo già in funzione e non con una promessa.
- Regione infra = `eu-west-1` (l'account SES vive lì; la richiesta è per-regione).

## 4. Flusso principale
1. **Avviare in anticipo.** È il vincolo col tempo di attesa più lungo dell'intera messa in cloud: la risposta di AWS arriva
   in **giorni**, non ore, e può essere respinta con richiesta di chiarimenti. Va aperta con largo margine sul go-live, non il
   giorno stesso.
2. **Preparare le prove di sostanza** (UC 0079 già attivo): destinazione delle notifiche di rimbalzo/reclamo, lista di
   soppressione, allarme sul tasso. Averle pronte è ciò che rende credibile la richiesta.
3. **Compilare la richiesta** dalla console SES ("richiesta di aumento del limite di invio" / uscita dalla modalità di prova):
   descrivere il caso d'uso reale (email **transazionali** — cioè legate a un'azione dell'utente: verifica indirizzo, reset
   password, inviti a un'organizzazione), il tipo di destinatari (solo utenti che si registrano al marketplace, nessuna lista
   acquistata, nessun invio promozionale di massa), e **come gestiamo i rimbalzi e i reclami** (rimando esplicito a UC 0079).
4. **Inviare** e annotare la data di apertura del ticket.
5. **Presidiare la casella** su cui AWS risponde: se chiedono chiarimenti, rispondere puntualmente e in fretta (una risposta
   lenta allunga i giorni di attesa).
6. **A esito positivo**: verificare che l'account risulti "fuori dalla modalità di prova" e che una email reale verso un
   indirizzo **non** verificato in precedenza venga consegnata (vedi anche lo smoke email di UC 0081).

## 5. Flussi alternativi / edge / errori
- **Richiesta respinta / chiarimenti**: AWS chiede dettagli su volumi, origine dei destinatari, gestione rimbalzi → rispondere
  con i presidi di UC 0079 già in funzione; ri-sottomettere.
- **Limite/quota insufficienti dopo l'uscita**: l'uscita dalla modalità di prova e l'aumento della quota giornaliera possono
  essere concessi a scaglioni → se il tetto iniziale è basso per i volumi attesi, aprire una seconda richiesta di aumento.
- **Prod in regione diversa**: la richiesta è per-regione; se prod usasse una regione diversa da `test` andrebbe rifatta.
  Qui non accade: tutto su `eu-west-1`.
- **Go-live anticipato**: se il go-live viene avvicinato, questo UC va comunque completato prima — non c'è aggiramento tecnico.

## 6. Risorse & runbook
**Risorse**: nessuna infrastruttura da creare via Terraform; è un'azione operativa del founder sulla console AWS/SES della
regione `eu-west-1`. La firma DKIM e la verifica del dominio provengono da UC 0018.
**Runbook passo-passo**:
1. Verificare in SES che il dominio sia verificato e DKIM attivo.
2. Verificare che UC 0079 sia attivo (notifiche rimbalzi + lista di soppressione + allarme sul tasso).
3. Console SES → richiesta di uscita dalla modalità di prova / aumento limite → compilare motivazione d'uso e gestione
   rimbalzi → inviare.
4. Annotare data e numero del ticket; presidiare la casella per i chiarimenti.
5. A esito positivo, eseguire un invio reale di verifica (vedi UC 0081 §smoke email).
**Rollback**: non applicabile in senso stretto (non si "rientra" in modalità di prova volontariamente). Se il tasso di
rimbalzo/reclamo degenera, è SES stesso a poter **sospendere** l'account → mitigazione in UC 0079.

## 7. Dati toccati
Nessun dato personale nuovo trattato da questo UC. Le email transazionali (contenuto e destinatari) sono già inquadrate in
UC 0018 e nel manifesto dati/RoPA (Registro dei trattamenti). Qui si tocca solo la **configurazione** del servizio SES.

## 8. Permessi & gate
- Azione svolta con l'accesso alla console AWS dell'account (founder/platform engineer). Non passa dalla pipeline: è una
  richiesta manuale ad AWS, non modificabile via infrastruttura come codice.
- **Gate di ambiente**: si esegue su `test` prima del go-live; l'uscita dalla modalità di prova vale a livello di account per
  la regione, quindi copre anche `prod` nella stessa regione `eu-west-1`.

## 9. Requisiti di test / verifica
Non essendoci un test automatico possibile per una richiesta manuale ad AWS, la verifica è **dal vivo**:
- prima della richiesta: una email verso un indirizzo **non** verificato **fallisce** (conferma dello stato di prova);
- dopo l'esito positivo: la stessa email viene **consegnata**;
- l'email di verifica indirizzo e quella di reset password arrivano a un destinatario esterno reale (aggancio con lo smoke
  email di UC 0081, sia in italiano sia in inglese).

## 10. Riferimenti & Definition of Done
- **Fonte**: R12 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Recapito email SES".
- **DoD**: account SES fuori dalla modalità di prova nella regione `eu-west-1`; un invio reale verso un destinatario non
  pre-verificato viene consegnato; UC 0079 risulta attivo (prerequisito di sostanza soddisfatto).

## Punti aperti / decisioni differite
- **Quota giornaliera reale**: il valore concesso da AWS potrebbe non bastare ai volumi del lancio (UC 0043); se emerge, aprire
  una richiesta di aumento dedicata — traccia da tenere qui finché non si conoscono i volumi effettivi.
