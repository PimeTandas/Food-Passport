import { Link, useLocation, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { InkStamp } from '../components/InkStamp'
import { ErrorMessage, Loading } from '../components/Status'
import { useAsync } from '../useAsync'

export function CountryPage() {
  const { code = '' } = useParams()
  const location = useLocation()
  const justStamped = (location.state as { justStamped?: number } | null)?.justStamped
  const { data, error, loading, reload } = useAsync(() => api.countryPage(code), [code])

  if (loading && !data) return <Loading />
  if (error) return <ErrorMessage error={error} onRetry={reload} />
  if (!data) return null

  const { country, stamps } = data
  return (
    <div className="stack" style={{ gap: 18 }}>
      <Link to="/" className="muted small">← Passport</Link>

      <div className="spread">
        <div className="country-header">
          <span className="flag" aria-hidden>{country.flag}</span>
          <div>
            <h1>{country.name}</h1>
            <span className="muted">{country.continent} · {stamps.length} stamp{stamps.length === 1 ? '' : 's'}</span>
          </div>
        </div>
      </div>

      <Link to={`/countries/${country.code}/stamp`} className="btn stamp-it">🖋️ Stamp this page</Link>

      <section className="visa-page" aria-label={`${country.name} stamps`}>
        {stamps.length === 0 ? (
          <div className="empty">
            <div className="big">{country.flag}</div>
            <p>No stamps yet. What's the first {country.name} dish you'll try?</p>
          </div>
        ) : (
          <div className="stamp-grid">
            {stamps.map((s) => <InkStamp key={s.id} stamp={s} landing={s.id === justStamped} />)}
          </div>
        )}
      </section>
    </div>
  )
}
