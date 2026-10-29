import type { ReactNode } from 'react'
import { useTheme, type ThemePreference } from '../theme'

const NEXT: Record<ThemePreference, ThemePreference> = { system: 'light', light: 'dark', dark: 'system' }
const LABEL: Record<ThemePreference, string> = {
  system: 'Theme: follows your device (tap for light)',
  light: 'Theme: light (tap for dark)',
  dark: 'Theme: dark (tap to follow your device)',
}

// Small inline SVG icons: crisp at any size and they take the text colour (currentColor).
const ICON: Record<ThemePreference, ReactNode> = {
  system: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor" />
    </svg>
  ),
  light: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  ),
  dark: (
    <svg viewBox="0 0 24 24" fill="currentColor">
      <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z" />
    </svg>
  ),
}

/** One tap cycles: follow device → light → dark → follow device. */
export function ThemeToggle() {
  const [pref, setPref] = useTheme()
  return (
    <button className="icon-btn" onClick={() => setPref(NEXT[pref])} aria-label={LABEL[pref]} title={LABEL[pref]}>
      {ICON[pref]}
    </button>
  )
}
