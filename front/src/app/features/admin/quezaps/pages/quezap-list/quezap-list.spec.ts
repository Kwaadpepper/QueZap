import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuezapList } from './quezap-list'

describe('QuezapList', () => {
  let component: QuezapList
  let fixture: ComponentFixture<QuezapList>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuezapList] })
      .compileComponents()

    fixture = TestBed.createComponent(QuezapList)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
