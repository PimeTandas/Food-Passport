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
import { formatDate, formatPrice } from '../format'
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

  async function remove() {
    const others = entry.with.length
    const message = others > 0
      ? 'Remove this stamp from your passport? The people you were with keep theirs.'
      : 'Remove this stamp? This deletes the food entry too.'
    if (!window.confirm(message)) return
    await api.deleteStamp(stamp!.id)
    navigate(`/countries/${stamp!.country.code}`, { replace: true })
  }

  return (
    <div className="stack" style={{ gap: 18 }}>
      <Link to={`/countries/${stamp.country.code}`} className="muted small">← {stamp.country.flag} {stamp.country.name}</Link>

      <div className="row" style={{ gap: 16, flexWrap: 'nowrap', alignItems: 'center' }}>
        <div style={{ width: 130, flex: '0 0 auto' }}><InkStamp stamp={stamp} /></div>
        <div>
          <h1>{entry.dishName}</h1>
          <span className="muted">Stamped {formatDate(stamp.stampedAt)}</span>
        </div>
      </div>

      <div className="detail-grid">
        {/* ----- personal ----- */}
        <section className="card stack">
          <div className="spread">
            <h2>Your take</h2>
            {editing !== 'review' && <button className="btn ghost small" onClick={() => setEditing('review')}>Edit</button>}
          </div>
          {editing === 'review'
            ? <ReviewForm stamp={stamp} onSaved={saved} onCancel={() => setEditing('none')} />
            : (
              <>
                {stamp.rating ? <StarRating value={stamp.rating} /> : <span className="muted">Not rated yet</span>}
                {stamp.notes ? <p style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{stamp.notes}</p> : <span className="muted small">No notes.</span>}
              </>
            )}
        </section>

        {/* ----- shared ----- */}
        <section className="card stack">
          <div className="spread">
            <h2>The meal</h2>
            {editing !== 'entry' && <button className="btn ghost small" onClick={() => setEditing('entry')}>Edit</button>}
          </div>
          {editing === 'entry'
            ? <EntryForm stamp={stamp} onSaved={saved} onCancel={() => setEditing('none')} />
            : (
              <dl className="kv" style={{ margin: 0 }}>
                <dt>Where</dt>
                <dd>{entry.homemade ? '🏠 Homemade' : '🍽️ Eaten out'}{entry.placeName && ` · ${entry.placeName}`}</dd>
                <dt>Price</dt>
                <dd>{entry.price ? formatPrice(entry.price) : '—'}</dd>
                <dt>Logged by</dt>
                <dd>{isCreator ? 'You' : `@${entry.createdBy?.username ?? 'someone'}`}</dd>
              </dl>
            )}
          {editing !== 'entry' && entry.with.length > 0 && (
            <p className="muted small" style={{ margin: 0 }}>Shared with everyone who was there: edits show up for them too.</p>
          )}
        </section>

        <section className="card stack">
          <div className="spread">
            <h2>Who was there</h2>
            {isCreator && editing !== 'companions' &&
              <button className="btn ghost small" onClick={() => setEditing('companions')}>Edit</button>}
          </div>
          {editing === 'companions'
            ? <CompanionsForm stamp={stamp} onSaved={saved} onCancel={() => setEditing('none')} />
            : entry.with.length === 0
              ? <span className="muted">Just you.</span>
              : (
                <div className="row">
                  {entry.with.map((u) => (
                    <span key={u.id} className="person">
                      <span className="avatar">{u.username[0]?.toUpperCase()}</span>@{u.username}
                    </span>
                  ))}
                </div>
              )}
        </section>
      </div>

      <button className="btn danger" onClick={remove}>Remove stamp</button>
    </div>
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
    <div className="row">
      <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
      <button type="button" className="btn ghost" onClick={onCancel}>Cancel</button>
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
    <form className="stack" onSubmit={submit} noValidate>
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

  if (loadError) return <ErrorMessage error={loadError} />
  if (!friends) return <Loading />
  // People already tagged stay pickable even if you've since unfriended them.
  const options = [...friends, ...stamp.entry.with.filter((u) => !friends.some((f) => f.id === u.id))]
  return (
    <form className="stack" onSubmit={submit}>
      {error && <div className="alert">{error.message}</div>}
      <FriendPicker friends={options} selected={selected} onChange={setSelected} />
      <p className="muted small" style={{ margin: 0 }}>Unticking someone removes the stamp from their passport.</p>
      <FormButtons busy={busy} onCancel={onCancel} />
    </form>
  )
}
