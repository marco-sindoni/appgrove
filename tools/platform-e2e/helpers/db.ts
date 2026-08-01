/**
 * db — asserzioni a livello di DATABASE per i journey (leak detector, #10 dec. 13).
 *
 * Interroga il Postgres del compose dev via `docker exec psql` (stessa tecnica del seed
 * dello smoke headless): nessuna dipendenza npm, nessuna credenziale oltre a quelle già
 * dentro il container. SOLO letture: i journey non manipolano mai lo stato dal DB — lo
 * creano dal browser o dalle API, come farebbe un utente.
 */
import { execFileSync } from 'node:child_process'

const CONTAINER = process.env.PLATFORM_PG_CONTAINER ?? ''
const PG_USER = process.env.POSTGRES_USER ?? 'appgrove'
const PG_DB = process.env.POSTGRES_DB ?? 'appgrove'

/**
 * Esegue una SELECT e ritorna le righe come array di colonne (stringhe).
 * I valori dei filtri passano SEMPRE da `params` ($1, $2, …): mai concatenare input nel SQL.
 */
export function dbRows(sql: string, params: string[] = []): string[][] {
  if (!CONTAINER) throw new Error('PLATFORM_PG_CONTAINER non impostato: db() richiede lo stack della suite (run.sh)')
  const bind = params.map((p, i) => `\\set p${i + 1} '${p.replace(/'/g, "''")}'`).join('\n')
  const query = sql.replace(/\$(\d+)/g, (_, n) => `:'p${n}'`)
  const out = execFileSync(
    'docker',
    ['exec', '-i', CONTAINER, 'psql', '-qtA', '-F', '\t', '-U', PG_USER, '-d', PG_DB, '-v', 'ON_ERROR_STOP=1'],
    { input: `${bind}\n${query};`, encoding: 'utf8' },
  )
  return out
    .split('\n')
    .filter((l) => l.length > 0)
    .map((l) => l.split('\t'))
}

/** Come dbRows ma pretende UNA sola riga (con messaggio parlante in caso contrario). */
export function dbRow(sql: string, params: string[] = []): string[] {
  const rows = dbRows(sql, params)
  if (rows.length !== 1) {
    throw new Error(`attesa 1 riga, trovate ${rows.length} — query: ${sql} — parametri: [${params.join(', ')}]`)
  }
  return rows[0]
}
