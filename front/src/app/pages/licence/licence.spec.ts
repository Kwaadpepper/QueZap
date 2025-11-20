import { ComponentFixture, TestBed } from '@angular/core/testing'

import { Licence } from './licence'

describe('Licence', () => {
  let component: Licence
  let fixture: ComponentFixture<Licence>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Licence] })
      .compileComponents()

    fixture = TestBed.createComponent(Licence)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
