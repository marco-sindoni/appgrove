# Checklist di conformità — campagne cookieless (postura difesa)

Da applicare **a ogni step** del flusso, non solo alla fine. Ogni voce dice *cosa* verificare, *perché*
(quale pilastro tutela) e la **regola**: se la condizione ammessa non è soddisfatta, la configurazione va
**bloccata** e all'utente si spiega il motivo, proponendo l'alternativa ammessa. Fonte: #14 J48/J49.

| # | Cosa verificare | Perché | Regola |
|---|---|---|---|
| 1 | **Nessun pixel** Meta (Meta Pixel) né tag di remarketing Google sul sito vetrina | Sono tracker non essenziali: richiederebbero consenso e un banner cookie → contraddicono la postura cookieless | **BLOCCA** se la campagna prevede di installare un pixel. La misura si fa con Plausible + UTM, non col pixel |
| 2 | **Nessuna API di conversione server-to-server con dati personali** (Meta Conversions API, Google enhanced conversions) alimentata con email/telefono, anche in forma cifrata (hash) | È trattamento di dati personali per finalità di advertising senza base giuridica/consenso adeguati | **BLOCCA**. Nessun invio di dati personali alle piattaforme per attribuzione/ottimizzazione |
| 3 | **Obiettivo ammesso**: solo **Traffico** o **Lead Form native** | Gli obiettivi "Conversioni/Vendite/Catalogo" presuppongono pixel o conversioni server-to-server (voci 1–2) | **BLOCCA** gli obiettivi basati su tracciamento; degrada a **Traffico** |
| 4 | **Destinazione = pagina del sito vetrina** (che ha solo Plausible cookieless), mai una pagina con tracker aggiuntivi | Mantiene "zero tracking sul sito" oltre Plausible | **BLOCCA** destinazioni con tracker non essenziali |
| 5 | **Lead Form native**: i contatti restano **sulla piattaforma**, zero tracking sul sito | I lead sono contatti; il loro trattamento (consenso, double opt-in) segue UC 0039, non la campagna | Ammesso. Ricorda che l'iscrizione newsletter dei lead segue **UC 0039** |
| 6 | **Nessun banner cookie** reso necessario dalla campagna | Il banner non serve perché non ci sono tracker non essenziali; introdurne uno contraddice la postura | **BLOCCA** qualsiasi scelta che imporrebbe un banner |
| 7 | **Impostazioni e strumenti con trattamento in Unione Europea**, nessuno strumento di tracciamento aggiuntivo | Coerenza con la linea "purista" sulla residenza dei dati | Preferisci le opzioni con trattamento in Unione Europea dove la piattaforma le offre; niente strumenti terzi di tracking |
| 8 | **UTM presenti e coerenti** su ogni URL di destinazione | Senza UTM, Plausible non può attribuire la campagna (unica via cookieless) | **BLOCCA** URL senza UTM; applica `reference/convenzioni-utm.md` |

## Trade-off accettato (da dichiarare all'utente)
Rinunciando a pixel e conversioni server-to-server, l'**attribuzione** e l'**ottimizzazione automatica** delle
piattaforme sono **più deboli**. È una scelta consapevole (#14 J48): si privilegia la **coerenza di brand e
privacy**. La misura resta possibile con **UTM + goal Plausible + click nativi** della piattaforma.

## Nota
Questa checklist è il presidio funzionale della skill; non sostituisce una revisione legale. La postura di
riferimento è #14 J48. Se una piattaforma introduce una modalità realmente cookieless e senza dati personali
per un obiettivo oggi bloccato, è un **punto da rivalutare** in UC 0043 (lancio), non da forzare qui.
