import { ArrowLeft, Stamp as StampIcon } from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { InkStamp } from '../components/InkStamp'
import { ErrorMessage, Loading } from '../components/Status'
import { pageLabel } from '../format'
import { useAsync } from '../useAsync'

export function CountryPage() {
  const { code = '' } = useParams()
  const location = useLocation()
  const justStamped = (location.state as { justStamped?: number } | null)?.justStamped
  // The country list gives us this country's page number (its place in the alphabet).
  const { data, error, loading, reload } = useAsync(
    () => Promise.all([api.countryPage(code), api.countries()]), [code])

  if (loading && !data) return <Loading />
  if (error) return <ErrorMessage error={error} onRetry={reload} />
  if (!data) return null

  const [{ country, stamps }, all] = data
  const page = all.findIndex((c) => c.code === country.code) + 1

  return (
    <main className="page">
      <Link to="/" className="back"><ArrowLeft strokeWidth={1.75} />Passport</Link>

      <header className="country-head">
        <div>
          <div className="country-meta">{pageLabel(page)} · {country.continent} · {country.code}</div>
          <h1>{country.name}</h1>
        </div>
        <span className="flag" aria-hidden>{country.flag}</span>
      </header>

      <div style={{ marginTop: 20 }}>
        <Link to={`/countries/${country.code}/stamp`} className="btn block">
          <StampIcon strokeWidth={1.75} />Stamp this page
        </Link>
      </div>

      <section className="visa-page" aria-label={`${country.name} stamps`}>
        <div className="visa-page-head">
          <span className="label">Visas · Stamps</span>
          <span className="label">{stamps.length} {stamps.length === 1 ? 'entry' : 'entries'}</span>
        </div>
        {stamps.length === 0 ? (
          <div className="empty">
            <StampIcon />
            <p>This page is still blank. What's the first {country.name} dish you'll try?</p>
          </div>
        ) : (
          <div className="stamp-grid">
            {stamps.map((s) => <InkStamp key={s.id} stamp={s} landing={s.id === justStamped} />)}
          </div>
        )}
        <div className="visa-page-foot">— {String(page).padStart(3, '0')} —</div>
      </section>
    </main>
  )
}
