// Articolo cluster "Miglior software di fatturazione GDPR per PMI" (UC 0042): confronto
// question-based (GEO) che rimanda al pilastro `fatturazione-pmi-ue` (pillarKey) e alla
// landing dell'app faro `fatture`. Parità 5 lingue a compile-time.
import type { BlogPost } from '../types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const softwareFatturazioneGdprPmi: BlogPost = {
  key: 'software-fatturazione-gdpr-pmi',
  kind: 'article',
  datePublished: '2026-07-26',
  appId: 'fatture',
  pillarKey: 'fatturazione-pmi-ue',
  content: { en, it, fr, es, de },
}
