import { ComponentFixture, TestBed } from '@angular/core/testing'

import { NicknameChooser } from './nickname-chooser'

describe('NicknameChooser', () => {
  let component: NicknameChooser
  let fixture: ComponentFixture<NicknameChooser>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [NicknameChooser] })
      .compileComponents()

    fixture = TestBed.createComponent(NicknameChooser)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
