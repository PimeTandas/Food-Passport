import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ThemeToggle } from './ThemeToggle'

/** Top bar + the current page. Also the gatekeeper: no login, no passport. */
export function Layout() {
  const { user, loading, logout } = useAuth()
  const location = useLocation()

  if (loading) return <div className="page muted">Opening your passport…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          <span className="brand-emblem" aria-hidden>🍜</span>
          <span>Food Passport<small>BON APPÉTIT</small></span>
        </Link>
        <div className="topbar-actions">
          <ThemeToggle />
          <span className="avatar" title={`@${user.username}`} aria-label={`Logged in as ${user.username}`}>
            {user.username[0]?.toUpperCase()}
          </span>
          <button className="btn ghost small" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
