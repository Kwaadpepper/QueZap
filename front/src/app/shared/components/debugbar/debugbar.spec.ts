import { ComponentFixture, TestBed } from '@angular/core/testing'

import { Debugbar } from './debugbar'

describe('Debugbar', () => {
  let component: Debugbar
  let fixture: ComponentFixture<Debugbar>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Debugbar] })
      .compileComponents()

    fixture = TestBed.createComponent(Debugbar)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
