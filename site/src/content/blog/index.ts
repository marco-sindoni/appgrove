// Registro dei contenuti del blog/risorse (UC 0042).
//
// Un array unico di post (pilastri + articoli cluster). Aggiungere un articolo = creare
// la cartella con i 5 file-lingua e appendere una entry qui: è la forma su cui poggerà la
// futura skill `new-blog-post` (docs/_BACKLOG.md). L'ordine dell'array è l'ordine di
// pubblicazione; l'indice ordina comunque per data. La validazione (lib/blog.ts, coperta
// dai test) fa da rete: parità 5 lingue, slug ben formati/non riservati/unici, coerenza
// pilastro↔cluster, appId collegato a una landing pubblicata.

import type { BlogPost } from './types.ts'
import { fatturazionePmiUe } from './fatturazione-pmi-ue/index.ts'
import { fatturaElettronicaANorma } from './fattura-elettronica-a-norma/index.ts'
import { softwareFatturazioneGdprPmi } from './software-fatturazione-gdpr-pmi/index.ts'

export const BLOG_POSTS: readonly BlogPost[] = [
  fatturazionePmiUe,
  fatturaElettronicaANorma,
  softwareFatturazioneGdprPmi,
]

export type { BlogPost } from './types.ts'
