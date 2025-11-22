import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuezapCreate } from './quezap-create'

describe('QuezapCreate', () => {
  let component: QuezapCreate
  let fixture: ComponentFixture<QuezapCreate>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuezapCreate] })
      .compileComponents()

    fixture = TestBed.createComponent(QuezapCreate)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
