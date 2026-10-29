export interface EntryValues {
  dishName: string
  placeName: string
  homemade: boolean
  priceAmount: string
  priceCurrency: string
}

interface Props {
  values: EntryValues
  onChange: (values: EntryValues) => void
  errors: Record<string, string>
}

const COMMON_CURRENCIES = ['EUR', 'GBP', 'USD', 'JPY', 'CNY', 'KRW', 'THB', 'INR', 'MXN', 'AUD', 'CAD', 'CHF', 'TRY', 'VND']

/** The shared details of a food entry: used when stamping and when editing. */
export function EntryFields({ values, onChange, errors }: Props) {
  const set = <K extends keyof EntryValues>(key: K, value: EntryValues[K]) => onChange({ ...values, [key]: value })
  return (
    <>
      <label className="field">
        <span>What did you eat?</span>
        <input className="input" placeholder="e.g. Tonkotsu ramen" value={values.dishName} maxLength={200}
               onChange={(e) => set('dishName', e.target.value)} />
        {errors.dishName && <span className="field-error">{errors.dishName}</span>}
      </label>

      <div className="field">
        <span>Where?</span>
        <div className="toggle" role="group" aria-label="Homemade or eaten out">
          <button type="button" aria-pressed={!values.homemade} onClick={() => set('homemade', false)}>🍽️ Eaten out</button>
          <button type="button" aria-pressed={values.homemade} onClick={() => set('homemade', true)}>🏠 Homemade</button>
        </div>
        <input className="input" value={values.placeName} maxLength={200}
               placeholder={values.homemade ? "e.g. Mum's kitchen" : 'e.g. Ichiran, Tokyo'}
               aria-label="Place" onChange={(e) => set('placeName', e.target.value)} />
      </div>

      <div className="field">
        <span>Price <span className="muted small">(optional)</span></span>
        <div className="row" style={{ flexWrap: 'nowrap' }}>
          <input className="input" type="number" inputMode="decimal" min="0" step="0.01" placeholder="0.00"
                 aria-label="Amount" value={values.priceAmount} onChange={(e) => set('priceAmount', e.target.value)} />
          <input className="input" style={{ maxWidth: 100, textTransform: 'uppercase' }} list="currencies"
                 maxLength={3} aria-label="Currency" placeholder="EUR" value={values.priceCurrency}
                 onChange={(e) => set('priceCurrency', e.target.value.toUpperCase())} />
          <datalist id="currencies">
            {COMMON_CURRENCIES.map((c) => <option key={c} value={c} />)}
          </datalist>
        </div>
        {(errors['price.amount'] || errors['price.currency'] || errors.price) && (
          <span className="field-error">{errors['price.amount'] ?? errors['price.currency'] ?? errors.price}</span>
        )}
      </div>
    </>
  )
}

/** Turn the form's strings into the API's shape. Price is only sent if an amount was typed. */
export function toEntryRequest(v: EntryValues) {
  const amount = v.priceAmount.trim()
  return {
    dishName: v.dishName,
    placeName: v.placeName || undefined,
    homemade: v.homemade,
    price: amount ? { amount: Number(amount), currency: v.priceCurrency || 'EUR' } : undefined,
  }
}
