import { create } from 'zustand'
import { decodeClaims, type SessionClaims } from './jwt'

export type AuthStatus = 'idle' | 'authenticated' | 'anonymous'

export interface AuthState {
  /** `idle` finché il refresh-on-load non ha risposto; poi `authenticated`/`anonymous`. */
  status: AuthStatus
  /** Access token **in memoria** (mai persistito: nessun localStorage/sessionStorage). */
  accessToken: string | null
  idToken: string | null
  claims: SessionClaims | null
  /** Imposta la sessione a partire dai token (decodifica i claim per la UX). */
  setSession: (tokens: { accessToken: string; idToken?: string | null }) => void
  /** Termina la sessione (logout o refresh fallito). */
  clear: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'idle',
  accessToken: null,
  idToken: null,
  claims: null,
  setSession: ({ accessToken, idToken }) => {
    const claims = decodeClaims(accessToken, idToken)
    if (!claims) {
      set({ status: 'anonymous', accessToken: null, idToken: null, claims: null })
      return
    }
    set({ status: 'authenticated', accessToken, idToken: idToken ?? null, claims })
  },
  clear: () => set({ status: 'anonymous', accessToken: null, idToken: null, claims: null }),
}))

/** Getter non-React per il data layer (api-client). Legge il token in memoria. */
export const getAccessToken = (): string | null => useAuthStore.getState().accessToken
