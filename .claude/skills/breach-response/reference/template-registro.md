# Template — voce del registro breach (art. 33.5)

La skill compila questa voce nella fase **DOCUMENT** e la propone da incollare in
[docs/compliance/breach-register.md](../../../../docs/compliance/breach-register.md). Riga sintetica per la
tabella-indice + sotto-sezione di dettaglio con tutti i campi. I campi sono **obbligatori** (per i non
applicabili, scrivere `N/A` con il perché). Per l'esito "improbabile", la **motivazione** è obbligatoria (art. 33.5).

## Riga per la tabella-indice

```
| BR-AAAA-NNN | <conoscenza: AAAA-MM-GG hh:mm> | <Titolare|Responsabile> | <Riservatezza|Integrità|Disponibilità> | <cat. interessati / n.> | <cat. dati> | <Improbabile|Rischio|Rischio elevato> | <Garante: Sì AAAA-MM-GG / No> | <Interessati: Sì AAAA-MM-GG / No> |
```

## Sotto-sezione di dettaglio

```markdown
### BR-AAAA-NNN — <titolo sintetico>

- **ID incidente / ticket**: BR-AAAA-NNN · ticket <rif>
- **Data/ora di conoscenza**: AAAA-MM-GG hh:mm (T0 delle 72h)
- **Data/ora di rilevazione o accadimento**: <se nota/stimabile, altrimenti N/A>
- **Ruolo di appgrove**: Titolare | Responsabile (per il trattamento <nome>)
- **Natura della violazione**: Riservatezza | Integrità | Disponibilità (una o più)
- **Descrizione dei fatti**: <cosa è successo, come, canale di detection (#08 / security.txt / tenant / fornitore)>
- **Categorie di interessati**: <utenti, membri di tenant, iscritti newsletter, …> — soggetti vulnerabili: Sì/No
- **Numero di interessati**: <esatto o stimato>
- **Categorie di dati**: <identificativi, credenziali, contenuti app, …> — categorie particolari art. 9: Sì (<quali>) / No
- **Cifratura / inintelligibilità**: <dati cifrati at-rest/in-transit? chiavi compromesse?> → leva art. 34.3: applicabile Sì/No
- **Effetti probabili per gli interessati**: <frode, furto d'identità, danno reputazionale/economico, discriminazione, …>
- **Misure adottate**: <contenimento, correzione, revoca token/credenziali, preservazione prove>
- **Misure proposte**: <azioni per attenuare gli effetti e prevenire recidive>
- **Esito della valutazione del rischio**: Improbabile | Rischio | Rischio elevato
  - **Motivazione**: <perché — obbligatoria; per "improbabile" spiega il "no-rischio", inclusa la leva cifratura se usata>
  - **Criteri EDPB pesati**: <tipo, natura/sensibilità/volume, identificabilità, gravità, vulnerabili, numero>
- **Decisione di notifica — Garante**: Sì (invio AAAA-MM-GG hh:mm; in fasi? Sì/No; ritardo oltre 72h? motivo) | No (motivo)
- **Decisione di notifica — Interessati**: Sì (AAAA-MM-GG; modalità: <email/avviso/…>) | No (motivo, es. leva cifratura art. 34.3)
- **Notifica al tenant-titolare** (se appgrove è responsabile): Sì (AAAA-MM-GG hh:mm) | N/A
- **Riferimenti**: <ticket, allegati/prove, corrispondenza con Garante/tenant/segnalante, bozze di notifica>
- **Stato**: Aperto | In notifica | Chiuso (data chiusura)
```
