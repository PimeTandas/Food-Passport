import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorMessage, Loading } from '../components/Status'
import { useAsync } from '../useAsync'

const CONTINENTS = ['Africa', 'Asia', 'Europe', 'North America', 'South America', 'Oceania', 'Antarctica']

export function PassportPage() {
  // Both requests run at the same time; we wait for both.
  const { data, error, loading, reload } = useAsync(
    () => Promise.all([api.passport(), api.countries()]), [])
  const [query, setQuery] = useState('')
  const [onlyStamped, setOnlyStamped] = useState(false)
  const [continent, setContinent] = useState<string | null>(null)

  const countries = useMemo(() => {
    if (!data) return []
    const q = query.trim().toLowerCase()
    return data[1]
      .filter((c) => !onlyStamped || c.stampCount > 0)
      .filter((c) => !continent || c.continent === continent)
      .filter((c) => !q || c.name.toLowerCase().includes(q) || c.code.toLowerCase() === q)
      // stamped countries first, then A-Z
      .sort((a, b) => Number(b.stampCount > 0) - Number(a.stampCount > 0) || a.name.localeCompare(b.name))
  }, [data, query, onlyStamped, continent])

  if (loading && !data) return <Loading text="Opening your passport…" />
  if (error) return <ErrorMessage error={error} onRetry={reload} />
  if (!data) return null

  const [summary] = data
  const percent = Math.round((summary.countriesStamped / summary.totalCountries) * 100)

  return (
    <div className="stack" style={{ gap: 18 }}>
      <section className="summary" aria-label="Passport summary">
        <div><strong>{summary.totalStamps}</strong><span>stamps</span></div>
        <div><strong>{summary.countriesStamped}</strong><span>countries</span></div>
        <div><strong>{Object.keys(summary.countriesStampedPerContinent).length}</strong><span>continents</span></div>
        <div className="progress" title={`${percent}% of the world`}>
          <div style={{ width: `${Math.max(percent, summary.countriesStamped ? 2 : 0)}%` }} />
        </div>
        <span style={{ gridColumn: '1 / -1' }}>
          {summary.countriesStamped === 0
            ? 'Your passport is empty. Pick a country and stamp your first dish!'
            : `${summary.countriesStamped} of ${summary.totalCountries} countries tasted`}
        </span>
      </section>

      <div className="stack">
        <input className="input" type="search" placeholder="Search countries…" aria-label="Search countries"
               value={query} onChange={(e) => setQuery(e.target.value)} />
        <div className="spread">
          <div className="toggle" role="group" aria-label="Show">
            <button aria-pressed={!onlyStamped} onClick={() => setOnlyStamped(false)}>All</button>
            <button aria-pressed={onlyStamped} onClick={() => setOnlyStamped(true)}>Stamped</button>
          </div>
          <span className="muted small">{countries.length} shown</span>
        </div>
        <div className="chips" role="group" aria-label="Continent">
          <button className="chip" aria-pressed={continent === null} onClick={() => setContinent(null)}>🌍 Everywhere</button>
          {CONTINENTS.map((c) => (
            <button key={c} className="chip" aria-pressed={continent === c}
                    onClick={() => setContinent(continent === c ? null : c)}>
              {c}{summary.countriesStampedPerContinent[c] ? ` · ${summary.countriesStampedPerContinent[c]}` : ''}
            </button>
          ))}
        </div>
      </div>

      {countries.length === 0 ? (
        <div className="empty"><div className="big">🧭</div>No countries match.</div>
      ) : (
        <div className="country-grid">
          {countries.map((c) => (
            <Link key={c.code} to={`/countries/${c.code}`}
                  className={`country-card${c.stampCount ? '' : ' unstamped'}`}>
              <span className="flag" aria-hidden>{c.flag}</span>
              <span className="name">{c.name}</span>
              {c.stampCount > 0 && <span className="mini-stamp" aria-label={`${c.stampCount} stamps`}>×{c.stampCount}</span>}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
