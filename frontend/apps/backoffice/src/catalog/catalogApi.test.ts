import { describe, it, expect } from 'vitest'
import {
  CATALOG_PAGE_SIZE,
  describeApp,
  filterApps,
  paginate,
  tintFor,
  type CatalogApp,
} from './catalogApi'

const app = (over: Partial<CatalogApp> = {}): CatalogApp => ({
  appSlug: 'notes',
  name: 'Notes',
  category: 'amber',
  descriptions: { en: 'Shared notes for your team', it: 'Note condivise per il tuo team' },
  state: 'available',
  ...over,
})

describe('descrizione localizzata (UC 0095)', () => {
  it('usa la lingua attiva quando c’è', () => {
    expect(describeApp(app(), 'it')).toBe('Note condivise per il tuo team')
  })

  it('ripiega sull’inglese, poi sulla prima disponibile, poi sul vuoto', () => {
    expect(describeApp(app(), 'de')).toBe('Shared notes for your team')
    expect(describeApp(app({ descriptions: { fr: 'Notes partagées' } }), 'de')).toBe('Notes partagées')
    expect(describeApp(app({ descriptions: undefined }), 'it')).toBe('')
  })
})

describe('tinta di categoria (UC 0095)', () => {
  it('rispetta quella dichiarata a listino', () => {
    expect(tintFor(app({ category: 'teal' }))).toBe('teal')
  })

  it('per un’app senza categoria ne deriva una stabile dallo slug', () => {
    const senza = app({ category: undefined })
    expect(tintFor(senza)).toBe(tintFor(senza))
    // Una categoria inventata non passa: si ripiega sulla derivazione, non si stampa una classe assente.
    expect(tintFor(app({ category: 'fucsia' }))).not.toBe('fucsia')
  })
})

describe('ricerca (UC 0095)', () => {
  const apps = [
    app({ appSlug: 'notes', name: 'Notes' }),
    app({ appSlug: 'teams', name: 'Teams', descriptions: { en: 'Shared workspaces and seats' } }),
  ]

  it('filtra su nome e descrizione senza distinguere maiuscole e minuscole', () => {
    expect(filterApps(apps, 'TEAMS', 'en').map((a) => a.appSlug)).toEqual(['teams'])
    expect(filterApps(apps, 'workspaces', 'en').map((a) => a.appSlug)).toEqual(['teams'])
    expect(filterApps(apps, 'note', 'en').map((a) => a.appSlug)).toEqual(['notes'])
  })

  it('cerca nella descrizione della lingua attiva', () => {
    expect(filterApps(apps, 'condivise', 'it').map((a) => a.appSlug)).toEqual(['notes'])
    expect(filterApps(apps, 'condivise', 'en')).toHaveLength(0)
  })

  it('una ricerca vuota non filtra nulla', () => {
    expect(filterApps(apps, '   ', 'en')).toHaveLength(2)
  })

  it('nessun risultato è un elenco vuoto, non un errore', () => {
    expect(filterApps(apps, 'zzz', 'en')).toEqual([])
  })
})

describe('paginazione (UC 0095)', () => {
  const many = Array.from({ length: 14 }, (_, i) => app({ appSlug: `a${i}`, name: `App ${i}` }))

  it('taglia la pagina alla dimensione prevista e conta le pagine', () => {
    const first = paginate(many, 1)
    expect(first.items).toHaveLength(CATALOG_PAGE_SIZE)
    expect(first.pages).toBe(3)
    expect(paginate(many, 3).items).toHaveLength(2)
  })

  it('riporta dentro i limiti una pagina fuori intervallo invece di mostrare il vuoto', () => {
    expect(paginate(many, 99).page).toBe(3)
    expect(paginate(many, 0).page).toBe(1)
    expect(paginate(many, 99).items).toHaveLength(2)
  })

  it('un elenco vuoto ha comunque una pagina', () => {
    expect(paginate([], 1)).toEqual({ items: [], page: 1, pages: 1 })
  })
})
