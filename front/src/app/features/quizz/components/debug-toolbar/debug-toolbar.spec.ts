import { ComponentFixture, TestBed } from '@angular/core/testing'

import { DebugToolbar } from './debug-toolbar'

describe('DebugToolbar', () => {
  let component: DebugToolbar
  let fixture: ComponentFixture<DebugToolbar>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DebugToolbar],
    })
      .compileComponents()

    fixture = TestBed.createComponent(DebugToolbar)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
