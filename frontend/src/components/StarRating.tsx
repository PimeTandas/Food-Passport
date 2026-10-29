interface Props {
  value: number | null
  onChange?: (value: number | null) => void
}

/** Clickable 1-5 stars. Tap the current rating again to clear it. Read-only without onChange. */
export function StarRating({ value, onChange }: Props) {
  if (!onChange) {
    if (!value) return null
    return <span className="stars readonly" aria-label={`${value} out of 5`}>{'★'.repeat(value)}{'☆'.repeat(5 - value)}</span>
  }
  return (
    <div className="stars" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} star${n > 1 ? 's' : ''}`}
          className={value !== null && n <= value ? 'on' : ''}
          onClick={() => onChange(value === n ? null : n)}
        >★</button>
      ))}
    </div>
  )
}
