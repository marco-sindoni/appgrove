// Pilastro "Fatturazione per piccole imprese in UE" (UC 0042): testa di serie del tema,
// con i due articoli cluster che vi rimandano (clusterKeys). Rimanda alla landing
// dell'app faro `fatture` (internal linking risolto per lingua). Il tipo Record<Locale, …>
// garantisce la parità delle 5 lingue a compile-time.
import type { BlogPost } from '../types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const fatturazionePmiUe: BlogPost = {
  key: 'fatturazione-pmi-ue',
  kind: 'pillar',
  datePublished: '2026-07-26',
  appId: 'fatture',
  clusterKeys: ['fattura-elettronica-a-norma', 'software-fatturazione-gdpr-pmi'],
  content: { en, it, fr, es, de },
}
