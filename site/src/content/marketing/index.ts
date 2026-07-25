// Indice dei contenuti marketing per lingua (UC 0037).
//
// Il tipo Record<Locale, MarketingContent> forza a COMPILE-TIME la presenza di
// tutte e 5 le lingue con la STESSA forma: una lingua mancante o una chiave in più
// non compila. È la garanzia di parità strutturale; il controllo post-build e il
// test vitest coprono i valori (nessuna stringa vuota, liste della stessa lunghezza).

import type { Locale } from '../../lib/i18n.ts'
import type { MarketingContent } from './types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const MARKETING: Record<Locale, MarketingContent> = { en, it, fr, es, de }

export type { MarketingContent } from './types.ts'
