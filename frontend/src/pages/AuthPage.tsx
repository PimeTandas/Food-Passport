import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Crest } from '../components/Crest'
import { ThemeToggle } from '../components/ThemeToggle'

/** The passport cover. One component for both screens: register just adds a username. */
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
    <div className="cover">
      <div className="cover-theme"><ThemeToggle /></div>
      <Crest className="cover-crest" withText />
      <div className="cover-title">
        <h1>FOOD PASSPORT</h1>
        <p>Passeport gourmand</p>
      </div>

      <form className="auth-card" onSubmit={submit} noValidate>
        <h2>{isRegister ? 'Apply for a passport' : 'Welcome back'}</h2>
        {error && Object.keys(error.fieldErrors).length === 0 && <div className="alert">{error.message}</div>}

        <label className="field">
          <span className="label">Email</span>
          <input className="input" type="email" autoComplete="email" required
                 value={email} onChange={(e) => setEmail(e.target.value)} />
          {fieldError('email')}
        </label>

        {isRegister && (
          <label className="field">
            <span className="label">Username</span>
            <input className="input" autoComplete="username" autoCapitalize="none" required
                   value={username} onChange={(e) => setUsername(e.target.value)} />
            <span className="faint small">How friends find you. Letters, numbers, _ and .</span>
            {fieldError('username')}
          </label>
        )}

        <label className="field">
          <span className="label">Password</span>
          <input className="input" type="password" required
                 autoComplete={isRegister ? 'new-password' : 'current-password'}
                 value={password} onChange={(e) => setPassword(e.target.value)} />
          {isRegister && <span className="faint small">At least 8 characters.</span>}
          {fieldError('password')}
        </label>

        <button className="btn block" disabled={busy}>
          {busy ? 'One moment…' : isRegister ? 'Issue my passport' : 'Log in'}
        </button>

        <p className="auth-switch">
          {isRegister
            ? <>Already have one? <Link to="/login">Log in</Link></>
            : <>New here? <Link to="/register">Apply for a passport</Link></>}
        </p>
      </form>
    </div>
  )
}
