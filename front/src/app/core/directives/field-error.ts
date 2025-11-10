import { ComponentRef, Directive, effect, inject, input, OnDestroy, ViewContainerRef } from '@angular/core'
import { FieldState, ValidationErrorWithField } from '@angular/forms/signals'

import { Message, MessageModule } from 'primeng/message'

type InputFieldState = FieldState<string, string>

@Directive({
  selector: '[quizzFieldError]',
  providers: [
    MessageModule,
  ],
  standalone: true,
})
export class FieldError implements OnDestroy {
  readonly fieldState = input.required<() => InputFieldState>()

  private readonly viewContainerRef = inject(ViewContainerRef)
  private readonly messageRef: ComponentRef<Message>

  constructor() {
    this.messageRef = this.viewContainerRef.createComponent(Message)
    this.messageRef.instance.severity = 'error'

    effect(() => {
      const errorList = this.extractErrorMessagesFrom(this.fieldState()())

      this.messageRef.setInput('pt', { content: { innerText: errorList.join('\n') } })
      this.messageRef.instance.visible.set(errorList.length !== 0)
    })
  }

  ngOnDestroy(): void {
    this.messageRef.destroy()
  }

  private extractErrorMessagesFrom(input: InputFieldState): string[] {
    if (!input.touched() || !input.dirty() || input.errors().length === 0) {
      return []
    }

    return this.toPrintableErrors(input.errors())
  }

  private toPrintableErrors(errors: ValidationErrorWithField[]): string[] {
    return errors.map((err) => {
      const field = err.field
      const kind = err.kind
      const message = err.message

      return message === undefined ? `• [${field}] Erreur de type '${kind}'` : `• ${message}`
    })
  }
}
