# Template — bozze di notifica (IT/EN)

> **BOZZE — validazione legale L12.** Questi testi sono **bozze solide** prodotte dal co-pilota, **non
> validate legalmente**. Prima di qualsiasi invio, revisione legale ([docs/_REVISIONE-LEGALE.md](../../../../docs/_REVISIONE-LEGALE.md),
> punto **L12**). I segnaposto `<…>` vanno compilati con i dati reali dell'incidente. Coprono i tre destinatari:
> **Garante** (art. 33), **interessati** (art. 34), **controller B2B / tenant-titolare** (appgrove responsabile).

**Entità e recapiti**: usa la denominazione legale e i recapiti reali del titolare (vedi manifesti/entità legale
#13); il **contatto privacy** è `privacy@appgrove.app`, il canale sicurezza è `security@appgrove.app`.

---

## 1. Notifica al Garante — art. 33 (titolare)

### IT
```
Oggetto: Notifica di violazione di dati personali — art. 33 GDPR — <denominazione titolare> — rif. BR-AAAA-NNN

Al Garante per la protezione dei dati personali,

ai sensi dell'art. 33 del Regolamento (UE) 2016/679, <denominazione titolare> notifica la seguente violazione
di dati personali.

1. Data e ora di conoscenza della violazione: <AAAA-MM-GG hh:mm>. La presente notifica è resa entro 72 ore /
   con un ritardo di <…> dovuto a <motivo> (art. 33.1).
2. Natura della violazione: <riservatezza/integrità/disponibilità> — <descrizione sintetica dei fatti e del
   canale di rilevazione>.
3. Categorie e numero approssimativo di interessati coinvolti: <categorie> — circa <n.>.
4. Categorie e numero approssimativo di registri di dati personali coinvolti: <categorie di dati; segnalare
   eventuali categorie particolari art. 9>.
5. Probabili conseguenze della violazione: <effetti probabili per gli interessati>.
6. Misure adottate o di cui si propone l'adozione per porre rimedio e attenuare i possibili effetti negativi:
   <contenimento, correzione, misure proposte>.
7. Dati di contatto del punto presso cui ottenere maggiori informazioni: <contatto privacy — privacy@appgrove.app>.

Ove non sia stato possibile fornire tutte le informazioni contestualmente, esse saranno fornite in fasi
successive senza ulteriore ingiustificato ritardo (art. 33.4).

<Luogo, data> — <firma / titolare>
```

### EN
```
Subject: Personal data breach notification — art. 33 GDPR — <controller legal name> — ref. BR-AAAA-NNN

To the supervisory authority (Garante per la protezione dei dati personali),

pursuant to art. 33 of Regulation (EU) 2016/679, <controller legal name> notifies the following personal data
breach.

1. Date and time the breach became known: <AAAA-MM-GG hh:mm>. This notification is provided within 72 hours /
   with a delay of <…> due to <reason> (art. 33(1)).
2. Nature of the breach: <confidentiality/integrity/availability> — <brief description of facts and detection
   channel>.
3. Categories and approximate number of data subjects concerned: <categories> — approx. <n.>.
4. Categories and approximate number of personal data records concerned: <data categories; flag any special
   categories under art. 9>.
5. Likely consequences of the breach: <likely effects on data subjects>.
6. Measures taken or proposed to address the breach and mitigate its possible adverse effects: <containment,
   remediation, proposed measures>.
7. Contact point for more information: <privacy contact — privacy@appgrove.app>.

Where it was not possible to provide all information at once, it will be provided in phases without undue
further delay (art. 33(4)).

<Place, date> — <signature / controller>
```

---

## 2. Comunicazione agli interessati — art. 34 (titolare, solo rischio elevato)

Linguaggio **chiaro e semplice** (art. 34.2). Da usare **solo** quando l'esito è "rischio elevato" e la leva
cifratura (art. 34.3) non esclude l'obbligo.

### IT
```
Oggetto: Informazioni importanti sulla sicurezza dei tuoi dati

Ciao <nome/generico>,

ti scriviamo per informarti di un incidente di sicurezza che potrebbe aver riguardato alcuni dei tuoi dati
personali.

- Cosa è successo: <descrizione chiara, senza gergo>.
- Quando: <AAAA-MM-GG> (ce ne siamo accorti il <…>).
- Quali dati potrebbero essere coinvolti: <categorie di dati in parole semplici>.
- Possibili conseguenze: <effetti probabili per te>.
- Cosa abbiamo fatto: <misure di contenimento e correzione>.
- Cosa ti consigliamo di fare: <es. cambia la password, attiva la verifica in due passaggi, fai attenzione a
  email sospette>.

Per qualsiasi domanda puoi scriverci a privacy@appgrove.app. Ci scusiamo per l'accaduto e restiamo a
disposizione.

<denominazione titolare>
```

### EN
```
Subject: Important information about the security of your data

Hi <name/generic>,

we are writing to inform you of a security incident that may have affected some of your personal data.

- What happened: <clear description, no jargon>.
- When: <AAAA-MM-GG> (we detected it on <…>).
- What data may be involved: <data categories in plain words>.
- Possible consequences: <likely effects on you>.
- What we have done: <containment and remediation measures>.
- What we recommend you do: <e.g. change your password, enable two-step verification, beware of suspicious
  emails>.

If you have any questions, write to us at privacy@appgrove.app. We are sorry for what happened and remain at
your disposal.

<controller legal name>
```

---

## 3. Notifica al tenant-titolare / controller B2B — art. 33.2 (appgrove responsabile)

Quando appgrove tratta i dati **per conto** di un'app/tenant B2B (ruolo responsabile), **non** notifica
direttamente Garante/interessati: **informa il tenant-titolare senza ritardo**, così che lui valuti e
notifichi. Fornisci gli elementi utili alla sua valutazione.

### IT
```
Oggetto: Notifica di violazione di dati personali — dati trattati per vostro conto — rif. BR-AAAA-NNN

Gentile <tenant/titolare>,

in qualità di responsabile del trattamento per i dati che trattiamo per vostro conto, vi informiamo senza
ritardo, ai sensi dell'art. 33.2 GDPR, della seguente violazione.

- Data/ora di conoscenza: <AAAA-MM-GG hh:mm>.
- Natura e descrizione: <riservatezza/integrità/disponibilità — fatti e canale di rilevazione>.
- Dati e interessati del vostro account potenzialmente coinvolti: <categorie di dati / interessati / stima numero>.
- Possibili conseguenze: <effetti probabili>.
- Misure adottate e proposte: <contenimento, correzione, misure a supporto>.
- Assistenza: siamo a disposizione per fornire ulteriori elementi utili alla vostra valutazione e agli
  eventuali obblighi di notifica che spettano a voi come titolari.

Contatto per il seguito: security@appgrove.app.

<denominazione responsabile (appgrove)>
```

### EN
```
Subject: Personal data breach notification — data processed on your behalf — ref. BR-AAAA-NNN

Dear <tenant/controller>,

as data processor for the data we process on your behalf, we inform you without delay, pursuant to art. 33(2)
GDPR, of the following breach.

- Date/time of awareness: <AAAA-MM-GG hh:mm>.
- Nature and description: <confidentiality/integrity/availability — facts and detection channel>.
- Data and data subjects of your account potentially involved: <data categories / data subjects / estimated number>.
- Possible consequences: <likely effects>.
- Measures taken and proposed: <containment, remediation, supporting measures>.
- Assistance: we are available to provide further information useful for your assessment and for any
  notification obligations that fall to you as controller.

Contact for follow-up: security@appgrove.app.

<processor legal name (appgrove)>
```
