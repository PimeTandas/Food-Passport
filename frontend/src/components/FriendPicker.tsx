import type { UserSummary } from '../api/types'

interface Props {
  friends: UserSummary[]
  selected: number[]
  onChange: (ids: number[]) => void
}

export function FriendPicker({ friends, selected, onChange }: Props) {
  if (friends.length === 0) {
    return <p className="muted small" style={{ margin: 0 }}>No friends yet. Once you add friends you can tag them here.</p>
  }
  const toggle = (id: number) =>
    onChange(selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id])
  return (
    <div className="friend-pick">
      {friends.map((f) => (
        <label key={f.id}>
          <input type="checkbox" checked={selected.includes(f.id)} onChange={() => toggle(f.id)} />
          @{f.username}
        </label>
      ))}
    </div>
  )
}
