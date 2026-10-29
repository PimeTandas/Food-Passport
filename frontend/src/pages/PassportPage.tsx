import { ChevronRight, Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { CountryListItem } from '../api/types'
import { ErrorMessage, Loading } from '../components/Status'
import { mrzLines, pageLabel, passportDate, passportNumber } from '../format'
import { useAsync } from '../useAsync'

const CONTINENTS = ['Africa', 'Asia', 'Europe', 'North America', 'South America', 'Oceania', 'Antarctica']

export function PassportPage() {
  // Three requests at once; the page waits for all of them.
  const { data, error, loading, reload } = useAsync(
    () => Promise.all([api.me(), api.passport(), api.countries()]), [])
  const [query, setQuery] = useState('')
  const [continent, setContinent] = useState<string | null>(null)

  const [visited, unvisited] = useMemo(() => {
    if (!data) return [[], []] as [Row[], Row[]]
    const q = query.trim().toLowerCase()
    // Page numbers come from the alphabetical order, before any filtering.
    const rows = data[2].map((c, i) => ({ ...c, page: i + 1 }))
      .filter((c) => !continent || c.continent === continent)
      .filter((c) => !q || c.name.toLowerCase().includes(q) || c.code.toLowerCase() === q)
    return [rows.filter((c) => c.stampCount > 0), rows.filter((c) => c.stampCount === 0)]
  }, [data, query, continent])

  if (loading && !data) return <Loading text="Opening your passport…" />
  if (error) return <ErrorMessage error={error} onRetry={reload} />
  if (!data) return null

  const [me, summary] = data
  const continents = Object.keys(summary.countriesStampedPerContinent).length
  const [mrz1, mrz2] = mrzLines(me.username, me.id, me.createdAt, summary.totalStamps)

  return (
    <main className="page">
      <section className="datapage" aria-label="Passport data page">
        <header className="datapage-head">
          <strong>FOOD PASSPORT</strong>
          <span>Passeport gourmand</span>
        </header>
        <div className="datapage-body">
          <div className="portrait" aria-hidden>{me.username[0]?.toUpperCase()}</div>
          <dl className="datafields">
            <div className="wide"><dt className="label">Holder / Titulaire</dt><dd className="holder">{me.username}</dd></div>
            <div><dt className="label">Passport no.</dt><dd>{passportNumber(me.id)}</dd></div>
            <div><dt className="label">Date of issue</dt><dd>{passportDate(me.createdAt)}</dd></div>
            <div><dt className="label">Stamps</dt><dd>{summary.totalStamps}</dd></div>
            <div><dt className="label">Countries</dt><dd>{summary.countriesStamped} / {summary.totalCountries}</dd></div>
            <div><dt className="label">Continents</dt><dd>{continents} / 7</dd></div>
          </dl>
        </div>
        <div className="mrz" aria-hidden>{mrz1}{'\n'}{mrz2}</div>
      </section>

      <div className="stack" style={{ marginTop: 32 }}>
        <div className="input-icon">
          <Search strokeWidth={1.75} />
          <input className="input" type="search" placeholder="Search 249 countries" aria-label="Search countries"
                 value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
        <div className="filters" role="group" aria-label="Continent">
          <button className="filter" aria-pressed={continent === null} onClick={() => setContinent(null)}>All</button>
          {CONTINENTS.map((c) => (
            <button key={c} className="filter" aria-pressed={continent === c}
                    onClick={() => setContinent(continent === c ? null : c)}>{c}</button>
          ))}
        </div>
      </div>

      {visited.length > 0 && (
        <>
          <div className="section-head">
            <h2>Your pages</h2>
            <span className="faint small">{visited.length} {visited.length === 1 ? 'country' : 'countries'}</span>
          </div>
          <CountryList rows={visited} />
        </>
      )}

      <div className="section-head">
        <h2>{visited.length > 0 || summary.totalStamps > 0 ? 'Still to taste' : 'Where to first?'}</h2>
        <span className="faint small">{unvisited.length}</span>
      </div>
      {unvisited.length === 0
        ? <p className="muted">Nothing matches “{query}”.</p>
        : <CountryList rows={unvisited} />}
    </main>
  )
}

type Row = CountryListItem & { page: number }

function CountryList({ rows }: { rows: Row[] }) {
  return (
    <ul className="country-list">
      {rows.map((c) => (
        <li key={c.code}>
          <Link to={`/countries/${c.code}`} className={`country-row${c.stampCount ? '' : ' unvisited'}`}>
            <span className="flag" aria-hidden>{c.flag}</span>
            <span>
              <span className="name">{c.name}</span>
              <span className="sub" style={{ display: 'block' }}>{c.continent}</span>
            </span>
            {c.stampCount > 0
              ? <span className="count">{c.stampCount} {c.stampCount === 1 ? 'stamp' : 'stamps'}</span>
              : <span className="page-no">{pageLabel(c.page)}</span>}
            <ChevronRight strokeWidth={1.75} aria-hidden />
          </Link>
        </li>
      ))}
    </ul>
  )
}
