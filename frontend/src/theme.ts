import { useEffect, useState } from 'react'

export type ThemePreference = 'system' | 'light' | 'dark'

const KEY = 'foodpassport.theme'

export function readThemePreference(): ThemePreference {
  try {
    const saved = localStorage.getItem(KEY)
    return saved === 'light' || saved === 'dark' ? saved : 'system'
  } catch {
    return 'system'
  }
}

/**
 * The CSS does the real work: light colours by default, dark colours when
 * <html data-theme="dark">, or when the phone is in dark mode and data-theme isn't "light".
 * So "system" just means: no data-theme attribute at all.
 */
export function applyTheme(pref: ThemePreference) {
  const root = document.documentElement
  if (pref === 'system') delete root.dataset.theme
  else root.dataset.theme = pref
}

export function useTheme() {
  const [pref, setPref] = useState<ThemePreference>(readThemePreference)
  useEffect(() => {
    applyTheme(pref)
    try {
      if (pref === 'system') localStorage.removeItem(KEY)
      else localStorage.setItem(KEY, pref)
    } catch {
      // storage blocked: the choice just won't be remembered
    }
  }, [pref])
  return [pref, setPref] as const
}
