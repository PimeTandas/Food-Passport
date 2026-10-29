import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, getToken, setToken, setUnauthorizedHandler } from '../api/client'
import type { UserSummary } from '../api/types'

interface AuthState {
  user: UserSummary | null
  /** true while we check a saved token on first load */
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

/**
 * Holds "who is logged in" for the whole app. Any component can call useAuth() to read it.
 * The token lives in localStorage so a page refresh keeps you logged in.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null)
  const [loading, setLoading] = useState(() => getToken() !== null)

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!getToken()) return
    // We have a saved token: ask the server who it belongs to (and whether it's still valid).
    api.me()
      .then((me) => setUser({ id: me.id, username: me.username }))
      .catch(() => logout())
      .finally(() => setLoading(false))
  }, [logout])

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    logout,
    login: async (email, password) => {
      const res = await api.login(email, password)
      setToken(res.token)
      setUser(res.user)
    },
    register: async (email, username, password) => {
      const res = await api.register(email, username, password)
      setToken(res.token)
      setUser(res.user)
    },
  }), [user, loading, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
