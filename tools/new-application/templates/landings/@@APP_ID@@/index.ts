// Landing per-app di @@APP_NAME@@ (@@APP_ID@@) — BOZZA generata da `new-application` (UC 0046).
//
// Stato `draft`: le bozze NON vanno online (gate di pubblicazione #14 52). Solo la skill
// `finalize-landing` (UC 0057) porta la landing a `published`, dopo aver: catturato gli
// screenshot reali (uno per lingua), generato l'immagine Open Graph e rifinito il copy nelle
// 5 lingue con la review dell'utente. Finché è bozza:
//   - `screenshot.src` è `null` (il template mostra un placeholder on-brand);
//   - `meta.ogImage` è `null` (l'immagine social la genera `finalize-landing`);
//   - il badge dell'hero porta il sentinella «DA RIFINIRE» che il preflight di
//     `finalize-landing` rifiuta: non si pubblica una bozza col copy non rivisto.
//
// La transizione a `published` si fa in QUESTO file (un solo punto), cambiando `status`.
import type { Landing } from '../types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const @@APP_CAMEL@@Landing: Landing = {
  appId: '@@APP_ID@@',
  status: 'draft',
  content: { en, it, fr, es, de },
}
