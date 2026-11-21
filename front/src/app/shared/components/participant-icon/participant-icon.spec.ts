import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ParticipantIcon } from './participant-icon'

describe('ParticipantIcon', () => {
  let component: ParticipantIcon
  let fixture: ComponentFixture<ParticipantIcon>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ParticipantIcon] })
      .compileComponents()

    fixture = TestBed.createComponent(ParticipantIcon)
    component = fixture.componentInstance
    fixture.componentRef.setInput('leafColor', '#ffffff')
    fixture.componentRef.setInput('backgroundColor', '#22c55e')
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
