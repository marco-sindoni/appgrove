// Registro delle landing per-app del sito vetrina (UC 0038).
//
// Ogni landing dichiara appId + status a livello app e il contenuto nelle 5 lingue.
// Il tipo Record<Locale, LandingLocaleContent> (in Landing) forza a compile-time la
// parità delle lingue. Le skill (new-application → bozza, UC 0046; finalize-landing
// → published, UC 0057) aggiungono/aggiornano voci qui dentro.
//
// `example` è la fixture dimostrativa in stato `draft`: collauda il template e i
// test senza pubblicare contenuti finti (le bozze non vanno online, #14 52).

import type { Landing } from './types.ts'
import { en } from './example/en.ts'
import { it } from './example/it.ts'
import { fr } from './example/fr.ts'
import { es } from './example/es.ts'
import { de } from './example/de.ts'

const example: Landing = {
  appId: 'example',
  status: 'draft',
  content: { en, it, fr, es, de },
}

export const LANDINGS: Landing[] = [example]

export type { Landing } from './types.ts'
