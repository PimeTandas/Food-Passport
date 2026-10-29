import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import { EntryFields, toEntryRequest, type EntryValues } from '../components/EntryFields'
import { FriendPicker } from '../components/FriendPicker'
import { StarRating } from '../components/StarRating'
import { ErrorMessage, Loading } from '../components/Status'
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
      return
    }
    setBusy(true)
    setSubmitError(null)
    try {
      const request = toEntryRequest(entry)
      const stamp = await api.createStamp(country.code, {
        ...request,
        rating: rating ?? undefined,
        notes: notes || undefined,
        withFriendIds: friendIds,
      })
      if (request.price) {
        try { localStorage.setItem(LAST_CURRENCY, request.price.currency) } catch { /* not important */ }
      }
      // Back to the page, where the new stamp "lands" with an animation.
      navigate(`/countries/${country.code}`, { state: { justStamped: stamp.id } })
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err : new ApiError(0, 'Could not reach the server'))
      setBusy(false)
    }
  }

  const errors = submitError?.fieldErrors ?? {}
  return (
    <form className="stack" style={{ gap: 18 }} onSubmit={submit} noValidate>
      <Link to={`/countries/${country.code}`} className="muted small">← {country.name}</Link>
      <h1>{country.flag} Stamp {country.name}</h1>

      {submitError && Object.keys(errors).length === 0 && <div className="alert">{submitError.message}</div>}

      <div className="card stack">
        <EntryFields values={entry} onChange={setEntry} errors={errors} />
      </div>

      <div className="card stack">
        <div className="field">
          <span>Your rating</span>
          <StarRating value={rating} onChange={setRating} />
        </div>
        <label className="field">
          <span>Your notes</span>
          <textarea className="input" maxLength={2000} placeholder="How was it? Would you have it again?"
                    value={notes} onChange={(e) => setNotes(e.target.value)} />
        </label>
        <p className="muted small" style={{ margin: 0 }}>Only you see your rating and notes.</p>
      </div>

      <div className="card stack">
        <div className="field">
          <span>Who were you with?</span>
          <FriendPicker friends={friends} selected={friendIds} onChange={setFriendIds} />
        </div>
        {friendIds.length > 0 && (
          <p className="muted small" style={{ margin: 0 }}>They'll get this stamp in their passport too.</p>
        )}
      </div>

      <button className="btn stamp-it block" disabled={busy}>{busy ? 'Stamping…' : '🖋️ Stamp it'}</button>
    </form>
  )
}
