import { Check } from 'lucide-react'
import type { UserSummary } from '../api/types'

interface Props {
  friends: UserSummary[]
  selected: number[]
  onChange: (ids: number[]) => void
}

export function FriendPicker({ friends, selected, onChange }: Props) {
  if (friends.length === 0) {
    return <p className="muted">No friends yet. Once you add friends you can tag them here.</p>
  }
  const toggle = (id: number) =>
    onChange(selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id])
  return (
    <ul className="pick-list">
      {friends.map((f) => (
        <li key={f.id}>
          <label className="pick-row">
            <input type="checkbox" checked={selected.includes(f.id)} onChange={() => toggle(f.id)} />
            <span className="avatar" aria-hidden>{f.username[0]?.toUpperCase()}</span>
            <span className="name">@{f.username}</span>
            <span className="pick-check" aria-hidden><Check strokeWidth={2.5} /></span>
          </label>
        </li>
      ))}
    </ul>
  )
}
