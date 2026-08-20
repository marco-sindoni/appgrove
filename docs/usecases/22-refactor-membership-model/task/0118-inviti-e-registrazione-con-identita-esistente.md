# Piano di lavoro — UC 0118 · Inviti e registrazione con identità esistente

**Storia**: [0118](../story/0118-inviti-e-registrazione-con-identita-esistente.md) · **Aree toccate**: `services/auth`, `services/core`, `frontend/apps/backoffice`
**Dimensione stimata**: media · **Prerequisiti**: [UC 0116](0116-identita-e-appartenenze.md), [UC 0117](0117-account-attivo-e-selettore.md)

## Passo 1 — L'invio dell'invito

**Modifica**: [InvitationResource.java](../../../../services/core/src/main/java/app/appgrove/core/platform/InvitationResource.java).

Il controllo di oggi copre un solo caso («esiste già un invito in attesa in questo account»). Vanno
distinti tre esiti, e la distinzione è **di sicurezza**, non di comodità:

| Situazione | Esito | Perché |
|---|---|---|
| già membro di **questo** account | rifiuto con messaggio chiaro | informazione dell'account: lecita |
| invito già in attesa in **questo** account | rifiuto con messaggio chiaro | idem |
| l'identità esiste **altrove** (o non esiste) | **stesso** esito nei due casi | l'esistenza di un rapporto fra quella persona e la piattaforma non è informazione dell'account |

Il terzo caso è il punto delicato: nessuna differenza di codice, di corpo o di percorso. Il modello è già in
casa — le risposte neutre di
[AuthResource.java](../../../../services/auth/src/main/java/app/appgrove/auth/AuthResource.java)
(«risposta neutra», «neutra contro l'enumerazione») esistono per la stessa ragione.

**Migrazione**: `platform.invitations` acquista `identity_id` (annullabile) e lo stato «rifiutato».
`identity_id` si valorizza **lato server** al momento dell'invio quando l'identità esiste: serve
all'accettazione, e non viene mai restituito a chi invita.

## Passo 2 — L'accettazione, due strade

**Modifica**: [AuthResource.java](../../../../services/auth/src/main/java/app/appgrove/auth/AuthResource.java)
`/invitations/accept` e il metodo `acceptInvitation` dei due fornitori
([CognitoIdentityProvider](../../../../services/auth/src/main/java/app/appgrove/auth/cognito/CognitoIdentityProvider.java),
[LocalIdentityProvider](../../../../services/auth/src/main/java/app/appgrove/auth/local/LocalIdentityProvider.java)).

- **identità inesistente** → percorso di oggi: parola d'accesso, nome, lingua; si crea identità +
  appartenenza;
- **identità esistente** → si **richiede l'autenticazione** e si crea la sola appartenenza. Nessuna parola
  d'accesso nuova: una seconda parola d'accesso sarebbe una seconda identità mascherata. L'indirizzo
  autenticato deve **coincidere** con quello invitato, altrimenti l'invito diventa trasferibile.

**Interfaccia**: l'accettazione da parte di chi è già dentro va mostrata come **consenso nell'applicazione**
(«l'azienda X ti invita ad accedere») e non come un modulo di registrazione. Serve un punto in cui si vedono
gli inviti in attesa: oggi non esiste — collocarlo nell'intestazione accanto al selettore di
[UC 0117](0117-account-attivo-e-selettore.md), che è dove la persona guarda quando pensa «a quali account
appartengo».

## Passo 3 — La registrazione di chi è già membro

**Modifica**: [PlatformWriter.java](../../../../services/auth/src/main/java/app/appgrove/auth/PlatformWriter.java)
`createAccountWithOwner` — se l'identità esiste, **non** la ricrea: si autentica e crea account +
appartenenza `owner`. Già toccato da UC 0116; qui si completa il caso.

**Attenzione**: il percorso di registrazione chiede parola d'accesso e nome. Per chi ha già un'identità
quei campi non vanno chiesti: il percorso diventa «entra e crea il tuo account». Va reso nell'interfaccia,
altrimenti la persona crea una seconda identità con un indirizzo diverso — ed è il difetto che poi si paga
in assistenza (unione di identità, fuori scope e sgradevole).

## Passo 4 — I posti

**Nessuna modifica al calcolo**: il posto si paga in ogni percorso, come in
[UC 0103](../story/0103-acquisto-anticipato-posto-invito.md), e la presenza dell'identità altrove non c'entra. Ciò che
serve è **il testo mostrato al cliente**, perché la reazione naturale è «ma la paga già l'altra azienda»:
una riga nella schermata dell'invito che dice che il posto è dell'account, non della persona.

## Passo 5 — Collaudi

- **Integrazione, percorso A e percorso B** interi.
- **Sicurezza, la prova che tiene la riservatezza**: l'invio dell'invito a un indirizzo con identità
  esistente e a uno inesistente produce risposte **indistinguibili** — stesso codice, stesso corpo. Da
  scrivere accanto ai collaudi già esistenti sulle risposte neutre, non in un file nuovo.
- **Integrazione, collisioni legittime**: già membro / invito in attesa → messaggi distinti e chiari.
- **Integrazione, indirizzo non coincidente** all'accettazione → rifiuto.
- **Posti**: senza posti disponibili l'invito è rifiutato anche se l'identità esiste già.
- **Percorsi end-to-end** `J-INVITE-EXISTING`: un'azienda invita una persona che ha già un proprio account;
  la persona accetta dall'applicazione e passa fra i due account. Da registrare in
  [copertura-e2e.yaml](../../../testing/copertura-e2e.yaml).

## Verifica finale

```bash
cd services && mvn -B test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **Il messaggio d'aiuto che rivela**: «questa persona ha già un account» è utile e inaccettabile. La
   tentazione tornerà a ogni revisione dell'interfaccia: scrivere il perché accanto al codice.
2. **Chiedere una parola d'accesso a chi ce l'ha già**: crea una seconda identità mascherata e si scopre
   tardi.
3. **L'invito trasferibile**: se non si verifica la coincidenza degli indirizzi, un invito inoltrato ad
   altri funziona.
4. **I tempi di risposta** possono rivelare quello che il corpo non dice: se il percorso «identità
   esistente» è sensibilmente più lento, la differenza è osservabile. Non serve una difesa sofisticata,
   serve non introdurre lavoro sproporzionato in uno dei due rami.
