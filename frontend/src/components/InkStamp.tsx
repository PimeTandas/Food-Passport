import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import type { Stamp } from '../api/types'
import { passportDate, stampLook } from '../format'

/** A stamp as it appears on a country page: country code, date, dish - in textured ink. */
export function InkStamp({ stamp, landing = false }: { stamp: Stamp; landing?: boolean }) {
  const { ink, tilt, oval } = stampLook(stamp.id)
  const style = { '--ink-color': ink, '--tilt': tilt } as CSSProperties
  const withCount = stamp.entry.with.length
  const classes = ['ink-stamp', oval && 'oval', landing && 'landing'].filter(Boolean).join(' ')
  return (
    <Link to={`/stamps/${stamp.id}`} className={classes} style={style}
          aria-label={`${stamp.entry.dishName}, ${passportDate(stamp.stampedAt)}`}>
      <span className="code">{stamp.country.code}</span>
      <span className="date">{passportDate(stamp.stampedAt)}</span>
      <span className="dish">{stamp.entry.dishName}</span>
      <span className="meta">
        {stamp.rating ? '★'.repeat(stamp.rating) : stamp.entry.homemade ? 'HOME' : '—'}
        {withCount > 0 && ` +${withCount}`}
      </span>
    </Link>
  )
}
