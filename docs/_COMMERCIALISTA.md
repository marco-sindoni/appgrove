# Inquadramento fiscale & commercialista (documento vivo)

Registro **unico** delle questioni **fiscali/contributive** legate all'avvio di appgrove come **persona fisica senza
società** che vende SaaS tramite **Paddle (Merchant of Record)**. Raccoglie note, certezze, rischi e i **punti da far
verificare a un commercialista**.

> **Natura**: queste sono note operative, **non consulenza fiscale**. La regola generale ("l'obbligo P.IVA nasce
> dall'attività abituale, non dal payout") è solida; le **specifiche numeriche e di classificazione** vanno confermate
> da un **commercialista** (consulenza una tantum, economica). A differenza della revisione legale dei testi (opzionale,
> [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md)), il **setup fiscale di base è concreto e raccomandato**: errori = sanzioni reali.

Origine: discussione #14, 2026-06-21.

## 1. Lato Paddle — ✅ nessuna società necessaria
- Ci si registra come **"Individual"** (persona fisica / ditta individuale): si forniscono info fiscali per la verifica
  identità, **nessun documento societario/quote**. Per i sole trader Paddle preferisce il **nome legale nei T&C**.
- **La strategia MoR-senza-azienda è pienamente supportata da Paddle.**
- Fonti: [Do You Need to Be Incorporated to Sell via Paddle?](https://www.boathouse.co/paddle-video-series-episode/3-do-you-need-to-incorporate-to-sell-with-paddle) ·
  [Business Identification – Paddle](https://www.paddle.com/help/start/account-verification/what-is-business-verification)

## 2. Verifica account & payout Paddle
- Prima di agire da MoR, Paddle verifica in 3 fasi: **Domain Review** (5–7 gg se manuale) → **Business Verification**
  (2–4 gg) → **Identity Verification** (>25% owner, o identità della persona fisica). **Finché non sei verificato non vai
  live** (non vendi/incassi davvero).
- Payout: il **1° del mese**, inviato **entro il 15** (+ fino a 3 gg lavorativi in banca), **solo se** il saldo raggiunge
  la **soglia minima €100**; sotto soglia il saldo **si accumula** al mese successivo.
- Fonti: [When and how do I get paid?](https://www.paddle.com/help/manage/get-paid/when-and-how-do-i-get-paid) ·
  [Payout settings](https://www.paddle.com/help/manage/get-paid/how-do-i-set-up-my-payout-settings)

## 3. Lato fisco italiano — la regola chiave
- **La partita IVA è obbligatoria per chi svolge attività in modo ABITUALE e CONTINUATIVO, a prescindere dal fatturato.**
  Criteri Agenzia Entrate: **abitualità, continuità, professionalità**.
- Un **SaaS ad abbonamento ricorrente è strutturalmente abituale/continuativo** → la **prestazione occasionale** (senza
  P.IVA), pensata per atti **isolati e sporadici**, **non è il vestito giusto**.
- ⚠️ I **5.000€** NON sono una soglia fiscale: sono il limite **contributivo INPS** (Gestione Separata). Non scudano dall'obbligo P.IVA.
- Fonti: [Partita IVA o Lavoro Occasionale? – TaxMan](https://www.taxmanapp.it/blog/2026/02/12/partita-iva-o-lavoro-occasionale/) ·
  [P.IVA e prestazione occasionale – ilCommercialistaOnline](https://www.ilcommercialistaonline.it/partita-iva-e-prestazione-occasionale-sono-compatibili/)

## 4. P.IVA ≠ "azienda"
- Aprire una **partita IVA in regime forfettario come ditta individuale** NON è "aprire un'azienda": è leggero, economico
  (poche centinaia di €/anno di commercialista), **flat tax** (imposta sostitutiva ~5% i primi 5 anni se requisiti nuova
  attività, poi 15%), contabilità semplificata. **Niente SRL, capitale, burocrazia societaria.**

## 5. Equivoco smontato: il PAYOUT non è il fatto che conta
- **L'obbligo P.IVA scatta con l'attività abituale, NON con il prelievo** (né con l'importo). Far girare abbonamenti
  ricorrenti a pagamento in modo continuativo → obbligo esistente **anche se non prelevi mai** e **a prescindere dalla cifra**.
- **I soldi nel saldo Paddle sono già reddito tuo** (credito che controlli): tenerli su Paddle **non li rende "non
  guadagnati"**. L'imputazione esatta del saldo trattenuto è dettaglio da commercialista, ma **l'obbligo P.IVA precede il payout**.
- **Cadenza/numero dei payout** (es. 2-3 l'anno) e **importi piccoli** (es. 2-3K€) **non cambiano la natura abituale**
  dell'attività: non si può "vestire" il SaaS ricorrente da prestazione occasionale.

## 6. Scenario "paga-da-subito, P.IVA dopo ~6 mesi" — rischi (richiesto 2026-06-21)
- **In Italia NON esiste un "periodo di prova" legale** in cui si può essere abituali senza P.IVA: l'abitualità di un SaaS
  ad abbonamento scatta **dall'inizio**. Quindi quel piano = **attività commerciale abituale senza P.IVA** = fattispecie
  **non conforme** (non una zona grigia consentita).
- **Rischi concreti** se si opera così: (1) **apertura P.IVA retroattiva** + **IRPEF arretrata**; (2) **contributi INPS**
  se dovuti; (3) **sanzioni** (tardiva apertura P.IVA, omessa fatturazione, omessa/infedele dichiarazione — % sugli
  importi con minimi fissi) + **interessi**; (4) **tracciabilità** (payout Paddle sul conto, tracciati).
- **Magnitudo pratica (onestà)**: su importi piccoli l'esposizione in € è modesta e la probabilità di accertamento mirato
  è bassa; esiste il **ravvedimento operoso** (sanzioni molto ridotte se ci si mette in regola spontaneamente). **MA
  "piccolo + improbabile + ravvedimento" ≠ "legale"**: è rischio piccolo, non zero.
- **Ribaltamento di convenienza**: la forfettaria da subito costa **pochissimo** (~5% su poco reddito = decine di €;
  commercialista poche centinaia di €). Rimandare di 6 mesi **risparmia quasi nulla** e ti espone inutilmente.

## 7. Le vie compliant (raccomandate)
- **Via A — valida la domanda SENZA incassare ricorrente** (niente attività abituale → niente P.IVA): sito vetrina live
  + **landing con prezzo reale + waitlist/"founding member"** (misuri willingness-to-pay) + eventuali app **free/freemium**.
- **Via B — paga-da-subito IN REGOLA**: apri la **forfettaria nel momento in cui accendi il primo abbonamento a
  pagamento** (costo trascurabile, rischio zero, tranquillità). Eventuale **pilot a pagamento** dopo l'apertura.
- Il "trigger" giusto **non** è "ho accumulato abbastanza", ma "**sto per far partire un'attività a pagamento abituale**".

## 8. Checklist da verificare con il commercialista (consulenza una tantum)
| # | Punto | Stato |
|---|---|---|
| F1 | Requisiti **regime forfettario** + **codice ATECO** corretto per vendita SaaS/software | ⏳ |
| F2 | **INPS Gestione Separata** (iscrizione, aliquote, soglia 5.000€) | ⏳ |
| F3 | **Classificazione dei payout Paddle** (Paddle MoR estero UK/IE → come si fattura/imputa il revenue share) | ⏳ |
| F4 | **IVA / reverse-charge / VIES** sul rapporto B2B con Paddle (servizi/licenza a soggetto estero) | ⏳ |
| F5 | **Timing esatto** di apertura P.IVA rispetto all'avvio del pagato | ⏳ |
| F6 | Imputazione fiscale del **saldo trattenuto da Paddle** (competenza vs cassa nel forfettario) | ⏳ |
| F7 | Eventuale **ravvedimento** se si è già operato prima dell'apertura | ⏳ (solo se applicabile) |
| F8 | **Sede ditta individuale = domiciliazione / virtual office** (scelta #14 D23): impostare un **indirizzo commerciale** (poche centinaia €/anno) per non pubblicare l'indirizzo di casa su privacy policy/T&C/Paddle. Verificare implicazioni (sede legale vs operativa, costi, provider) | ⏳ |
| F9 | **Identità titolare già in fase free**: pubblicando il sito si tratta dati → si è **titolari del trattamento** anche prima della P.IVA. In fase free il titolare è la **persona fisica** (nome+indirizzo+email); alla monetizzazione si aggiunge la **P.IVA (ditta individuale)** e si aggiornano i documenti | ⏳ |

## Impatti su altre aree
- [#14 Sito vetrina & legale](14-sito-vetrina-legale.md) (entità legale L11 = persona fisica/ditta individuale),
  [09-pagamenti](09-pagamenti.md) (Paddle MoR), [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md) (L11), memoria `paddle-activation-blocker`.
