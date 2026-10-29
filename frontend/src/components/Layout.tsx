import { LogOut } from 'lucide-react'
import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Crest } from './Crest'
import { ThemeToggle } from './ThemeToggle'

/** Header + the current page. Also the gatekeeper: no login, no passport. */
export function Layout() {
  const { user, loading, logout } = useAuth()
  const location = useLocation()

  if (loading) return <div className="page muted">Opening your passport…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <Link to="/" className="wordmark">
            <Crest className="crest" />
            <span>Food Passport</span>
          </Link>
          <div className="topbar-actions">
            <ThemeToggle />
            <button className="icon-btn" onClick={logout} aria-label="Log out" title="Log out">
              <LogOut strokeWidth={1.75} />
            </button>
            <span className="avatar" title={`@${user.username}`}>{user.username[0]?.toUpperCase()}</span>
          </div>
        </div>
      </header>
      <Outlet />
    </>
  )
}
