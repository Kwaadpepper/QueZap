import { computed, inject } from '@angular/core'
import { toObservable } from '@angular/core/rxjs-interop'

import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals'
import { rxMethod } from '@ngrx/signals/rxjs-interop'
import { catchError, debounceTime, distinctUntilChanged, EMPTY, filter, pipe, retry, switchMap, tap } from 'rxjs'

import { pageComparator, Pagination, validatePagination } from '@quezap/core/types'
import { Theme } from '@quezap/domain/models'

import { THEME_SERVICE } from '../services'

interface ThemePageState {
  query: {
    page: number
    pageSize: number
  }
  pageData: Theme[]
  _pageMetaData: {
    totalElements: number
    totalPages: number
    page: number
    pageSize: number
  }
  _isLoading: boolean
  _doReload: boolean
  _isRollingBack: boolean
}

const initialState: ThemePageState = {
  query: { page: 1, pageSize: 25 },
  pageData: [],
  _pageMetaData: {
    totalElements: 0,
    totalPages: 0,
    page: 1,
    pageSize: 25,
  },
  _isLoading: false,
  _doReload: false,
  _isRollingBack: false,
}

export const ThemePageStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(store => ({
    pageInfo: computed(() => {
      const page = store.query().page
      const pageSize = store.query().pageSize
      const totalPages = store._pageMetaData().totalPages
      const totalElements = store._pageMetaData().totalElements

      return {
        page,
        pageSize,
        isFirstPage: page === 1,
        isLastPage: page >= totalPages,
        totalElements,
        totalPages,
      }
    }),
    isLoading: computed(() => store._isLoading() && !store._isRollingBack()),
  })),

  withMethods((store, themeService = inject(THEME_SERVICE)) => {
    const updatePagination = (query: { page: number, pageSize: number }) => {
      if (!validatePagination(query)) {
        return
      }
      patchState(store, {
        query: { ...query },
        _doReload: false,
      })
    }

    const loadThemePage = rxMethod<Pagination>(
      pipe(
        filter(validatePagination),
        distinctUntilChanged((a, b) => {
          // Reloading inhibits distinctness
          return (pageComparator(a, b) && !store._doReload())
        }),
        // Ignore rolling back after distinctness to consume changes
        filter(() => !store._isRollingBack()),
        // Debounce only valids changes
        debounceTime(300),
        tap(() => patchState(store, { _isLoading: true })),
        switchMap((pagination) => {
          return themeService.getThemePage(pagination).pipe(
            // Retry twice on failure
            retry(2),
            tap({
              next: (response) => {
                patchState(store, {
                  pageData: response.data,
                  _pageMetaData: {
                    totalElements: response.totalElements,
                    totalPages: response.totalPages,
                    page: response.page,
                    pageSize: response.pageSize,
                  },
                  _isLoading: false,
                  _doReload: false,
                  _isRollingBack: false,
                })
              },
            }),
            // Gracefully handle errors
            catchError((error) => {
              console.error('Error loading themes:', error)

              const lastSuccessfulPage = {
                page: store._pageMetaData().page,
                pageSize: store._pageMetaData().pageSize,
              }

              // Mark as rolling back
              patchState(store, { _isRollingBack: true, _isLoading: false, _doReload: false })

              // Next tick to make sure isRollingBack change is detected
              setTimeout(() => {
                patchState(store, {
                  query: { ...lastSuccessfulPage },
                  _pageMetaData: {
                    ...store._pageMetaData(),
                    page: lastSuccessfulPage.page,
                    pageSize: lastSuccessfulPage.pageSize,
                  },
                })

                // Next tick to reset isRollingBack
                setTimeout(() => { // NOSONAR
                  patchState(store, { _isRollingBack: false })
                })
              }, 0)

              return EMPTY
            }),
          )
        },
        ),
      ),
    )

    // Connect query changes to loader
    loadThemePage(toObservable(store.query))

    // Public methods
    return {

      /** Reload current page */
      reload: () => {
        patchState(store, {
          query: { ...store.query() },
          _doReload: true,
        })
      },

      /** Set pagination and reload */
      setPagination: (pagination: Partial<{ page: number, pageSize: number }>) => {
        updatePagination({ ...store.query(), ...pagination })
      },
    }
  }),
)
