# E22.1 — Fondamenta del modello centralizzato

**Epica madre**: [Epica 22](E22-00-rifacimento-modello-appartenenza.md) · **Storie**: **0098 ✅**, 0099, 0100, 0101
**Stato**: 🟡 in corso (0098 implementata dalla change 0091) · **Ultimo aggiornamento**: 2026-08-21

## Obiettivo

> Non è più la **prima** sotto-epica a eseguirsi: la precede
> [E22.5 — Identità e appartenenze](E22-05-identita-e-appartenenze.md), che scioglie il vincolo «una persona,
> un solo account». La tabella degli accessi di 0098 nasce quindi riferendo l'**identità**.

Costruire le fondamenta su cui tutto il resto poggia: **dove vive il ruolo**, **come lo si fa
rispettare**, **che aspetto ha l'elenco delle persone** e **che cosa significano esattamente i tre
ruoli**. Nessuna di queste quattro storie porta valore visibile da sola al cliente; insieme rendono
possibile tutto il resto, e sbagliarle costringerebbe a rifare le altre dodici.

## Perché in questo ordine

1. **0098 — i dati** ✅ *(change 0091)*. Prima serve un posto dove scrivere «Marta è `admin` sul
   Mini-CRM». Finché non esiste, ogni discorso su menu, prezzi e schermate è aria. La storia porta anche la
   mutazione del ruolo di piattaforma da tre valori a due (`owner` e `member`) — fatta
   nell'**enumerazione**, mentre la conversione dei dati reali resta a UC 0113.
2. **0099 — l'autorizzazione.** Avere il dato non basta: i servizi devono **negare** in base ad esso, e
   devono farlo in un modo unico e riusabile, non applicazione per applicazione. Qui si decide la cosa
   più delicata dell'intera epica: **il ruolo per applicazione non entra nel token**.
3. **0101 — il contratto dei ruoli.** Prima di scrivere una sola schermata serve una definizione di
   `viewer`/`editor`/`admin` abbastanza precisa perché due applicazioni diverse la interpretino allo
   stesso modo. È un documento *più* un pezzo di codice condiviso, non solo prosa.
4. **0100 — l'elenco unico.** Ultima delle quattro perché è la prima cosa che si **vede**, e vederla
   prima che i tre pilastri esistano darebbe l'illusione che il modello sia pronto.

## Le decisioni portanti

**L'accesso è una entità, non un campo.** Nasce `platform.app_access`: una riga per ogni terna
(account, applicazione, persona) con il suo ruolo. L'alternativa — una lista di ruoli dentro l'utente —
è stata scartata: non si potrebbe interrogare («chi ha accesso al Mini-CRM?»), non si potrebbe vincolare
e non si potrebbe verificare riga per riga come impone l'invariante di separazione fra account.

**Il ruolo di piattaforma scende a due valori.** `owner` e `member`. Il valore `admin` di oggi
**scompare da quel livello** e riappare, con un significato molto più circoscritto, sull'applicazione.
Chi oggi è `admin` diventa `member` con ruolo `admin` su ogni applicazione dell'account: la migrazione
(UC 0113) non toglie poteri a nessuno il giorno del rilascio.

**Il ruolo per applicazione non entra nel token.** È la decisione più importante e la meno visibile. Se
i ruoli finissero nei claim, un cambio di ruolo avrebbe effetto solo al rinnovo del token: una persona
retrocessa resterebbe `admin` per minuti. E un account con dieci applicazioni gonfierebbe ogni
richiesta con dieci coppie. Nel token restano quindi identità, account e **solo** il ruolo di
piattaforma; il ruolo per applicazione si legge dal core con lo **stesso meccanismo già collaudato per
i diritti d'accesso** (`services/commons/.../entitlement/projection/`), che tiene una copia locale
invalidata dagli eventi.

**L'owner è per costruzione onnipotente e indistruttibile.** Non ha bisogno di righe di accesso: gli
sono implicite su tutte le applicazioni dell'account. Non è retrocedibile né rimovibile — l'argine
esiste già oggi come regola dell'«ultimo owner» e va reso strutturale.

## Come si vede che ha funzionato

- Esiste una domanda con una sola risposta possibile: «che ruolo ha questa persona su questa
  applicazione?», e ogni pezzo di sistema la fa allo stesso posto.
- Un cambio di ruolo si sente **entro pochi secondi**, senza che l'utente rientri.
- La sezione «Members» non mostra ruoli, e nessuno la scambia più per il posto in cui si decidono i
  poteri.
- Un servizio nuovo ottiene il rispetto dei ruoli **dichiarandolo**, non riscrivendolo.

## Rischi propri di questa sotto-epica

| Rischio | Mitigazione |
|---|---|
| La copia locale del ruolo resta indietro e una persona conserva poteri revocati | Invalidazione a eventi come per i diritti d'accesso, più durata massima breve della copia; le operazioni **distruttive** rileggono dal core |
| La migrazione a due ruoli di piattaforma tronca i poteri di qualcuno | UC 0113 traduce `admin` in accesso `admin` su tutte le applicazioni esistenti |
| Il varco riusabile viene aggirato da un'applicazione distratta | UC 0112: il generatore di applicazioni lo mette già cablato, e il collaudo di parità lo pretende |
