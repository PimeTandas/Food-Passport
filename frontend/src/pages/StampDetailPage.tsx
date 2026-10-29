import { ArrowLeft, Banknote, MapPin, PenLine, Trash2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { Stamp } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EntryFields, toEntryRequest, type EntryValues } from '../components/EntryFields'
import { FriendPicker } from '../components/FriendPicker'
import { InkStamp } from '../components/InkStamp'
import { StarRating } from '../components/StarRating'
import { ErrorMessage, Loading } from '../components/Status'
import { formatPrice, passportDate } from '../format'
import { useAsync } from '../useAsync'

type Editing = 'none' | 'review' | 'entry' | 'companions'

export function StampDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const { data: stamp, error, loading, reload, setData } = useAsync(() => api.stamp(id), [id])
  const [editing, setEditing] = useState<Editing>('none')

  if (loading && !stamp) return <Loading />
  if (error) return <ErrorMessage error={error} onRetry={reload} />
  if (!stamp) return null

  const entry = stamp.entry
  const isCreator = entry.createdBy?.id === user?.id
  const saved = (updated: Stamp) => { setData(updated); setEditing('none') }
  const editButton = (section: Editing) =>
    editing !== section && <button className="text-btn" onClick={() => setEditing(section)}>Edit</button>

  async function remove() {
    const message = entry.with.length > 0
      ? 'Remove this stamp from your passport? The people you were with keep theirs.'
      : 'Remove this stamp? This deletes the meal too.'
    if (!window.confirm(message)) return
    await api.deleteStamp(stamp!.id)
    navigate(`/countries/${stamp!.country.code}`, { replace: true })
  }

  return (
    <main className="page">
      <Link to={`/countries/${stamp.country.code}`} className="back"><ArrowLeft strokeWidth={1.75} />{stamp.country.name}</Link>

      <header className="detail-head">
        <div>
          <div className="country-meta">{stamp.country.flag} {stamp.country.code} · {passportDate(stamp.stampedAt)}</div>
          <h1>{entry.dishName}</h1>
        </div>
        <InkStamp stamp={stamp} />
      </header>

      <section className="detail-section">
        <div className="spread"><h2>Your verdict</h2>{editButton('review')}</div>
        {editing === 'review'
          ? <ReviewForm stamp={stamp} onSaved={saved} onCancel={() => setEditing('none')} />
          : (
            <>
              {stamp.rating ? <StarRating value={stamp.rating} /> : <p className="faint">Not rated yet.</p>}
              {stamp.notes && <p className="notes">“{stamp.notes}”</p>}
            </>
          )}
      </section>

      <section className="detail-section">
        <div className="spread"><h2>The meal</h2>{editButton('entry')}</div>
        {editing === 'entry'
          ? <EntryForm stamp={stamp} onSaved={saved} onCancel={() => setEditing('none')} />
          : (
            <dl className="facts">
              <div className="fact">
                <MapPin strokeWidth={1.5} aria-hidden />
                <div><dt>{entry.homemade ? 'Homemade' : 'Eaten out'}</dt><dd>{entry.placeName ?? 'No place noted'}</dd></div>
              </div>
              <div className="fact">
                <Banknote strokeWidth={1.5} aria-hidden />
                <div><dt>Price</dt><dd>{entry.price ? formatPrice(entry.price) : 'Not noted'}</dd></div>
              </div>
              <div className="fact">
                <PenLine strokeWidth={1.5} aria-hidden />
                <div><dt>Logged by</dt><dd>{isCreator ? 'You' : `@${entry.createdBy?.username ?? 'someone'}`}</dd></div>
              </div>
            </dl>
          )}
        {editing !== 'entry' && entry.with.length > 0 && (
          <p className="faint small">Shared with everyone who was there, so edits show up for them too.</p>
        )}
      </section>

      <section className="detail-section">
        <div className="spread"><h2>Who was there</h2>{isCreator && editButton('companions')}</div>
        {editing === 'companions'
          ? <CompanionsForm stamp={stamp} onSaved={saved} onCancel={() => setEditing('none')} />
          : entry.with.length === 0
            ? <p className="faint">Just you.</p>
            : (
              <ul className="people">
                {entry.with.map((u) => (
                  <li key={u.id} className="person-row">
                    <span className="avatar" aria-hidden>{u.username[0]?.toUpperCase()}</span>@{u.username}
                  </li>
                ))}
              </ul>
            )}
      </section>

      <section className="detail-section">
        <button className="text-btn danger" onClick={remove}><Trash2 strokeWidth={1.75} />Remove stamp</button>
      </section>
    </main>
  )
}

