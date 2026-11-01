import { ComponentFixture, TestBed } from '@angular/core/testing'

import { Confirmation, ConfirmationService, MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { ThemeId } from '@quezap/domain/models'

import { THEME_SERVICE, ThemesEventsBus } from '../../services'

import { ThemeCard } from './theme-card'

describe('ThemeCard', () => {
  let component: ThemeCard
  let fixture: ComponentFixture<ThemeCard>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThemeCard],
      providers: [
        MessageService,
        ThemesEventsBus,
        {
          provide: THEME_SERVICE,
          useValue: {
            delete: jest.fn().mockReturnValue(
              of({ kind: 'success', result: undefined }),
            ),
          },
        },
      ],
    })
      .compileComponents()

    fixture = TestBed.createComponent(ThemeCard)
    component = fixture.componentInstance
    fixture.componentRef.setInput('theme', {
      id: '018e3a2b-8888-7def-8123-000000000001' as ThemeId,
      name: 'Plantes',
    })
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should confirm and delete theme when onDeleteTheme is accepted', () => {
    const themeService = TestBed.inject(THEME_SERVICE)
    const eventsBus = TestBed.inject(ThemesEventsBus)
    const dispatchSpy = jest.spyOn(eventsBus, 'dispatchChanged')
    const confirmationService = fixture.debugElement.injector.get(ConfirmationService)
    jest.spyOn(confirmationService, 'confirm').mockImplementation((confirmation: Confirmation) => {
      confirmation.accept?.()
      return confirmationService
    })

    component['onDeleteTheme']({ currentTarget: document.createElement('button') } as unknown as Event)

    expect(themeService.delete).toHaveBeenCalledWith('018e3a2b-8888-7def-8123-000000000001')
    expect(dispatchSpy).toHaveBeenCalledWith('018e3a2b-8888-7def-8123-000000000001')
  })
})
