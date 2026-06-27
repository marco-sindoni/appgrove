import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from './authStore'
import { logoutSession } from './authApi'
import { useConfig } from '../config'

/** Logout: invalida il cookie refresh lato server, azzera lo stato in memoria, torna al login. */
export function useLogout(): () => Promise<void> {
  const config = useConfig()
  const clear = useAuthStore((s) => s.clear)
  const navigate = useNavigate()

  return useCallback(async () => {
    await logoutSession(config.authBaseUrl)
    clear()
    navigate('/login')
  }, [config.authBaseUrl, clear, navigate])
}
