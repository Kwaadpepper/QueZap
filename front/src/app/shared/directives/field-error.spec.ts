import { Component, signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { FieldState } from '@angular/forms/signals'

import { MessageService } from 'primeng/api'

import { FieldError } from './field-error'

@Component({
  template: `<div [quizzFieldError] [fieldState]="mockFieldState"></div>`,
  imports: [FieldError],
})
class TestHostComponent {
  mockFieldState = () => ({
    touched: signal(false),
    dirty: signal(false),
    errors: signal([]),
  } as unknown as FieldState<string, string | number>)
}

describe('FieldError', () => {
  let fixture: ComponentFixture<TestHostComponent>
  let hostComponent: TestHostComponent

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [MessageService],
    }).compileComponents()

    fixture = TestBed.createComponent(TestHostComponent)
    hostComponent = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create an instance via host component', () => {
    expect(hostComponent).toBeTruthy()
  })
})
