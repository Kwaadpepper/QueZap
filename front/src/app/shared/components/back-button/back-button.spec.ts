import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { BackButton } from './back-button'

describe('BackButton', () => {
  let component: BackButton
  let fixture: ComponentFixture<BackButton>

  const mockRouter = {
    navigateByUrl: jest.fn(),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BackButton],
      providers: [
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(BackButton)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  afterEach(() => {
    jest.clearAllMocks()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should navigate to backUrl on goBack', () => {
    // GIVEN
    const testUrl = '/test-url'
    fixture.componentRef.setInput('backUrl', testUrl)

    // WHEN
    component['onGoBackLink']()

    // THEN
    expect(mockRouter.navigateByUrl).toHaveBeenCalledWith(testUrl)
  })
})
