import { useQuery } from '@tanstack/react-query'
import { useConfig } from '../config'
import { getAccessToken, useAuthStore } from './authStore'
import { twoFaStatus } from './authApi'

/** Chiave della lettura dello stato del secondo fattore: condivisa da Dashboard e pagina Sicurezza. */
export const TWOFA_STATUS_KEY = ['auth', '2fa', 'status'] as const

/**
 * Stato del secondo fattore dell'utente in sessione (UC 0097). Vive qui e non fra le letture del core
 * perché la sua fonte è il **servizio di autenticazione**, che non fa parte dello spec del core: la
 * chiamata passa quindi da `authApi`, non dal client tipizzato.
 *
 * <p>La lettura è abilitata solo a sessione autenticata. In caso di guasto non si assume nulla: chi la
 * usa mostra l'avviso soltanto quando la risposta dice esplicitamente "non attivo" — un errore di rete
 * non deve trasformarsi in un rimprovero a chi ha già fatto la cosa giusta.
 */
export function useTwoFaStatus() {
  const config = useConfig()
  const authenticated = useAuthStore((s) => s.status === 'authenticated')
  return useQuery({
    queryKey: TWOFA_STATUS_KEY,
    enabled: authenticated,
    queryFn: () => twoFaStatus(config.authBaseUrl, getAccessToken() ?? ''),
  })
}
