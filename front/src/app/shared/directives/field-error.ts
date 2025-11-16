import { ComponentRef, Directive, effect, inject, input, OnDestroy, ViewContainerRef } from '@angular/core'
import { FieldState, StandardSchemaValidationError, ValidationErrorWithField } from '@angular/forms/signals'

import { Message, MessageModule } from 'primeng/message'

type InputFieldState = FieldState<string, string | number>

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

    return input.errors().map(this.getErrorMessage)
  }

  private getErrorMessage(error: ValidationErrorWithField): string {
    const field = error.field().name
    const kind = error.kind
    const message = error.message

    if (kind === 'standardSchema') {
      return (error as StandardSchemaValidationError).issue.message
    }

    return message === undefined ? `• [${field}] Erreur de type '${kind}'` : `• ${message}`
  }
}
