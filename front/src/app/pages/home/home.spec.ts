import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { By } from '@angular/platform-browser'
import { provideRouter } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { Home } from './home'

describe('Home', () => {
  let component: Home
  let fixture: ComponentFixture<Home>
  const isLoggedInSignal = signal(false)

  const setup = async (loggedIn = false) => {
    isLoggedInSignal.set(loggedIn)

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        {
          provide: AuthenticatedUserStore,
          useValue: { isLoggedIn: isLoggedInSignal },
        },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(Home)
    component = fixture.componentInstance
    fixture.detectChanges()
  }

  describe('unauthenticated user', () => {
    beforeEach(async () => {
      await setup(false)
    })

    it('should create', () => {
      expect(component).toBeTruthy()
    })

    it('should render h1 with sr-only class for accessibility', () => {
      const h1 = fixture.debugElement.query(By.css('h1'))
      expect(h1).toBeTruthy()
      expect(h1.nativeElement.classList.contains('sr-only')).toBe(true)
      expect(h1.nativeElement.classList.contains('hidden')).toBe(false)
    })

    it('should have the corrected spelling for public participant', () => {
      const ol = fixture.debugElement.query(By.css('ol'))
      expect(ol.nativeElement.textContent).toContain('ou saisit le code pour rejoindre')
      expect(ol.nativeElement.textContent).not.toContain('ou saisis le code')
    })

    it('should display register button when not logged in and open modal on click', () => {
      const button = fixture.debugElement.query(By.css('button[aria-label="Créer une partie"]'))
      expect(button).toBeTruthy()

      const link = fixture.debugElement.query(By.css('a[routerLink="/admin/sessions/create"]'))
      expect(link).toBeNull()

      button.nativeElement.click()
      fixture.detectChanges()
      expect(component['registerModalVisible']()).toBe(true)
    })
  })

  describe('authenticated user', () => {
    beforeEach(async () => {
      await setup(true)
    })

    it('should render CTA as a link to /admin/sessions/create when logged in', () => {
      const link = fixture.debugElement.query(By.css('a[aria-label="Créer une partie"]'))
      expect(link).toBeTruthy()
      expect(link.attributes['routerLink']).toBe('/admin/sessions/create')

      const button = fixture.debugElement.query(By.css('button[aria-label="Créer une partie"]'))
      expect(button).toBeNull()
    })
  })
})
