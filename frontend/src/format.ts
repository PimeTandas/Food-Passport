import type { Price } from './api/types'

const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']

/** Passport style: "29 OCT 2026". */
export function passportDate(iso: string): string {
  const d = new Date(iso)
  return `${String(d.getDate()).padStart(2, '0')} ${MONTHS[d.getMonth()]} ${d.getFullYear()}`
}

export function formatPrice(price: Price): string {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: price.currency }).format(price.amount)
  } catch {
    return `${price.amount} ${price.currency}`
  }
}

/** e.g. user 12 → "FP0000012". */
export function passportNumber(userId: number): string {
  return 'FP' + String(userId).padStart(7, '0')
}

/** "P. 112" - countries are pages in alphabetical order, like the backend sends them. */
export function pageLabel(page: number): string {
  return `P. ${String(page).padStart(3, '0')}`
}

/**
 * The ICAO 9303 check digit used on real passports: weight each character 7, 3, 1, 7, 3, 1...
 * (digits are themselves, A=10 ... Z=35, '<'=0), sum, take the last digit.
 */
export function mrzCheckDigit(s: string): number {
  const weights = [7, 3, 1]
  let sum = 0
  for (let i = 0; i < s.length; i++) {
    const c = s[i]
    const value = c >= '0' && c <= '9' ? Number(c) : c >= 'A' && c <= 'Z' ? c.charCodeAt(0) - 55 : 0
    sum += value * weights[i % 3]
  }
  return sum % 10
}

/** The two machine-readable lines at the bottom of the data page (44 characters each). */
export function mrzLines(username: string, userId: number, issued: string | null, stamps: number): [string, string] {
  const name = username.toUpperCase().replace(/[^A-Z0-9]/g, '<')
  const line1 = `P<FDP${name}<<FOOD<PASSPORT`.padEnd(44, '<').slice(0, 44)

  const number = passportNumber(userId)
  const d = issued ? new Date(issued) : new Date()
  const yymmdd = String(d.getFullYear()).slice(2) + String(d.getMonth() + 1).padStart(2, '0') + String(d.getDate()).padStart(2, '0')
  const count = String(stamps).padStart(4, '0')
  const line2 = `${number}${mrzCheckDigit(number)}FDP${yymmdd}${mrzCheckDigit(yymmdd)}<${count}${mrzCheckDigit(count)}`
    .padEnd(43, '<')
  return [line1, line2 + mrzCheckDigit(line2)]
}

const INKS = ['--ink-red', '--ink-blue', '--ink-green', '--ink-violet']

/** Each stamp gets a stable ink, tilt and shape from its id, so it looks the same every visit. */
export function stampLook(id: number): { ink: string; tilt: string; oval: boolean } {
  const tilt = ((id * 37) % 11) - 5 // -5..5 degrees
  return { ink: `var(${INKS[id % INKS.length]})`, tilt: `${tilt}deg`, oval: id % 3 === 1 }
}
