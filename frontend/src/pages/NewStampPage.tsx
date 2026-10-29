import { ArrowLeft, Stamp as StampIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import { EntryFields, toEntryRequest, type EntryValues } from '../components/EntryFields'
import { FriendPicker } from '../components/FriendPicker'
import { StarRating } from '../components/StarRating'
import { ErrorMessage, Loading } from '../components/Status'
import { passportDate } from '../format'
import { useAsync } from '../useAsync'

const LAST_CURRENCY = 'foodpassport.lastCurrency'

function lastCurrency(): string {
  try { return localStorage.getItem(LAST_CURRENCY) ?? '' } catch { return '' }
}

export function NewStampPage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()
  const { data, error, loading, reload } = useAsync(
    () => Promise.all([api.countryPage(code), api.friends()]), [code])

  const [entry, setEntry] = useState<EntryValues>({
    dishName: '', placeName: '', homemade: false, priceAmount: '', priceCurrency: lastCurrency(),
  })
  const [rating, setRating] = useState<number | null>(null)
  const [notes, setNotes] = useState('')
  const [friendIds, setFriendIds] = useState<number[]>([])
  const [submitError, setSubmitError] = useState<ApiError | null>(null)
  const [busy, setBusy] = useState(false)

  if (loading && !data) return <Loading />
  if (error) return <ErrorMessage error={error} onRetry={reload} />
  if (!data) return null
  const [{ country }, friends] = data

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!entry.dishName.trim()) {
      setSubmitError(new ApiError(400, 'Some fields need fixing', { dishName: 'Tell us what you ate' }))
      window.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }
    setBusy(true)
    setSubmitError(null)
    try {
      const request = toEntryRequest(entry)
      const stamp = await api.createStamp(country.code, {
        ...request, rating: rating ?? undefined, notes: notes || undefined, withFriendIds: friendIds,
      })
      if (request.price) {
        try { localStorage.setItem(LAST_CURRENCY, request.price.currency) } catch { /* not important */ }
      }
      // Back to the page, where the new stamp lands with a small animation.
      navigate(`/countries/${country.code}`, { state: { justStamped: stamp.id } })
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err : new ApiError(0, 'Could not reach the server'))
      setBusy(false)
    }
  }

  const errors = submitError?.fieldErrors ?? {}
  return (
    <form onSubmit={submit} noValidate>
      <main className="page with-action-bar">
        <Link to={`/countries/${country.code}`} className="back"><ArrowLeft strokeWidth={1.75} />{country.name}</Link>
        <div className="country-meta">{country.flag} {country.code} · {passportDate(new Date().toISOString())}</div>
        <h1 style={{ marginBottom: 8 }}>New stamp</h1>

        {submitError && Object.keys(errors).length === 0 && <div className="alert">{submitError.message}</div>}

        <section className="form-section" aria-label="The meal">
          <EntryFields values={entry} onChange={setEntry} errors={errors} />
        </section>

        <section className="form-section">
          <div>
            <h2>Your verdict</h2>
            <p className="faint small">Only you see your rating and notes.</p>
          </div>
          <StarRating value={rating} onChange={setRating} />
          <label className="field">
            <span className="label">Notes</span>
            <textarea className="input" maxLength={2000} placeholder="How was it? Would you have it again?"
                      value={notes} onChange={(e) => setNotes(e.target.value)} />
          </label>
        </section>

        <section className="form-section">
          <div>
            <h2>Who were you with?</h2>
            <p className="faint small">They'll get this stamp in their passport too.</p>
          </div>
          <FriendPicker friends={friends} selected={friendIds} onChange={setFriendIds} />
        </section>
      </main>

      <div className="action-bar">
        <div className="action-bar-inner">
          <span className="small muted">
            {friendIds.length > 0 ? `You + ${friendIds.length} ${friendIds.length === 1 ? 'friend' : 'friends'}` : country.name}
          </span>
          <button className="btn" disabled={busy}><StampIcon strokeWidth={1.75} />{busy ? 'Stamping…' : 'Stamp it'}</button>
        </div>
      </div>
    </form>
  )
}
