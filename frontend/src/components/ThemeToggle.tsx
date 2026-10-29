import { Monitor, Moon, Sun } from 'lucide-react'
import { useTheme, type ThemePreference } from '../theme'

const NEXT: Record<ThemePreference, ThemePreference> = { system: 'light', light: 'dark', dark: 'system' }
const LABEL: Record<ThemePreference, string> = {
  system: 'Theme: follows your device (tap for light)',
  light: 'Theme: light (tap for dark)',
  dark: 'Theme: dark (tap to follow your device)',
}
const ICON = { system: Monitor, light: Sun, dark: Moon }

/** One tap cycles: follow device → light → dark → follow device. */
export function ThemeToggle() {
  const [pref, setPref] = useTheme()
  const Icon = ICON[pref]
  return (
    <button className="icon-btn" onClick={() => setPref(NEXT[pref])} aria-label={LABEL[pref]} title={LABEL[pref]}>
      <Icon strokeWidth={1.75} />
    </button>
  )
}
