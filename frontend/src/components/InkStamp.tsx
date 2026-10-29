import { Link } from 'react-router-dom'
import type { Stamp } from '../api/types'
import { formatDate, stampLook } from '../format'
import type { CSSProperties } from 'react'

/** A stamp as it appears on a country page: country code, date, dish - in "ink". */
export function InkStamp({ stamp, landing = false }: { stamp: Stamp; landing?: boolean }) {
  const { ink, tilt } = stampLook(stamp.id)
  const style = { '--ink': ink, '--tilt': tilt } as CSSProperties
  const withCount = stamp.entry.with.length
  return (
    <Link to={`/stamps/${stamp.id}`} className={`ink-stamp${landing ? ' landing' : ''}`} style={style}>
      <div className="code">{stamp.country.code}</div>
      <div className="date">{formatDate(stamp.stampedAt)}</div>
      <div className="dish">{stamp.entry.dishName}</div>
      <div className="meta">
        {stamp.rating ? '★'.repeat(stamp.rating) : stamp.entry.homemade ? 'homemade' : 'not rated'}
        {withCount > 0 && ` · +${withCount}`}
      </div>
    </Link>
  )
}
