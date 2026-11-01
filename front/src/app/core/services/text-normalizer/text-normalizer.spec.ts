import { matchesTolerant, normalizeText } from './text-normalizer'

describe('TextNormalizer', () => {
  it('should handle null, undefined and empty strings', () => {
    expect(normalizeText(null)).toBe('')
    expect(normalizeText(undefined)).toBe('')
    expect(normalizeText('')).toBe('')
    expect(normalizeText('   ')).toBe('')
    expect(matchesTolerant(null, 'tulipe')).toBe(false)
    expect(matchesTolerant('tulipe', undefined)).toBe(false)
  })

  it.each([
    'tulipe',
    'Tulipe',
    'TULIPE',
    'tuliipe',
    'tulllipe',
    'tuuliippe',
    'túlipé',
    'tulipe 🌷',
    '  tulipe ! ',
    'tulipe...',
    't-u-l-i-p-e',
  ])('should match variant "%s" with expected "tulipe"', (candidate) => {
    expect(matchesTolerant(candidate, 'tulipe')).toBe(true)
  })

  it('should not match different words', () => {
    expect(matchesTolerant('rose', 'tulipe')).toBe(false)
    expect(matchesTolerant('tournesol', 'tulipe')).toBe(false)
    expect(matchesTolerant('tuile', 'tulipe')).toBe(false)
  })
})