// ---------- the three edit forms ----------

interface FormProps {
  stamp: Stamp
  onSaved: (stamp: Stamp) => void
  onCancel: () => void
}

/** Shared save/cancel/error plumbing for the small forms below. */
function useSave(onSaved: (s: Stamp) => void) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)
  const run = async (action: () => Promise<Stamp>) => {
    setBusy(true)
    setError(null)
    try {
      onSaved(await action())
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Could not reach the server'))
      setBusy(false)
    }
  }
  return { busy, error, run }
}

function FormButtons({ busy, onCancel }: { busy: boolean; onCancel: () => void }) {
  return (
    <div className="row" style={{ gap: 16 }}>
      <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
      <button type="button" className="text-btn" onClick={onCancel}>Cancel</button>
    </div>
  )
}

function ReviewForm({ stamp, onSaved, onCancel }: FormProps) {
  const [rating, setRating] = useState(stamp.rating)
  const [notes, setNotes] = useState(stamp.notes ?? '')
  const { busy, error, run } = useSave(onSaved)
  const submit = (e: FormEvent) => { e.preventDefault(); run(() => api.review(stamp.id, rating, notes)) }
  return (
    <form className="stack" onSubmit={submit}>
      {error && <div className="alert">{error.message}</div>}
      <StarRating value={rating} onChange={setRating} />
      <textarea className="input" maxLength={2000} value={notes} aria-label="Notes" onChange={(e) => setNotes(e.target.value)} />
      <FormButtons busy={busy} onCancel={onCancel} />
    </form>
  )
}

function EntryForm({ stamp, onSaved, onCancel }: FormProps) {
  const e = stamp.entry
  const [values, setValues] = useState<EntryValues>({
    dishName: e.dishName,
    placeName: e.placeName ?? '',
    homemade: e.homemade,
    priceAmount: e.price ? String(e.price.amount) : '',
    priceCurrency: e.price?.currency ?? '',
  })
  const { busy, error, run } = useSave(onSaved)
  const submit = (ev: FormEvent) => { ev.preventDefault(); run(() => api.updateEntry(stamp.id, toEntryRequest(values))) }
  const errors = error?.fieldErrors ?? {}
  return (
    <form className="stack" style={{ gap: 16 }} onSubmit={submit} noValidate>
      {error && Object.keys(errors).length === 0 && <div className="alert">{error.message}</div>}
      <EntryFields values={values} onChange={setValues} errors={errors} />
      <FormButtons busy={busy} onCancel={onCancel} />
    </form>
  )
}

function CompanionsForm({ stamp, onSaved, onCancel }: FormProps) {
  const { data: friends, error: loadError } = useAsync(() => api.friends(), [])
  const [selected, setSelected] = useState(stamp.entry.with.map((u) => u.id))
  const { busy, error, run } = useSave(onSaved)
  const submit = (e: FormEvent) => { e.preventDefault(); run(() => api.setCompanions(stamp.id, selected)) }

  if (loadError) return <div className="alert">{loadError.message}</div>
  if (!friends) return <p className="muted">Loading friends…</p>
  // People already tagged stay pickable even if you've since unfriended them.
  const options = [...friends, ...stamp.entry.with.filter((u) => !friends.some((f) => f.id === u.id))]
  return (
    <form className="stack" onSubmit={submit}>
      {error && <div className="alert">{error.message}</div>}
      <FriendPicker friends={options} selected={selected} onChange={setSelected} />
      <p className="faint small">Unticking someone removes the stamp from their passport.</p>
      <FormButtons busy={busy} onCancel={onCancel} />
    </form>
  )
}
