/**
 * Normalise un texte selon les règles de tolérance canonique :
 * 1. Trim et passage en minuscules.
 * 2. Décomposition Unicode NFD et suppression des signes diacritiques (accents, cédilles, trémas).
 * 3. Suppression des emojis, ponctuations et caractères non-alphanumériques.
 * 4. Réduction des répétitions consécutives de caractères (ex: "tuliipe" -> "tulipe", "tullipe" -> "tulipe").
 */
export function normalizeText(input: string | null | undefined): string {
  if (!input) {
    return ''
  }
  const text = input.trim().toLowerCase()
  if (!text) {
    return ''
  }

  // 1. Décomposition Unicode NFD et suppression des signes diacritiques
  const withoutDiacritics = text.normalize('NFD').replace(/[\u0300-\u036f]/g, '')

  // 2. Suppression des emojis, ponctuations et caractères non-alphanumériques
  const alphanumericOnly = withoutDiacritics.replace(/[^a-z0-9]+/g, '')

  // 3. Réduction des répétitions consécutives de lettres/chiffres (ex: "tuliipe" -> "tulipe")
  return alphanumericOnly.replace(/([a-z0-9])\1+/g, '$1')
}

/**
 * Compare deux réponses de façon tolérante en appliquant la normalisation symétrique.
 */
export function matchesTolerant(candidate: string | null | undefined, expected: string | null | undefined): boolean {
  const normCandidate = normalizeText(candidate)
  const normExpected = normalizeText(expected)
  return normCandidate !== '' && normCandidate === normExpected
}
