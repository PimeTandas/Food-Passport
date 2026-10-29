import { Star } from 'lucide-react'

interface Props {
  value: number | null
  onChange?: (value: number | null) => void
}

/** 1-5 stars. Tap the current rating again to clear it. Read-only without onChange. */
export function StarRating({ value, onChange }: Props) {
  if (!onChange) {
    if (!value) return null
    return (
      <span className="stars readonly" role="img" aria-label={`${value} out of 5`}>
        {[1, 2, 3, 4, 5].map((n) => <Star key={n} className={n <= value ? 'on' : ''} strokeWidth={1.5} />)}
      </span>
    )
  }
  return (
    <div className="stars" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button key={n} type="button" role="radio" aria-checked={value === n}
                aria-label={`${n} star${n > 1 ? 's' : ''}`}
                className={value !== null && n <= value ? 'on' : ''}
                onClick={() => onChange(value === n ? null : n)}>
          <Star strokeWidth={1.5} />
        </button>
      ))}
    </div>
  )
}
