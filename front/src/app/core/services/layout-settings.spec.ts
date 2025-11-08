import { TestBed } from '@angular/core/testing'

import { LayoutSettings } from './layout-settings'

describe('LayoutSettings', () => {
  let service: LayoutSettings

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(LayoutSettings)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})
