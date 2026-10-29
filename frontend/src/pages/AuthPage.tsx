import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/** One component for both screens: the only difference is the username field and which API call. */
export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<ApiError | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const isRegister = mode === 'register'

  async function submit(e: FormEvent) {
    e.preventDefault() // stop the browser doing an old-fashioned full-page form post
    setBusy(true)
    setError(null)
    try {
      if (isRegister) await register(email, username, password)
      else await login(email, password)
      navigate((location.state as { from?: string } | null)?.from ?? '/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Could not reach the server'))
    } finally {
      setBusy(false)
    }
  }

  const fieldError = (name: string) =>
    error?.fieldErrors[name] && <span className="field-error">{error.fieldErrors[name]}</span>

  return (
    <div className="auth-wrap">
      <div className="passport-cover">
        <div className="emblem" aria-hidden>🍜</div>
        <h1>FOOD PASSPORT</h1>
        <div className="subtitle">BON APPÉTIT</div>

        <form className="card stack" onSubmit={submit} noValidate>
          <h2>{isRegister ? 'Get your passport' : 'Welcome back'}</h2>
          {error && Object.keys(error.fieldErrors).length === 0 && <div className="alert">{error.message}</div>}

          <label className="field">
            <span>Email</span>
            <input className="input" type="email" autoComplete="email" required
                   value={email} onChange={(e) => setEmail(e.target.value)} />
            {fieldError('email')}
          </label>

          {isRegister && (
            <label className="field">
              <span>Username</span>
              <input className="input" autoComplete="username" autoCapitalize="none" required
                     value={username} onChange={(e) => setUsername(e.target.value)} />
              <span className="muted small">Friends find you by this. Letters, numbers, _ and .</span>
              {fieldError('username')}
            </label>
          )}

          <label className="field">
            <span>Password</span>
            <input className="input" type="password" required
                   autoComplete={isRegister ? 'new-password' : 'current-password'}
                   value={password} onChange={(e) => setPassword(e.target.value)} />
            {isRegister && <span className="muted small">At least 8 characters.</span>}
            {fieldError('password')}
          </label>

          <button className="btn block" disabled={busy}>
            {busy ? 'One moment…' : isRegister ? 'Issue my passport' : 'Log in'}
          </button>

          <p className="muted small" style={{ textAlign: 'center', margin: 0 }}>
            {isRegister
              ? <>Already have one? <Link to="/login">Log in</Link></>
              : <>New here? <Link to="/register">Create an account</Link></>}
          </p>
        </form>
      </div>
    </div>
  )
}
