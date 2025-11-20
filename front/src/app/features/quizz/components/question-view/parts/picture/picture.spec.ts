import { ComponentFixture, TestBed } from '@angular/core/testing'

import { Picture } from './picture'

describe('Picture', () => {
  let component: Picture
  let fixture: ComponentFixture<Picture>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Picture],
    })
      .compileComponents()

    fixture = TestBed.createComponent(Picture)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
