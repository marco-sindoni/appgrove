import { create } from 'zustand'
import type { EnforcementKind } from './enforcement'

/**
 * Store globale dell'ultimo esito di enforcement (UC 0028): alimentato dal `QueryClient` quando una
 * query/mutation fallisce con 402/429, letto dal banner globale. Un solo avviso alla volta (l'ultimo vince);
 * `clear` lo chiude.
 */
interface EnforcementState {
  kind: EnforcementKind | null
  notify: (kind: EnforcementKind) => void
  clear: () => void
}

export const useEnforcementStore = create<EnforcementState>((set) => ({
  kind: null,
  notify: (kind) => set({ kind }),
  clear: () => set({ kind: null }),
}))
