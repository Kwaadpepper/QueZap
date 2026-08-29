import { ComponentFixture, TestBed } from '@angular/core/testing'

import { Paginator } from './paginator'

describe('Paginator', () => {
  let component: Paginator
  let fixture: ComponentFixture<Paginator>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Paginator] })
      .compileComponents()

    fixture = TestBed.createComponent(Paginator)
    component = fixture.componentInstance
    fixture.componentRef.setInput('pageInfo', {
      page: 1,
      pageSize: 10,
      totalElements: 10,
      totalPages: 1,
    })
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
