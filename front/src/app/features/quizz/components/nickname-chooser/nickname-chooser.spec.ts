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
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should be valid with a 2-character nickname', () => {
    component['nicknameForm'].nicknameValue().value.set('Jo')
    fixture.detectChanges()
    expect(component['nicknameForm']().valid()).toBe(true)
  })

  it('should be invalid with a 1-character nickname', () => {
    component['nicknameForm'].nicknameValue().value.set('J')
    fixture.detectChanges()
    expect(component['nicknameForm']().valid()).toBe(false)
  })

  it('should be valid with a 50-character nickname and invalid with 51 characters', () => {
    const valid50 = 'A'.repeat(50)
    component['nicknameForm'].nicknameValue().value.set(valid50)
    fixture.detectChanges()
    expect(component['nicknameForm']().valid()).toBe(true)

    const invalid51 = 'A'.repeat(51)
    component['nicknameForm'].nicknameValue().value.set(invalid51)
    fixture.detectChanges()
    expect(component['nicknameForm']().valid()).toBe(false)
  })

  it('should display conflict error message when conflictError signal is set', () => {
    component['conflictError'].set(
      'Ce pseudo est déjà utilisé dans ce salon. Choisis un autre prénom ou ajoute une initiale.',
    )
    fixture.detectChanges()

    const compiled = fixture.nativeElement as HTMLElement
    const errorEl = compiled.querySelector('#nickname-feedback')
    expect(errorEl).not.toBeNull()
    expect(errorEl?.textContent).toContain('Ce pseudo est déjà utilisé')
  })

  it('should reset conflictError when nickname value changes', () => {
    component['conflictError'].set('Erreur conflit')
    fixture.detectChanges()

    component['nicknameForm'].nicknameValue().value.set('Nouveau')
    fixture.detectChanges()

    expect(component['conflictError']()).toBeNull()
  })
})
