// Articolo cluster "Fattura elettronica a norma" (UC 0042): how-to question-based che
// rimanda al pilastro `fatturazione-pmi-ue` (pillarKey) e alla landing dell'app faro
// `fatture` (internal linking risolto per lingua). Parità 5 lingue a compile-time.
import type { BlogPost } from '../types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const fatturaElettronicaANorma: BlogPost = {
  key: 'fattura-elettronica-a-norma',
  kind: 'article',
  datePublished: '2026-07-26',
  appId: 'fatture',
  pillarKey: 'fatturazione-pmi-ue',
  content: { en, it, fr, es, de },
}
