export type Pagination = {
  page: number
  pageSize: number
} | {
  from: number
  to: number
} | {
  offset: number
  limit: number
}

export interface PageOf<T> {
  data: T[]
  /** Nombre total d'élements existants */
  totalElements: number
  /** Nombre total de pages existantes */
  totalPages: number
  /** Numéro de la page courante (1-indexed) */
  page: number
  /** Taille de la page courante */
  pageSize: number
  /** Indique s'il existe une page suivante */
  hasNext: boolean
  /** Indique s'il existe une page précédente */
  hasPrevious: boolean
}

export function pageComparator(a: Pagination, b: Pagination): boolean {
  const pageA = toPageBasedPagination(a)
  const pageB = toPageBasedPagination(b)

  return pageA.page === pageB.page && pageA.pageSize === pageB.pageSize
}

export function validatePagination(pagination: Pagination): boolean {
  if (isPageBasedPagination(pagination)) {
    return pagination.page >= 1 && pagination.pageSize > 0
  }
  else if (isOffsetBasedPagination(pagination)) {
    return pagination.offset >= 0 && pagination.limit > 0
  }
  else if (isFromToBasedPagination(pagination)) {
    return pagination.from >= 0 && pagination.to >= pagination.from
  }
  return false
}

export function isPageBasedPagination(pagination: Pagination): pagination is { page: number, pageSize: number } {
  return (pagination as { page: number, pageSize: number }).page !== undefined
}

export function isOffsetBasedPagination(pagination: Pagination): pagination is { offset: number, limit: number } {
  return (pagination as { offset: number, limit: number }).offset !== undefined
}

export function isFromToBasedPagination(pagination: Pagination): pagination is { from: number, to: number } {
  return (pagination as { from: number, to: number }).from !== undefined
}

export function toPageBasedPagination(pagination: Pagination): { page: number, pageSize: number } {
  if (isPageBasedPagination(pagination)) {
    return {
      page: Math.max(pagination.page, 1),
      pageSize: Math.max(pagination.pageSize, 1),
    }
  }
  else if (isOffsetBasedPagination(pagination)) {
    const pageSize = Math.max(pagination.limit, 1)
    const page = Math.floor(pagination.offset / pageSize) + 1

    return { page, pageSize }
  }

  else if (isFromToBasedPagination(pagination)) {
    const pageSize = Math.max(pagination.to - pagination.from + 1, 1)

    const offset = pagination.from
    const page = Math.floor(offset / pageSize) + 1

    return { page, pageSize }
  }
  else {
    console.warn('Type de pagination inconnu. Retourne la première page par défaut.')
    return { page: 1, pageSize: 10 }
  }
}
