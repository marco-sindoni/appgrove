// Landing dell'app #1 "Fatture" (UC 0053): identità + stato a livello app, contenuto
// nelle 5 lingue. Bozza generata a mano (fatture precede new-application); passa a
// `published` solo tramite `finalize-landing publish`, dopo screenshot reali + OG +
// approvazione della copy (#14 51/52/35). Il tipo Record<Locale, …> garantisce la
// parità delle lingue a compile-time.
import type { Landing } from '../types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const fattureLanding: Landing = {
  appId: 'fatture',
  status: 'published',
  content: { en, it, fr, es, de },
}
